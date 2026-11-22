import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { api } from './api/api'

describe('Dashboard deep link', () => {
  beforeEach(() => {
    sessionStorage.clear()
    window.history.replaceState(null, '', '/dashboard')
  })

  afterEach(() => {
    sessionStorage.clear()
    window.history.replaceState(null, '', '/')
    vi.restoreAllMocks()
  })

  it('opens the existing dashboard when a viewer follows the deep link', async () => {
    sessionStorage.setItem('opspulse.user', JSON.stringify({
      id: 'viewer-test',
      email: 'viewer@example.test',
      fullName: 'Demo Viewer',
      active: true,
      roles: ['VIEWER'],
      createdAt: '2026-01-01T00:00:00Z',
    }))
    const dashboard = vi.spyOn(api, 'dashboard').mockResolvedValue({
      totalProducts: 30,
      totalOpenOrders: 47,
      delayedOrdersCount: 4,
      openRiskEventsCount: 12,
      asOf: '2026-01-01T00:00:00Z',
    })

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Operations at a glance' })).toBeInTheDocument()
    expect(await screen.findByText('47')).toBeInTheDocument()
    expect(window.location.pathname).toBe('/')
    expect(dashboard).toHaveBeenCalledOnce()
  })

  it('keeps the dashboard behind sign-in for an anonymous visitor', async () => {
    const dashboard = vi.spyOn(api, 'dashboard')

    render(<App />)

    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/login')
    expect(dashboard).not.toHaveBeenCalled()
  })
})
