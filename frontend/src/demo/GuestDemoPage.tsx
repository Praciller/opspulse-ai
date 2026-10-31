import { useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { Card, StatusPill } from '../components/Ui'
import { demoBrief, demoMetrics, demoOrders, demoProducts, demoRisks, demoSuppliers } from './data'

const navigation = [
  { href: '/demo', label: 'Overview' },
  { href: '/demo/products', label: 'Products' },
  { href: '/demo/risks', label: 'Risk events' },
  { href: '/demo/reports', label: 'Reports' },
  { href: '/demo/brief', label: 'AI brief' },
] as const

const sectionTitle: Record<string, string> = {
  '/demo': 'Operations at a glance',
  '/demo/products': 'Inventory and product health',
  '/demo/risks': 'Evidence-backed risk events',
  '/demo/reports': 'Operational reports',
  '/demo/brief': 'A daily action plan, grounded in data',
}

function Intro({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) {
  return <header className="mb-7"><p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">{eyebrow}</p><h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl">{title}</h2><p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">{description}</p></header>
}

function DemoOverview() {
  return <>
    <Intro eyebrow="01 / Overview" title="Operations at a glance" description="Start with the signals a warehouse or operations manager would review first. Everything below is a fixed, fictional scenario." />
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {demoMetrics.map(metric => <Card key={metric.label}><p className="text-sm text-slate-400">{metric.label}</p><p className="mt-2 text-4xl font-semibold text-white">{metric.value}</p><p className="mt-2 text-xs text-slate-400">{metric.hint}</p></Card>)}
    </div>
    <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1.55fr)_minmax(0,1fr)]">
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="text-lg font-semibold text-white">Needs attention</h3><Link to="/demo/risks" className="text-sm font-medium text-accent hover:underline">View all risks</Link></div>
        <div className="mt-4 space-y-3">
          {demoRisks.slice(0,3).map(risk => <div key={risk.id} className="rounded-xl border border-slate-800 bg-slate-950/40 p-4"><div className="flex flex-wrap items-center justify-between gap-3"><p className="font-medium text-slate-100">{risk.label}</p><StatusPill value={risk.severity} /></div><p className="mt-2 text-sm text-slate-400">{risk.evidence}</p></div>)}
        </div>
      </Card>
      <Card>
        <h3 className="text-lg font-semibold text-white">From signal to action</h3>
        <ol className="mt-4 space-y-5">
          {[['01', 'Read operational data', 'Inventory, orders and supplier performance feed the scenario.'], ['02', 'Apply deterministic rules', 'Reorder thresholds, delayed orders and SLA metrics explain each alert.'], ['03', 'Prepare a grounded brief', 'Recommended actions reference specific evidence; people make the decisions.']].map(([number, title, description]) => <li key={number} className="flex gap-3"><span className="font-mono text-sm font-semibold text-accent">{number}</span><div><h4 className="text-sm font-semibold text-slate-100">{title}</h4><p className="mt-1 text-sm leading-6 text-slate-400">{description}</p></div></li>)}
        </ol>
        <Link to="/demo/brief" className="mt-6 inline-block text-sm font-medium text-accent hover:underline">Read example AI brief</Link>
      </Card>
    </div>
  </>
}

