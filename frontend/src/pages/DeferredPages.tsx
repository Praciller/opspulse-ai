import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/api'
import { useAuth } from '../auth/useAuth'
import { Button, Card, Deferred, Notice, PageHeader, Pagination, StatusPill } from '../components/Ui'
import type { DailyOpsBriefReport, ImportJob, ImportRowError, InventoryRiskReport, OrderDelayReport, Paged, ProductMarginReport, SupplierSlaReport } from '../types'

export function ReportsPage() {
  const [report, setReport] = useState('inventory')
  const [data, setData] = useState<{ daily?: DailyOpsBriefReport; inventory?: InventoryRiskReport; supplier?: SupplierSlaReport; order?: OrderDelayReport; margin?: ProductMarginReport }>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let mounted = true
    setLoading(true); setError('')
    Promise.all([api.reportDailyOpsBrief(), api.reportInventoryRisk(), api.reportSupplierSla(), api.reportOrderDelay(), api.reportProductMargin()])
      .then(([daily, inventory, supplier, order, margin]) => { if (mounted) setData({ daily, inventory, supplier, order, margin }) })
      .catch(exception => { if (mounted) setError(exception instanceof Error ? exception.message : 'Unable to load reports') })
      .finally(() => { if (mounted) setLoading(false) })
    return () => { mounted = false }
  }, [])
  const body = useMemo(() => {
    if (report === 'daily') return <Card><h2 className="font-semibold text-white">Daily operations brief</h2><p className="mt-3 text-sm text-slate-300">{data.daily?.summary ?? 'No brief has been generated yet.'}</p>{data.daily?.generatedBy && <StatusPill value={data.daily.generatedBy} />}</Card>
    if (report === 'supplier') return <Card><h2 className="font-semibold text-white">Supplier SLA</h2><div className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Supplier SLA report</caption><thead><tr><th>Supplier</th><th>Received</th><th>Late</th><th>Late rate</th><th>Avg lead</th></tr></thead><tbody>{(data.supplier?.suppliers ?? []).map(item => <tr key={item.supplierId}><td>{item.name}</td><td>{item.receivedCount}</td><td>{item.lateCount}</td><td>{item.lateRatePct}%</td><td>{item.averageLeadTimeDays}d</td></tr>)}</tbody></table></div></Card>
    if (report === 'order') return <Card><h2 className="font-semibold text-white">Order delay</h2><div className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Order delay report</caption><thead><tr><th>Order</th><th>Customer</th><th>Due</th><th>Days late</th><th>Status</th></tr></thead><tbody>{(data.order?.delayedOrders ?? []).map(item => <tr key={item.orderId}><td>{item.orderNumber}</td><td>{item.customerName}</td><td>{item.expectedShipDate}</td><td>{item.daysLate}</td><td><StatusPill value={item.status} /></td></tr>)}</tbody></table></div></Card>
    if (report === 'margin') return <Card><div className="flex items-center justify-between"><h2 className="font-semibold text-white">Product margin</h2><span className="text-sm text-slate-400">At risk: {data.margin?.lowMarginCount ?? 0}</span></div><div className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Product margin report</caption><thead><tr><th>SKU</th><th>Product</th><th>Margin</th><th>Margin %</th><th>Risk</th></tr></thead><tbody>{(data.margin?.products ?? []).map(item => <tr key={item.productId}><td>{item.sku}</td><td>{item.name}</td><td>{item.marginAmount.toFixed(2)}</td><td>{item.marginPct?.toFixed(2) ?? '—'}</td><td><StatusPill value={item.riskLevel} /></td></tr>)}</tbody></table></div></Card>
    return <Card><div className="flex items-center justify-between"><h2 className="font-semibold text-white">Inventory risk</h2><span className="text-sm text-slate-400">As of {data.inventory?.asOf ? new Date(data.inventory.asOf).toLocaleString() : '—'}</span></div><div className="mt-4 grid gap-3 sm:grid-cols-2">{Object.entries(data.inventory?.bySeverity ?? {}).map(([key, value]) => <div key={key} className="rounded-xl border border-slate-800 p-3"><StatusPill value={key} /><p className="mt-2 text-2xl font-semibold text-white">{value}</p></div>)}</div><div className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Top inventory risks</caption><thead><tr><th>Type</th><th>Severity</th><th>Entity</th><th>Explanation</th></tr></thead><tbody>{(data.inventory?.topRisks ?? []).map(item => <tr key={item.id}><td>{item.riskType}</td><td><StatusPill value={item.severity} /></td><td>{item.entityType}</td><td>{item.explanation}</td></tr>)}</tbody></table></div></Card>
  }, [data, report])
  return <><PageHeader eyebrow="Analysis" title="Reports" description="Deterministic JSON-first operational aggregates." /><div className="mb-5 flex flex-wrap gap-2" role="tablist" aria-label="Reports">{[['inventory', 'Inventory risk'], ['supplier', 'Supplier SLA'], ['order', 'Order delay'], ['margin', 'Product margin'], ['daily', 'Daily brief']].map(([value, label]) => <Button key={value} variant={report === value ? 'primary' : 'secondary'} onClick={() => setReport(value)} role="tab" aria-selected={report === value}>{label}</Button>)}</div>{error && <Notice tone="info">{error}</Notice>}{loading ? <Card><div className="skeleton h-48 w-full" /></Card> : body}</>
}

