import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import { USE_MOCKS } from '../config'
import type { DashboardSnapshot } from '../types'
import { Button, Card, Notice, PageHeader, StatusPill } from '../components/Ui'

const fixture: DashboardSnapshot = { totalProducts: 30, totalOpenOrders: 47, delayedOrdersCount: 4, openRiskEventsCount: 12, asOf: '2026-07-17T00:00:00Z' }
export function DashboardPage() {
  const [data, setData] = useState<DashboardSnapshot | null>(USE_MOCKS ? fixture : null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(!USE_MOCKS)
  const load = useCallback(() => {
    if (USE_MOCKS) return
    setLoading(true); setError('')
    api.dashboard().then(setData).catch(exception => setError(exception instanceof Error ? exception.message : 'Dashboard is temporarily unavailable')).finally(() => setLoading(false))
  }, [])
  useEffect(() => { load() }, [load])
  return <><PageHeader eyebrow="Overview" title="Operations at a glance" description="A focused view of the signals that need attention first." />{error && <Notice tone="info"><span>Backend may be waking from an idle state. {error}</span><Button className="ml-3" variant="secondary" onClick={load}>Retry</Button></Notice>}{loading && <Card><div className="skeleton h-6 w-48" /><div className="skeleton mt-4 h-24 w-full" /></Card>}{data && <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[['Products', data.totalProducts], ['Open orders', data.totalOpenOrders], ['Delayed orders', data.delayedOrdersCount], ['Open risks', data.openRiskEventsCount]].map(([label, value]) => <Card key={label as string}><p className="text-sm text-slate-400">{label}</p><p className="mt-3 text-4xl font-semibold text-white">{value}</p></Card>)}</div>}{data?.latestBrief && <Card className="mt-6"><div className="flex items-center justify-between"><h2 className="font-semibold">Latest operations brief</h2><StatusPill value={data.latestBrief.generatedBy} /></div><p className="mt-3 text-sm leading-6 text-slate-300">{data.latestBrief.summary}</p></Card>}</>
}
