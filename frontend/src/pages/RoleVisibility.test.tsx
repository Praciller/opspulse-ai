import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../auth/context'
import { ProductsPage } from './ProductsPage'
import { PurchaseOrdersPage } from './PurchaseOrdersPage'
import { RisksPage } from './RisksPage'

const { apiMock } = vi.hoisted(() => ({ apiMock: {
  products: vi.fn(), createProduct: vi.fn(), deactivateProduct: vi.fn(),
  risks: vi.fn(), scanRisks: vi.fn(), riskAction: vi.fn(),
  purchaseOrders: vi.fn(), suppliers: vi.fn(),
  createPurchaseOrder: vi.fn(), updatePurchaseOrderStatus: vi.fn(), receivePurchaseOrder: vi.fn(),
} }))
vi.mock('../api/api', () => ({ api: apiMock }))

const product = { id: 'product-1', sku: 'SKU-1', name: 'Widget', category: 'Test', unit: 'PCS', currentStock: 3, safetyStock: 2, reorderPoint: 2, cost: 1, sellingPrice: 2, active: true, createdAt: '2026-07-17T00:00:00Z', version: 0 }
const risk = { id: 'risk-1', riskType: 'STOCKOUT_RISK', severity: 'HIGH' as const, entityType: 'PRODUCT', entityId: 'product-1', sourceMetrics: {}, explanation: 'Stock is low.', recommendedAction: 'Replenish.', status: 'OPEN' as const, createdAt: '2026-07-17T00:00:00Z' }
const purchaseOrder = { id: 'po-1', poNumber: 'PO-1', supplierId: 'supplier-1', status: 'DRAFT', expectedDeliveryDate: '2026-07-20', items: [], totalAmount: 10, createdAt: 'now', updatedAt: 'now', version: 0 }
const renderWithRoles = (roles: Array<'ADMIN' | 'MANAGER' | 'OPERATOR' | 'VIEWER'>, page: React.ReactNode) => render(<AuthContext.Provider value={{ user: { id: 'u1', email: 'user@example.test', fullName: 'Test User', active: true, roles, createdAt: 'now' }, loading: false, login: vi.fn(), logout: vi.fn(), can: (...allowed) => allowed.some(role => roles.includes(role)) }}>{page}</AuthContext.Provider>)

describe('role-aware operational controls', () => {
  beforeEach(() => { vi.clearAllMocks(); apiMock.products.mockResolvedValue({ items: [product], page: 0, size: 20, total: 1, totalPages: 1 }); apiMock.risks.mockResolvedValue({ items: [risk], page: 0, size: 20, total: 1, totalPages: 1 }); apiMock.purchaseOrders.mockResolvedValue({ items: [purchaseOrder], page: 0, size: 20, total: 1, totalPages: 1 }); apiMock.suppliers.mockResolvedValue({ items: [], page: 0, size: 20, total: 0, totalPages: 0 }) })

  it('hides product mutations for viewers and shows admin deactivation', async () => {
    const viewer = renderWithRoles(['VIEWER'], <ProductsPage />)
    await waitFor(() => expect(screen.getByText('Widget')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'New product' })).not.toBeInTheDocument()
    viewer.unmount()
    renderWithRoles(['ADMIN'], <ProductsPage />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Deactivate' })).toBeInTheDocument())
    expect(screen.getByRole('button', { name: 'New product' })).toBeInTheDocument()
  })

  it('shows risk actions only to manager-capable roles', async () => {
    const viewer = renderWithRoles(['VIEWER'], <RisksPage />)
    await waitFor(() => expect(screen.getByText('Stock is low.')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Run scan' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Acknowledge' })).not.toBeInTheDocument()
    viewer.unmount()
    renderWithRoles(['MANAGER'], <RisksPage />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Run scan' })).toBeInTheDocument())
    expect(screen.getByRole('button', { name: 'Acknowledge' })).toBeInTheDocument()
  })

  it('shows purchase-order transitions to managers while viewers remain read-only', async () => {
    const viewer = renderWithRoles(['VIEWER'], <PurchaseOrdersPage />)
    await waitFor(() => expect(screen.getByText('PO-1')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Send PO' })).not.toBeInTheDocument()
    viewer.unmount()
    renderWithRoles(['MANAGER'], <PurchaseOrdersPage />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Send PO' })).toBeInTheDocument())
  })
})
