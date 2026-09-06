export function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description?: string; action?: React.ReactNode }) {
  return <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs uppercase tracking-[0.22em] text-accent">{eyebrow}</p><h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">{title}</h1>{description && <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">{description}</p>}</div>{action}</div>
}

export function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <section className={`rounded-2xl border border-slate-800 bg-panel/80 p-5 shadow-xl shadow-black/10 ${className}`}>{children}</section>
}

export function Button({ children, busy, variant = 'primary', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { busy?: boolean; variant?: 'primary' | 'secondary' | 'danger' }) {
  const styles = { primary: 'bg-accent text-slate-950 hover:bg-teal-300', secondary: 'border border-slate-700 text-slate-200 hover:border-slate-500', danger: 'border border-rose-400/40 text-rose-200 hover:bg-rose-400/10' }
  return <button {...props} disabled={busy || props.disabled} className={`rounded-lg px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${styles[variant]} ${props.className ?? ''}`}>{busy ? 'Working...' : children}</button>
}

export function StatusPill({ value }: { value: string }) {
  const tone = /CRITICAL|HIGH|DELAYED|OPEN/.test(value) ? 'bg-rose-400/10 text-rose-200' : /MEDIUM|ACKNOWLEDGED/.test(value) ? 'bg-amber-400/10 text-amber-200' : 'bg-slate-700 text-slate-300'
  return <span className={`inline-flex rounded-full px-2 py-1 text-[11px] font-semibold uppercase tracking-wide ${tone}`}>{value.replaceAll('_', ' ')}</span>
}

export function Notice({ children, tone = 'error' }: { children: React.ReactNode; tone?: 'error' | 'info' }) {
  return <div role="alert" className={`rounded-lg border px-4 py-3 text-sm ${tone === 'error' ? 'border-rose-400/30 bg-rose-400/10 text-rose-100' : 'border-sky-400/30 bg-sky-400/10 text-sky-100'}`}>{children}</div>
}

export function Deferred({ title, description }: { title: string; description: string }) {
  return <Card><div className="flex items-start gap-4"><div aria-hidden="true" className="rounded-xl bg-slate-800 p-3 text-accent">i</div><div><h2 className="font-semibold text-white">{title}</h2><p className="mt-2 max-w-xl text-sm leading-6 text-slate-400">{description}</p><span className="mt-4 inline-flex rounded-full bg-slate-800 px-2 py-1 text-[11px] uppercase tracking-wide text-slate-400">Deferred backend surface</span></div></div></Card>
}

export function Pagination({ page, totalPages, onPageChange, size, onSizeChange }: { page: number; totalPages: number; onPageChange: (page: number) => void; size?: number; onSizeChange?: (size: number) => void }) {
  if (totalPages <= 1 && !onSizeChange) return null
  return <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-800 pt-4" aria-label="Pagination">
    <span className="text-sm text-slate-400">Page {page + 1} of {Math.max(totalPages, 1)}</span>
    <div className="flex items-center gap-2">
      {onSizeChange && <label className="text-sm text-slate-400">Rows <select aria-label="Rows per page" value={size} onChange={event => onSizeChange(Number(event.target.value))} className="field ml-1 w-auto py-1"><option value="1">1</option><option value="10">10</option><option value="20">20</option><option value="50">50</option></select></label>}
      <Button variant="secondary" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Previous</Button>
      <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Next</Button>
    </div>
  </div>
}