function DemoProducts() {
  const [search, setSearch] = useState('')
  const filtered = demoProducts.filter(product => [product.sku, product.name, product.category, product.supplier].some(value => value.toLowerCase().includes(search.trim().toLowerCase())))
  return <>
    <Intro eyebrow="02 / Inventory" title="Inventory and product health" description="Search fictional SKUs and compare current stock with reorder points. No inventory is created or changed." />
    <Card>
      <label htmlFor="demo-product-search" className="block text-sm font-medium text-slate-200">Search example products</label>
      <input id="demo-product-search" className="field mt-2 max-w-sm" value={search} onChange={event => setSearch(event.target.value)} placeholder="Name, SKU, category, supplier" />
      <p role="status" className="mt-3 text-xs text-slate-400">{filtered.length} of {demoProducts.length} sample products</p>
      <div role="region" aria-label="Demo products table" tabIndex={0} className="mt-3 overflow-x-auto">
        <table className="data-table"><caption className="sr-only">Synthetic product inventory</caption><thead><tr><th scope="col">Product</th><th scope="col">Supplier</th><th scope="col">Stock</th><th scope="col">Reorder point</th><th scope="col">Signal</th></tr></thead><tbody>
          {filtered.map(item => <tr key={item.sku}><td><span className="font-medium text-white">{item.name}</span><span className="block text-xs text-slate-500">{item.sku}</span></td><td>{item.supplier}</td><td>{item.stock}</td><td>{item.reorderPoint}</td><td><StatusPill value={item.stock < item.reorderPoint ? 'HIGH' : 'HEALTHY'} /></td></tr>)}
        </tbody></table>
        {filtered.length === 0 && <p className="py-8 text-center text-sm text-slate-400">No sample products match your search.</p>}
      </div>
    </Card>
  </>
}