export function ImportsPage() {
  const { user } = useAuth()
  const canImport = user?.roles.some(role => ['ADMIN', 'MANAGER', 'OPERATOR'].includes(role)) ?? false
  const [jobs, setJobs] = useState<Paged<ImportJob> | null>(null)
  const [errors, setErrors] = useState<Paged<ImportRowError> | null>(null)
  const [file, setFile] = useState<File | null>(null)
  const [type, setType] = useState('products')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [page, setPage] = useState(0)
  const load = useCallback(() => { setLoading(true); api.imports({ page, size: 10 }).then(setJobs).catch(exception => setError(exception instanceof Error ? exception.message : 'Unable to load imports')).finally(() => setLoading(false)) }, [page])
  useEffect(() => { if (canImport) load() }, [canImport, load])
  useEffect(() => { const pending = jobs?.items.some(job => job.status === 'PENDING' || job.status === 'PROCESSING'); if (!pending) return; const timer = window.setTimeout(load, 500); return () => window.clearTimeout(timer) }, [jobs, load])
  if (!canImport) return <><PageHeader eyebrow="Data operations" title="Imports" /><Notice tone="info">Your role can view reports but cannot start or inspect imports.</Notice></>
  const submit = async (event: React.FormEvent) => { event.preventDefault(); if (!file) { setError('Choose a CSV file first.'); return } setBusy(true); setError(''); setMessage(''); try { const job = await api.startImport(file, type, crypto.randomUUID()); setMessage(`Import ${job.id} accepted.`); setFile(null); load() } catch (exception) { setError(exception instanceof Error ? exception.message : 'Unable to start import') } finally { setBusy(false) } }
  const showErrors = async (id: string) => { try { setErrors(await api.importErrors(id, { page: 0, size: 20 })) } catch (exception) { setError(exception instanceof Error ? exception.message : 'Unable to load row errors') } }
  return <><PageHeader eyebrow="Data operations" title="Imports" description="Upload bounded CSV files with idempotency, status polling, and row-level validation." />{error && <Notice>{error}</Notice>}{message && <Notice tone="info">{message}</Notice>}<Card className="mb-5"><form onSubmit={submit} className="grid gap-4 sm:grid-cols-[1fr_180px_auto] sm:items-end"><label className="text-sm text-slate-300">CSV file<input required type="file" accept=".csv,text/csv" onChange={event => setFile(event.target.files?.[0] ?? null)} className="field mt-2" /></label><label className="text-sm text-slate-300">Import type<select value={type} onChange={event => setType(event.target.value)} className="field mt-2"><option value="products">Products</option><option value="suppliers">Suppliers</option><option value="orders">Orders</option><option value="inventory">Inventory</option><option value="po">Purchase orders</option></select></label><Button type="submit" busy={busy}>Start import</Button></form><p className="mt-3 text-xs text-slate-400">CSV files are limited to 10 MiB and 10,000 rows. Use a new idempotency key for corrected content.</p></Card><Card>{loading ? <div className="skeleton h-40 w-full" /> : !jobs?.items.length ? <p className="py-12 text-center text-sm text-slate-400">No imports yet.</p> : <div className="overflow-x-auto"><table className="data-table"><caption className="sr-only">Import jobs</caption><thead><tr><th>Type</th><th>Status</th><th>Rows</th><th>Created</th><th>Errors</th></tr></thead><tbody>{jobs.items.map(job => <tr key={job.id}><td>{job.importType}</td><td><StatusPill value={job.status} /></td><td>{job.totalRows ?? '—'} / {job.successRows ?? '—'}</td><td>{new Date(job.createdAt).toLocaleString()}</td><td>{job.errorRows ? <Button variant="secondary" onClick={() => void showErrors(job.id)}>View {job.errorRows}</Button> : '—'}</td></tr>)}</tbody></table><Pagination page={jobs.page} totalPages={jobs.totalPages} onPageChange={setPage} size={jobs.size} onSizeChange={() => undefined} /></div>}</Card>{errors && <Card className="mt-5"><h2 className="font-semibold text-white">Row errors</h2><div className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Import row errors</caption><thead><tr><th>Row</th><th>Code</th><th>Message</th><th>Raw row</th></tr></thead><tbody>{errors.items.map(item => <tr key={item.id}><td>{item.rowNumber}</td><td>{item.errorCode}</td><td>{item.errorMessage}</td><td className="max-w-sm truncate">{item.rawRow}</td></tr>)}</tbody></table></div></Card>}</>
}

export function NotFoundPage() { return <><PageHeader eyebrow="Console" title="Page not found" description="The requested console route does not exist." /><Deferred title="Choose an operations area" description="Use the navigation to return to a supported workflow." /></> }
