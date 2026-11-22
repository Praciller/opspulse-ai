import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

const renderRoutes = (initial: string) => render(
  <AuthProvider>
    <MemoryRouter initialEntries={[initial]}>
      <Routes>
        <Route path="/login" element={<p>Login screen</p>} />
        <Route path="/demo" element={<p>Guest demo screen</p>} />
        <Route element={<ProtectedRoute />}><Route index element={<p>Dashboard screen</p>} /></Route>
        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="/admin" element={<p>Admin action</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  </AuthProvider>,
)

describe('ProtectedRoute', () => {
  beforeEach(() => sessionStorage.clear())

  it('redirects unauthenticated users to login', () => {
    renderRoutes('/admin')
    expect(screen.getByText('Login screen')).toBeInTheDocument()
  })

  it('redirects authenticated users without the required role to dashboard', () => {
    sessionStorage.setItem('opspulse.user', JSON.stringify({ id: 'u1', email: 'viewer@example.test', fullName: 'Viewer', active: true, roles: ['VIEWER'], createdAt: new Date().toISOString() }))
    renderRoutes('/admin')
    expect(screen.getByText('Dashboard screen')).toBeInTheDocument()
  })
  it('sends anonymous first-time visitors to the public guest demo', () => {
    renderRoutes('/')
    expect(screen.getByText('Guest demo screen')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard screen')).not.toBeInTheDocument()
  })

  it('keeps the real dashboard for already signed-in users', () => {
    sessionStorage.setItem('opspulse.user', JSON.stringify({ id: 'u2', email: 'viewer@example.test', fullName: 'Viewer', active: true, roles: ['VIEWER'], createdAt: 'now' }))
    renderRoutes('/')
    expect(screen.getByText('Dashboard screen')).toBeInTheDocument()
  })

})