function DemoRisks() {
  const [severity, setSeverity] = useState('ALL')
  const filtered = demoRisks.filter(risk => severity === 'ALL' || risk.severity === severity)
  return <>
    <Intro eyebrow="03 / Risk intelligence" title="Evidence-backed risk events" description="Try filtering by severity and inspect which source metrics support each recommended action. Rules are illustrated, not executed against production data." />
    <div className="mb-4 flex flex-wrap gap-2" role="group" aria-label="Risk severity filter">
      {['ALL', 'HIGH', 'MEDIUM'].map(value => <button key={value} type="button" onClick={() => setSeverity(value)} aria-pressed={severity === value} className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${severity === value ? 'border-teal-300/50 bg-teal-400/10 text-accent' : 'border-slate-700 text-slate-300 hover:border-slate-500'}`}>{value === 'ALL' ? 'All risks' : value[0] + value.slice(1).toLowerCase()}</button>)}
    </div>
    <div className="grid gap-4 lg:grid-cols-2">
      {filtered.map(risk => <Card key={risk.id}><div className="flex items-center justify-between gap-3"><span className="font-mono text-xs text-slate-500">{risk.id}</span><StatusPill value={risk.severity} /></div><h3 className="mt-3 text-lg font-semibold text-white">{risk.label}</h3><p className="mt-1 text-xs text-slate-500">{risk.type} · {risk.entity}</p><div className="mt-4 border-t border-slate-800 pt-4"><p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Evidence</p><p className="mt-2 text-sm leading-6 text-slate-200">{risk.evidence}</p></div><div className="mt-4 rounded-xl bg-slate-950/60 p-3"><p className="text-xs font-semibold uppercase tracking-wider text-accent">Suggested next step</p><p className="mt-2 text-sm leading-6 text-slate-200">{risk.action}</p></div></Card>)}
    </div>
  </>
}

function DemoReports() {
  const [report, setReport] = useState<'inventory' | 'suppliers' | 'orders' | 'margin'>('inventory')
  const tabs = [{ id: 'inventory', label: 'Inventory risk' }, { id: 'suppliers', label: 'Supplier SLA' }, { id: 'orders', label: 'Order delay' }, { id: 'margin', label: 'Product margin' }] as const
  return <>
    <Intro eyebrow="04 / Reports" title="Operational reports" description="Switch between deterministic views of the same fictional dataset. No server requests, credentials or live reporting data are involved." />
    <div role="tablist" aria-label="Example reports" className="mb-4 flex flex-wrap gap-2">
      {tabs.map(tab => <button id={`demo-tab-${tab.id}`} key={tab.id} type="button" role="tab" aria-controls="demo-report-panel" aria-selected={report === tab.id} onClick={() => setReport(tab.id)} className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${report === tab.id ? 'border-teal-300/50 bg-teal-400/10 text-accent' : 'border-slate-700 text-slate-300 hover:border-slate-500'}`}>{tab.label}</button>)}
    </div>
    <div id="demo-report-panel" role="tabpanel" aria-labelledby={`demo-tab-${report}`}>
      <Card>
        {report === 'inventory' && <><h3 className="text-lg font-semibold text-white">Inventory risk</h3><p className="mt-2 text-sm text-slate-400">{demoProducts.filter(p => p.stock < p.reorderPoint).length} products are below the sample reorder threshold.</p><div className="mt-4 grid gap-3 sm:grid-cols-2">{demoProducts.filter(p => p.stock < p.reorderPoint).map(item => <div key={item.sku} className="rounded-xl border border-slate-800 p-4"><p className="text-sm font-medium text-white">{item.name}</p><p className="mt-2 text-sm text-slate-300">Stock: {item.stock} · Reorder: {item.reorderPoint}</p></div>)}</div></>}
        {report === 'suppliers' && <><h3 className="text-lg font-semibold text-white">Supplier SLA</h3><div role="region" aria-label="Sample supplier SLA" tabIndex={0} className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Synthetic supplier performance</caption><thead><tr><th scope="col">Supplier</th><th scope="col">Deliveries</th><th scope="col">Late</th><th scope="col">Late rate</th></tr></thead><tbody>{demoSuppliers.map(item => <tr key={item.name}><td>{item.name}</td><td>{item.deliveries}</td><td>{item.late}</td><td>{Math.round(item.late / item.deliveries * 100)}%</td></tr>)}</tbody></table></div></>}
        {report === 'orders' && <><h3 className="text-lg font-semibold text-white">Order delay</h3><div role="region" aria-label="Sample delayed orders" tabIndex={0} className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Synthetic order delays</caption><thead><tr><th scope="col">Order</th><th scope="col">Customer</th><th scope="col">Status</th><th scope="col">Days late</th></tr></thead><tbody>{demoOrders.map(order => <tr key={order.id}><td>{order.id}</td><td>{order.customer}</td><td><StatusPill value={order.status} /></td><td>{order.daysLate || '—'}</td></tr>)}</tbody></table></div></>}
        {report === 'margin' && <><h3 className="text-lg font-semibold text-white">Product margin</h3><div role="region" aria-label="Sample product margins" tabIndex={0} className="mt-4 overflow-x-auto"><table className="data-table"><caption className="sr-only">Synthetic product margins</caption><thead><tr><th scope="col">Product</th><th scope="col">Selling price</th><th scope="col">Cost</th><th scope="col">Gross margin</th></tr></thead><tbody>{demoProducts.map(item => <tr key={item.sku}><td>{item.name}</td><td>{item.price.toFixed(2)}</td><td>{item.cost.toFixed(2)}</td><td>{((item.price - item.cost) / item.price * 100).toFixed(1)}%</td></tr>)}</tbody></table></div></>}
      </Card>
    </div>
  </>
}

function DemoBrief() {
  return <>
    <Intro eyebrow="05 / Decision support" title="A daily action plan, grounded in data" description="A static example of a rule-based fallback brief. It is not a live AI model response; every suggested action references the displayed evidence." />
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="text-xl font-semibold text-white">{demoBrief.title}</h3><StatusPill value="RULE_BASED" /></div>
      <p className="mt-4 max-w-3xl text-sm leading-7 text-slate-300">{demoBrief.summary}</p>
      <h4 className="mt-6 text-sm font-semibold uppercase tracking-[0.14em] text-slate-400">Proposed actions</h4>
      <ol className="mt-3 space-y-3">{demoBrief.actions.map(action => <li key={action.label} className="rounded-xl border border-slate-800 p-4"><div className="flex flex-wrap items-center justify-between gap-3"><p className="font-medium text-white">{action.label}</p><StatusPill value={action.priority} /></div><p className="mt-2 text-xs text-slate-400">{action.evidence}</p></li>)}</ol>
    </Card>
    <p className="mt-4 text-sm text-slate-400">Engineering note: production uses deterministic risks, audit evidence and an optional AI provider, with a rule-based fallback when AI is unavailable. This page is only a sample of the intended output.</p>
  </>
}

