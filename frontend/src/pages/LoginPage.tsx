import { FormEvent, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Notice, Button } from '../components/Ui'

export function LoginPage() {
  const { login, loading } = useAuth(); const navigate = useNavigate(); const location = useLocation()
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [error, setError] = useState('')
  const submit = async (event: FormEvent) => { event.preventDefault(); setError(''); try { await login(email, password); navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true }) } catch (exception) { setError(exception instanceof Error ? exception.message : 'Unable to sign in') } }
  return <div className="flex min-h-screen items-center justify-center bg-slate-950 px-5 py-10"><div className="w-full max-w-md"><div className="mb-8"><div className="text-lg font-bold tracking-tight">Ops<span className="text-accent">Pulse</span></div><h1 className="mt-8 text-4xl font-semibold tracking-tight text-white">Make the next move obvious.</h1><p className="mt-3 text-sm leading-6 text-slate-400">Sign in to the operations console to review inventory, supplier, order, and risk signals.</p></div><form onSubmit={submit} className="rounded-2xl border border-slate-800 bg-panel p-6 shadow-2xl shadow-black/20">{error && <Notice>{error}</Notice>}<label className="mt-1 block text-sm font-medium text-slate-200">Email<input required type="email" value={email} onChange={event => setEmail(event.target.value)} className="field mt-2" autoComplete="email" /></label><label className="mt-4 block text-sm font-medium text-slate-200">Password<input required type="password" value={password} onChange={event => setPassword(event.target.value)} className="field mt-2" autoComplete="current-password" /></label><Button type="submit" busy={loading} className="mt-6 w-full">Sign in</Button></form></div></div>
}
