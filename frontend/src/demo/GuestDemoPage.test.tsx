import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { GuestDemoPage } from './GuestDemoPage'

const renderDemo = (route = '/demo') => render(
  <MemoryRouter initialEntries={[route]}>
    <Routes><Route path="/demo/*" element={<GuestDemoPage />} /></Routes>
  </MemoryRouter>,
)

describe('public portfolio demo', () => {
  afterEach(() => vi.restoreAllMocks())

  it('loads a genuine read-only overview with no backend traffic or credentials', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    renderDemo()
    expect(screen.getByRole('heading', { name: 'Operations at a glance' })).toBeInTheDocument()
    expect(screen.getByText(/synthetic data · public guest demo/i)).toBeInTheDocument()
    expect(screen.getByText('Products monitored')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign in to live console' })).toHaveAttribute('href', '/login')
    expect(screen.queryByRole('button', { name: /create|approve|delete|generate/i })).not.toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('searches products and exposes an unambiguous empty state', () => {
    renderDemo('/demo/products')
    const search = screen.getByRole('textbox', { name: 'Search example products' })
    fireEvent.change(search, { target: { value: 'sensor' } })
    expect(screen.getByText('Temperature Sensor')).toBeInTheDocument()
    expect(screen.queryByText('Warehouse Widget')).not.toBeInTheDocument()
    fireEvent.change(search, { target: { value: 'no-match-123' } })
    expect(screen.getByText('No sample products match your search.')).toBeInTheDocument()
  })

  it('filters the risk examples without mutating any data', () => {
    renderDemo('/demo/risks')
    expect(screen.getByText('Supplier SLA below target')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'High' }))
    expect(screen.getByRole('button', { name: 'High' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByText('Supplier SLA below target')).not.toBeInTheDocument()
    expect(screen.getByText('Widget stock below threshold')).toBeInTheDocument()
  })

  it('switches across four independent synthetic report views', () => {
    renderDemo('/demo/reports')
    expect(screen.getByRole('heading', { name: 'Inventory risk' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'Supplier SLA' }))
    expect(screen.getByRole('heading', { name: 'Supplier SLA' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Sample supplier SLA' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'Order delay' }))
    expect(screen.getByRole('region', { name: 'Sample delayed orders' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'Product margin' }))
    expect(screen.getByRole('region', { name: 'Sample product margins' })).toBeInTheDocument()
  })

  it('identifies the brief as a rule-based sample, not a live AI call', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    renderDemo('/demo/brief')
    expect(screen.getByText(/static example of a rule-based fallback brief/i)).toBeInTheDocument()
    expect(screen.getByText(/RISK-01 · 18 units/)).toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})