export function GuestDemoPage() {
  const { pathname } = useLocation()
  const path = pathname.replace(/\/$/, '') || '/demo'
  const section = path in sectionTitle ? path : '/demo/unknown'
  return <div className="min-h-screen bg-ink text-slate-100">
    <a href="#demo-content" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-slate-800 focus:p-3 focus:text-white">Skip to demo content</a>
    <header className="border-b border-slate-800/80"><div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 py-5 lg:px-10"><Link to="/demo" className="text-xl font-bold tracking-tight">Ops<span className="text-accent">Pulse</span><span className="ml-2 text-sm font-normal text-slate-400">AI</span></Link><div className="flex items-center gap-4"><span className="hidden text-xs font-semibold uppercase tracking-widest text-accent sm:inline">Guest · Read only</span><Link to="/login" className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 hover:border-slate-500 hover:text-white">Sign in to live console</Link></div></div></header>
    <div className="mx-auto max-w-7xl px-5 pb-16 pt-9 lg:px-10">
      <main>
      <div className="max-w-4xl"><p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Interactive portfolio · No account needed</p><h1 className="mt-3 max-w-3xl text-3xl font-semibold leading-tight tracking-tight text-white sm:text-5xl">Make operational decisions easier to see.</h1><p className="mt-4 max-w-2xl text-base leading-7 text-slate-300">Explore how inventory, purchasing, supplier metrics and risk rules come together in one operations console. Built as a full-stack AI and data engineering portfolio project.</p><div className="mt-6 flex flex-wrap gap-3"><Link to="/demo/risks" className="rounded-lg bg-accent px-5 py-3 text-sm font-semibold text-slate-950 hover:bg-teal-300">Explore priority risks</Link><Link to="/demo/reports" className="rounded-lg border border-slate-700 px-5 py-3 text-sm font-semibold text-slate-200 hover:border-slate-500">Explore reports</Link></div></div>
      <aside className="my-8 rounded-xl border border-sky-800/50 bg-sky-900/10 px-5 py-4 text-sm leading-6 text-sky-100" aria-label="Demo data disclosure"><strong>Synthetic data · Public guest demo.</strong> All names, orders, metrics and recommendations on these screens are fictional. This guest experience runs entirely in your browser: no account, API requests, database access or write permissions.</aside>
      <nav aria-label="Guest demo navigation" className="mb-8 flex flex-wrap gap-2 border-b border-slate-800 pb-4">{navigation.map(item => <NavLink key={item.href} to={item.href} end className={({isActive}) => `rounded-lg px-4 py-2 text-sm font-medium transition ${isActive ? 'bg-slate-800 text-accent' : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'}`}>{item.label}</NavLink>)}</nav>
      <section id="demo-content" tabIndex={-1} className="outline-none">
        {section === '/demo' && <DemoOverview />}
        {section === '/demo/products' && <DemoProducts />}
        {section === '/demo/risks' && <DemoRisks />}
        {section === '/demo/reports' && <DemoReports />}
        {section === '/demo/brief' && <DemoBrief />}
        {section === '/demo/unknown' && <Card><h2 className="text-xl font-semibold text-white">Demo page not found</h2><Link to="/demo" className="mt-3 inline-block text-accent hover:underline">Return to overview</Link></Card>}
      </section>
      </main>
      <footer className="mt-12 flex flex-wrap items-center justify-between gap-4 border-t border-slate-800 pt-6 text-sm text-slate-400"><p>Guest mode is a read-only illustration — not a production account or live dashboard.</p><a href="https://github.com/Praciller/opspulse-ai" target="_blank" rel="noopener noreferrer" className="font-medium text-accent hover:underline">Explore the source on GitHub</a></footer>
    </div>
  </div>
}
