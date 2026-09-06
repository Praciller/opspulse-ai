import { apiFetch } from './client'
import { USE_MOCKS } from '../config'
import { mocks } from './mocks'
import type { AuthResponse, CreateOrderInput, CreateProductInput, CreatePurchaseOrderInput, DashboardSnapshot, DailyOpsBriefReport, ImportJob, ImportRowError, InventoryRiskReport, ListQuery, Order, OrderDelayReport, Paged, Product, ProductMarginReport, PurchaseOrder, Recommendation, Risk, Supplier, SupplierInput, SupplierSlaReport, UpdateOrderInput, UpdateProductInput, UpdatePurchaseOrderInput } from '../types'

const query = (values: ListQuery) => new URLSearchParams(Object.entries(values).filter(([, value]) => value !== undefined && value !== '').map(([key, value]) => [key, String(value)])).toString()
export const api = {
  login: (email: string, password: string) => USE_MOCKS ? mocks.login() : apiFetch<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  refresh: (refreshToken: string) => USE_MOCKS ? mocks.refresh() : apiFetch<AuthResponse>('/api/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
  logout: (refreshToken: string) => USE_MOCKS ? mocks.logout() : apiFetch<void>('/api/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
  products: (params: ListQuery = {}) => USE_MOCKS ? mocks.products(params) : apiFetch<Paged<Product>>(`/api/products?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  createProduct: (input: CreateProductInput) => USE_MOCKS ? mocks.createProduct(input) : apiFetch<Product>('/api/products', { method: 'POST', body: JSON.stringify(input) }),
  updateProduct: (id: string, input: UpdateProductInput) => USE_MOCKS ? mocks.updateProduct(id, input) : apiFetch<Product>(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  deactivateProduct: (id: string, version: number) => USE_MOCKS ? mocks.deactivateProduct(id) : apiFetch<void>(`/api/products/${id}?version=${version}`, { method: 'DELETE' }),
  suppliers: (params: ListQuery = {}) => USE_MOCKS ? mocks.suppliers(params) : apiFetch<Paged<Supplier>>(`/api/suppliers?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  createSupplier: (input: SupplierInput) => USE_MOCKS ? mocks.createSupplier(input) : apiFetch<Supplier>('/api/suppliers', { method: 'POST', body: JSON.stringify(input) }),
  updateSupplier: (id: string, input: SupplierInput) => USE_MOCKS ? mocks.updateSupplier(id, input) : apiFetch<Supplier>(`/api/suppliers/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  deactivateSupplier: (id: string, version: number) => USE_MOCKS ? mocks.deactivateSupplier(id) : apiFetch<void>(`/api/suppliers/${id}?version=${version}`, { method: 'DELETE' }),
  orders: (params: ListQuery = {}) => USE_MOCKS ? mocks.orders(params) : apiFetch<Paged<Order>>(`/api/orders?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  createOrder: (input: CreateOrderInput) => USE_MOCKS ? mocks.createOrder(input) : apiFetch<Order>('/api/orders', { method: 'POST', body: JSON.stringify(input) }),
  updateOrder: (id: string, input: UpdateOrderInput) => USE_MOCKS ? mocks.updateOrder(id, input) : apiFetch<Order>(`/api/orders/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  updateOrderStatus: (id: string, status: string, version: number, actualShipDate?: string) => USE_MOCKS ? mocks.updateOrderStatus(id, status) : apiFetch<Order>(`/api/orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, version, actualShipDate }) }),
  purchaseOrders: (params: ListQuery = {}) => USE_MOCKS ? mocks.purchaseOrders(params) : apiFetch<Paged<PurchaseOrder>>(`/api/purchase-orders?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  createPurchaseOrder: (input: CreatePurchaseOrderInput) => USE_MOCKS ? mocks.createPurchaseOrder(input) : apiFetch<PurchaseOrder>('/api/purchase-orders', { method: 'POST', body: JSON.stringify(input) }),
  updatePurchaseOrder: (id: string, input: UpdatePurchaseOrderInput) => USE_MOCKS ? mocks.updatePurchaseOrder(id, input) : apiFetch<PurchaseOrder>(`/api/purchase-orders/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  updatePurchaseOrderStatus: (id: string, status: string, version: number) => USE_MOCKS ? mocks.updatePurchaseOrderStatus(id, status) : apiFetch<PurchaseOrder>(`/api/purchase-orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, version }) }),
  receivePurchaseOrder: (id: string, items: Array<{ productId: string; quantity: number }>, version: number, actualDeliveryDate?: string) => USE_MOCKS ? mocks.receivePurchaseOrder(id) : apiFetch<PurchaseOrder>(`/api/purchase-orders/${id}/receive`, { method: 'POST', body: JSON.stringify({ items, version, actualDeliveryDate }) }),
  risks: (params: ListQuery = {}) => USE_MOCKS ? mocks.risks(params) : apiFetch<Paged<Risk>>(`/api/risks?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  scanRisks: () => USE_MOCKS ? mocks.scanRisks() : apiFetch<{ evaluatedEntities: number; createdRiskEvents: number; completedAt: string }>('/api/risks/scan', { method: 'POST' }),
  riskAction: (id: string, action: 'acknowledge' | 'resolve' | 'dismiss') => USE_MOCKS ? mocks.riskAction(id, action) : apiFetch<Risk>(`/api/risks/${id}/${action}`, { method: 'POST' }),
  recommendations: (params: ListQuery = {}) => USE_MOCKS ? mocks.recommendations(params) : apiFetch<Paged<Recommendation>>(`/api/ai/recommendations?${query({ page: 0, size: 20, sort: 'createdAt,desc', ...params })}`),
  generateBrief: (topN = 5) => USE_MOCKS ? mocks.generateBrief() : apiFetch<Recommendation>('/api/ai/recommendations/generate', { method: 'POST', body: JSON.stringify({ topN }) }),
  briefAction: (id: string, action: 'approve' | 'reject') => USE_MOCKS ? mocks.briefAction(id, action) : apiFetch<Recommendation>(`/api/ai/recommendations/${id}/${action}`, { method: 'POST' }),
  feedback: (id: string, feedback: string) => USE_MOCKS ? mocks.feedback(id, feedback) : apiFetch<Recommendation>(`/api/ai/recommendations/${id}/feedback`, { method: 'PATCH', body: JSON.stringify({ feedback }) }),
  dashboard: () => USE_MOCKS ? mocks.dashboard() : apiFetch<DashboardSnapshot>('/api/dashboard'),
  imports: (params: ListQuery = {}) => USE_MOCKS ? mocks.imports(params) : apiFetch<Paged<ImportJob>>(`/api/imports?${query({ page: 0, size: 20, ...params })}`),
  startImport: (file: File, type: string, idempotencyKey: string) => {
    const body = new FormData(); body.append('file', file); body.append('type', type)
    return USE_MOCKS ? mocks.startImport(file, type, idempotencyKey) : apiFetch<ImportJob>(`/api/imports?type=${encodeURIComponent(type)}`, { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey }, body })
  },
  importErrors: (id: string, params: ListQuery = {}) => USE_MOCKS ? mocks.importErrors(id, params) : apiFetch<Paged<ImportRowError>>(`/api/imports/${id}/errors?${query({ page: 0, size: 20, ...params })}`),
  reportDailyOpsBrief: () => USE_MOCKS ? mocks.reportDailyOpsBrief() : apiFetch<DailyOpsBriefReport>('/api/reports/daily-ops-brief'),
  reportInventoryRisk: () => USE_MOCKS ? mocks.reportInventoryRisk() : apiFetch<InventoryRiskReport>('/api/reports/inventory-risk'),
  reportSupplierSla: () => USE_MOCKS ? mocks.reportSupplierSla() : apiFetch<SupplierSlaReport>('/api/reports/supplier-sla'),
  reportOrderDelay: () => USE_MOCKS ? mocks.reportOrderDelay() : apiFetch<OrderDelayReport>('/api/reports/order-delay'),
  reportProductMargin: () => USE_MOCKS ? mocks.reportProductMargin() : apiFetch<ProductMarginReport>('/api/reports/product-margin'),
}
