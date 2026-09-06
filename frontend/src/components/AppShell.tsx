import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const links = [
  ['/', 'Overview'],
  ['/products', 'Products'],
  ['/suppliers', 'Suppliers'],
  ['/orders', 'Orders'],
  ['/purchase-orders', 'Purchase orders'],
  ['/risks', 'Risk events'],
  ['/brief', 'AI brief'],
  ['/reports', 'Reports'],
  ['/imports', 'Imports'],
] as const

export function AppShell() {
  const { user, logout } = useAuth()
  return <div className="min-h-screen bg-slate-950 text-slate-100 lg:flex">
    <aside className="border-b border-slate-800 bg-ink lg:flex lg:min-h-screen lg:w-64 lg:flex-col lg:border-b-0 lg:border-r">
      <div className="flex items-center justify-between px-5 py-5 lg:block">
        <div className="text-lg font-bold tracking-tight">Ops<span className="text-accent">Pulse</span></div>
        <span className="rounded-full bg-slate-800 px-2 py-1 text-[10px] uppercase tracking-widest text-slate-400">Console</span>
      </div>
      <nav aria-label="Primary navigation" className="flex gap-1 overflow-x-auto px-3 pb-4 lg:block lg:flex-1 lg:space-y-1 lg:px-3">
        {links.map(([to, label]) => <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `block whitespace-nowrap rounded-lg px-3 py-2 text-sm transition ${isActive ? 'bg-teal-400/10 text-accent' : 'text-slate-400 hover:bg-slate-800 hover:text-white'}`}>{label}</NavLink>)}
      </nav>
      <div className="hidden border-t border-slate-800 p-4 lg:block">
        <div className="truncate text-sm font-medium">{user?.fullName}</div>
        <div className="truncate text-xs text-slate-400">{user?.roles.join(' / ')}</div>
        <button onClick={() => void logout()} className="mt-3 text-xs text-slate-400 hover:text-white">Sign out</button>
      </div>
    </aside>
    <main className="min-w-0 flex-1">
      <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4 lg:px-10">
        <div><p className="text-xs uppercase tracking-[0.22em] text-slate-400">Operations intelligence</p><p className="mt-1 text-sm text-slate-300">Evidence first. Action ready.</p></div>
        <button onClick={() => void logout()} className="rounded-md border border-slate-700 px-3 py-2 text-xs text-slate-300 hover:border-slate-500 lg:hidden">Sign out</button>
      </div>
      <div className="mx-auto max-w-7xl p-5 lg:p-10"><Outlet /></div>
    </main>
  </div>
}
