import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { CreateProductInput, Product } from '../types'
import { useAuth } from '../auth/useAuth'
import { Button, Card, Notice, PageHeader, StatusPill } from '../components/Ui'

const emptyDraft: CreateProductInput = { sku: '', name: '', category: '', unit: 'PCS', currentStock: 0, safetyStock: 0, reorderPoint: 0, cost: 0, sellingPrice: 0 }

export function ProductsPage() {
  const { can } = useAuth()
  const [items, setItems] = useState<Product[]>([]); const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [showForm, setShowForm] = useState(false); const [error, setError] = useState(''); const [success, setSuccess] = useState(''); const [search, setSearch] = useState(''); const [draft, setDraft] = useState<CreateProductInput>(emptyDraft)
  const load = useCallback(() => { setLoading(true); setError(''); api.products({ search: search || undefined }).then(response => setItems(response.items)).catch(e => setError(e instanceof Error ? e.message : 'Unable to load products')).finally(() => setLoading(false)) }, [search])
  useEffect(() => { load() }, [load])
  const field = (key: keyof CreateProductInput, value: string) => setDraft(current => ({ ...current, [key]: ['currentStock', 'safetyStock', 'reorderPoint', 'cost', 'sellingPrice'].includes(key) ? Number(value) : value }))
  const save = async (event: FormEvent) => { event.preventDefault(); setSaving(true); setError(''); setSuccess(''); try { const created = await api.createProduct(draft); setItems(current => [created, ...current]); setDraft(emptyDraft); setShowForm(false); setSuccess(`Product ${created.sku} created.`) } catch (e) { setError(e instanceof Error ? e.message : 'Unable to create product') } finally { setSaving(false) } }
  const deactivate = async (item: Product) => { setError(''); setSuccess(''); try { await api.deactivateProduct(item.id, item.version); setItems(current => current.map(row => row.id === item.id ? { ...row, active: false } : row)); setSuccess(`Product ${item.sku} deactivated.`) } catch (e) { setError(e instanceof Error ? e.message : 'Unable to deactivate product') } }
  return <>
    <PageHeader eyebrow="Inventory" title="Products" description="Monitor stock position, margin inputs, and product activity." action={can('ADMIN', 'OPERATOR') && <Button onClick={() => { setShowForm(value => !value); setSuccess('') }}>{showForm ? 'Close form' : 'New product'}</Button>} />
    {error && <Notice>{error}</Notice>}{success && <Notice tone="info">{success}</Notice>}
    {showForm && <Card className="mb-5"><h2 className="font-semibold text-white">Create product</h2><form onSubmit={save} className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <label className="text-sm text-slate-300">SKU<input required value={draft.sku} onChange={e => field('sku', e.target.value)} className="field mt-2" /></label>
      <label className="text-sm text-slate-300">Name<input required value={draft.name} onChange={e => field('name', e.target.value)} className="field mt-2" /></label>
      <label className="text-sm text-slate-300">Unit<input required value={draft.unit} onChange={e => field('unit', e.target.value)} className="field mt-2" /></label>
      <label className="text-sm text-slate-300">Category<input value={draft.category} onChange={e => field('category', e.target.value)} className="field mt-2" /></label>
      {(['currentStock', 'safetyStock', 'reorderPoint', 'cost', 'sellingPrice'] as const).map(key => <label key={key} className="text-sm capitalize text-slate-300">{key.replace(/([A-Z])/g, ' $1')}<input required min="0" step="0.01" type="number" value={draft[key]} onChange={e => field(key, e.target.value)} className="field mt-2" /></label>)}
      <div className="sm:col-span-2 lg:col-span-3"><Button type="submit" busy={saving}>Create product</Button></div>
    </form></Card>}
    <Card><div className="mb-5 flex flex-col gap-3 sm:flex-row"><input aria-label="Search products" placeholder="Search SKU or name" value={search} onChange={event => setSearch(event.target.value)} className="field max-w-sm" /><Button variant="secondary" onClick={load}>Search</Button></div>
      {loading ? <div className="skeleton h-40 w-full" /> : items.length === 0 ? <p className="py-12 text-center text-sm text-slate-400">No products found.</p> : <div className="overflow-x-auto"><table className="data-table"><caption className="sr-only">Products</caption><thead><tr><th scope="col">Product</th><th scope="col">Stock</th><th scope="col">Margin</th><th scope="col">Status</th>{can('ADMIN') && <th scope="col">Actions</th>}</tr></thead><tbody>{items.map(item => <tr key={item.id}><td><div className="font-medium text-white">{item.name}</div><div className="text-xs text-slate-400">{item.sku}</div></td><td>{item.currentStock} {item.unit}</td><td>{item.sellingPrice > 0 ? `${(((item.sellingPrice - item.cost) / item.sellingPrice) * 100).toFixed(1)}%` : '-'}</td><td><StatusPill value={item.active ? 'ACTIVE' : 'INACTIVE'} /></td>{can('ADMIN') && <td>{item.active && <Button variant="danger" onClick={() => void deactivate(item)}>Deactivate</Button>}</td>}</tr>)}</tbody></table></div>}
    </Card>
  </>
}

