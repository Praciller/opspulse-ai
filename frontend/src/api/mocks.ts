import type { AuthResponse, CreateOrderInput, CreateProductInput, CreatePurchaseOrderInput, DashboardSnapshot, DailyOpsBriefReport, ImportJob, ImportRowError, InventoryRiskReport, Order, OrderDelayReport, Paged, Product, ProductMarginReport, PurchaseOrder, Recommendation, Risk, Supplier, SupplierInput, SupplierSlaReport, UpdateOrderInput, UpdateProductInput, UpdatePurchaseOrderInput, User } from '../types'

const user: User = { id: 'demo-user', email: 'manager@opspulse.demo', fullName: 'Demo Operations Lead', active: true, roles: ['MANAGER', 'OPERATOR'], createdAt: '2026-07-17T00:00:00Z' }
let products: Product[] = [{ id: 'product-1', sku: 'WIDGET-001', name: 'Warehouse Widget', category: 'Widgets', unit: 'PCS', currentStock: 18, safetyStock: 25, reorderPoint: 30, cost: 12, sellingPrice: 19.99, active: true, createdAt: '2026-07-10T00:00:00Z', version: 0 }]
let suppliers: Supplier[] = [{ id: 'supplier-1', name: 'Northstar Supply', contactInfo: { email: 'ops@northstar.example' }, averageLeadTimeDays: 8, expectedSlaDays: 7, active: true, createdAt: '2026-07-10T00:00:00Z', updatedAt: '2026-07-10T00:00:00Z', version: 0 }]
let orders: Order[] = [{ id: 'order-1', orderNumber: 'ORD-1001', customerName: 'Acme Retail', status: 'DELAYED', expectedShipDate: '2026-07-16', totalAmount: 1250, items: [], createdAt: '2026-07-10T00:00:00Z', updatedAt: '2026-07-10T00:00:00Z', version: 0 }]
let purchaseOrders: PurchaseOrder[] = [{ id: 'po-1', poNumber: 'PO-2001', supplierId: 'supplier-1', status: 'SENT', expectedDeliveryDate: '2026-07-22', totalAmount: 720, items: [], createdAt: '2026-07-10T00:00:00Z', updatedAt: '2026-07-10T00:00:00Z', version: 0 }]
let risks: Risk[] = [
  { id: 'risk-1', riskType: 'STOCKOUT_RISK', severity: 'HIGH', entityType: 'PRODUCT', entityId: 'product-1', sourceMetrics: { ratio: 0.8, ruleVersion: '1.0.0' }, explanation: 'Stock is below the required level.', recommendedAction: 'Replenish stock before the next order window.', status: 'OPEN', createdAt: '2026-07-17T00:00:00Z' },
  { id: 'risk-2', riskType: 'ORDER_DELAY_RISK', severity: 'MEDIUM', entityType: 'ORDER', entityId: 'order-1', sourceMetrics: { ratio: 0.8, ruleVersion: '1.0.0' }, explanation: 'An order is approaching its due date.', recommendedAction: 'Confirm fulfillment capacity with the operator.', status: 'OPEN', createdAt: '2026-07-16T00:00:00Z' },
  { id: 'risk-3', riskType: 'SUPPLIER_DELAY_RISK', severity: 'LOW', entityType: 'SUPPLIER', entityId: 'supplier-1', sourceMetrics: { ratio: 0.5, ruleVersion: '1.0.0' }, explanation: 'Supplier delivery performance is below target.', recommendedAction: 'Review the next purchase order with the supplier.', status: 'ACKNOWLEDGED', createdAt: '2026-07-15T00:00:00Z' },
]
let recommendation: Recommendation | null = null
let importJobs: ImportJob[] = []
const importErrorsByJob: Record<string, ImportRowError[]> = {}

const now = () => new Date().toISOString()
const marginRisk = (cost: number, price: number) => {
  const margin = price - cost
  if (price <= 0 || margin <= 0) return 'CRITICAL'
  const ratio = 20 / (((margin / price) * 100))
  if (ratio < 0.5) return 'NONE'
  if (ratio < 0.667) return 'LOW'
  if (ratio < 1) return 'MEDIUM'
  if (ratio < 2) return 'HIGH'
  return 'CRITICAL'
}
const page = <T,>(items: T[], params: { page?: number; size?: number } = {}): Paged<T> => {
  const currentPage = params.page ?? 0; const size = params.size ?? 20; const start = currentPage * size
  return { items: items.slice(start, start + size), page: currentPage, size, total: items.length, totalPages: items.length ? Math.ceil(items.length / size) : 0 }
}

export const mocks = {
  login: async (): Promise<AuthResponse> => ({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token', tokenType: 'Bearer', expiresIn: 900, user }),
  refresh: async (): Promise<AuthResponse> => ({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token', tokenType: 'Bearer', expiresIn: 900, user }),
  logout: async () => undefined,
  products: async (params: { page?: number; size?: number; search?: string }) => page(products.filter(item => !params.search || `${item.name} ${item.sku}`.toLowerCase().includes(params.search.toLowerCase())), params),
  createProduct: async (input: CreateProductInput) => { const created = { ...input, id: crypto.randomUUID(), active: true, createdAt: now(), version: 0 } as Product; products = [created, ...products]; return created },
  updateProduct: async (id: string, input: UpdateProductInput) => { const updated = { ...products.find(item => item.id === id)!, ...input, id, version: input.version + 1 } as Product; products = products.map(item => item.id === id ? updated : item); return updated },
  deactivateProduct: async (id: string) => { products = products.map(item => item.id === id ? { ...item, active: false, version: item.version + 1 } : item) },
  suppliers: async (params: { page?: number; size?: number; search?: string }) => page(suppliers.filter(item => !params.search || item.name.toLowerCase().includes(params.search.toLowerCase())), params),
  createSupplier: async (input: SupplierInput) => { const created = { ...input, id: crypto.randomUUID(), contactInfo: input.contactInfo ?? {}, averageLeadTimeDays: input.averageLeadTimeDays ?? 0, active: true, createdAt: now(), updatedAt: now(), version: 0 } as Supplier; suppliers = [created, ...suppliers]; return created },
  updateSupplier: async (id: string, input: SupplierInput) => { const updated = { ...suppliers.find(item => item.id === id)!, ...input, id, contactInfo: input.contactInfo ?? {}, updatedAt: now(), version: (input.version ?? 0) + 1 } as Supplier; suppliers = suppliers.map(item => item.id === id ? updated : item); return updated },
  deactivateSupplier: async (id: string) => { suppliers = suppliers.map(item => item.id === id ? { ...item, active: false, version: item.version + 1 } : item) },
  orders: async (params: { page?: number; size?: number }) => page(orders, params),
  createOrder: async (input: CreateOrderInput) => { const created = { ...input, id: crypto.randomUUID(), status: 'NEW', totalAmount: input.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0), items: input.items.map(item => ({ ...item, id: crypto.randomUUID(), lineTotal: item.quantity * item.unitPrice })), createdAt: now(), updatedAt: now(), version: 0 } as Order; orders = [created, ...orders]; return created },
  updateOrder: async (id: string, input: UpdateOrderInput) => { const updated = { ...orders.find(item => item.id === id)!, ...input, id, updatedAt: now(), version: input.version + 1 } as Order; orders = orders.map(item => item.id === id ? updated : item); return updated },
  updateOrderStatus: async (id: string, status: string) => { const current = orders.find(item => item.id === id)!; const updated = { ...current, status, updatedAt: now(), version: current.version + 1 } as Order; orders = orders.map(item => item.id === id ? updated : item); return updated },
  purchaseOrders: async (params: { page?: number; size?: number }) => page(purchaseOrders, params),
  createPurchaseOrder: async (input: CreatePurchaseOrderInput) => { const created = { ...input, id: crypto.randomUUID(), status: 'DRAFT', totalAmount: input.items.reduce((sum, item) => sum + item.quantity * item.unitCost, 0), items: input.items.map(item => ({ ...item, id: crypto.randomUUID(), lineTotal: item.quantity * item.unitCost, receivedQuantity: 0 })), createdAt: now(), updatedAt: now(), version: 0 } as PurchaseOrder; purchaseOrders = [created, ...purchaseOrders]; return created },
  updatePurchaseOrder: async (id: string, input: UpdatePurchaseOrderInput) => { const updated = { ...purchaseOrders.find(item => item.id === id)!, ...input, id, updatedAt: now(), version: input.version + 1 } as PurchaseOrder; purchaseOrders = purchaseOrders.map(item => item.id === id ? updated : item); return updated },
  updatePurchaseOrderStatus: async (id: string, status: string) => { const current = purchaseOrders.find(item => item.id === id)!; const updated = { ...current, status, updatedAt: now(), version: current.version + 1 } as PurchaseOrder; purchaseOrders = purchaseOrders.map(item => item.id === id ? updated : item); return updated },
  receivePurchaseOrder: async (id: string) => { const current = purchaseOrders.find(item => item.id === id)!; const updated = { ...current, status: 'RECEIVED', actualDeliveryDate: now().slice(0, 10), updatedAt: now(), version: current.version + 1 } as PurchaseOrder; purchaseOrders = purchaseOrders.map(item => item.id === id ? updated : item); return updated },
  risks: async (params: { page?: number; size?: number; severity?: string; status?: string; riskType?: string; entityType?: string; entityId?: string; from?: string; to?: string; sort?: string }) => {
    const filtered = risks.filter(item => (!params.severity || item.severity === params.severity) && (!params.status || item.status === params.status) && (!params.riskType || item.riskType === params.riskType) && (!params.entityType || item.entityType === params.entityType) && (!params.entityId || item.entityId === params.entityId) && (!params.from || item.createdAt >= params.from) && (!params.to || item.createdAt <= params.to))
    const [field, direction] = (params.sort ?? 'createdAt,desc').split(',')
    filtered.sort((a, b) => { const left = String(a[field as keyof Risk] ?? ''); const right = String(b[field as keyof Risk] ?? ''); return (left < right ? -1 : left > right ? 1 : 0) * (direction === 'asc' ? 1 : -1) })
    return page(filtered, params)
  },
  scanRisks: async () => ({ evaluatedEntities: 3, createdRiskEvents: 1, completedAt: now() }),
  riskAction: async (id: string, action: 'acknowledge' | 'resolve' | 'dismiss') => { const status = action === 'acknowledge' ? 'ACKNOWLEDGED' : action === 'resolve' ? 'RESOLVED' : 'DISMISSED'; risks = risks.map(item => item.id === id ? { ...item, status } : item); return risks.find(item => item.id === id)! },
  recommendations: async (params: { page?: number; size?: number }) => page(recommendation ? [recommendation] : [], params),
  generateBrief: async () => { recommendation = { id: 'brief-1', promptVersion: '1.0.0', modelProviderName: 'RULE_BASED', generatedBy: 'RULE_BASED', summary: 'One high-priority stockout risk needs attention today.', actions: [{ riskEventId: 'risk-1', action: 'Replenish stock before the next order window.', priority: 'HIGH' }], messageDrafts: [], inputRiskEventIds: ['risk-1'], createdAt: now(), status: 'GENERATED' }; return recommendation },
  briefAction: async (_id: string, action: 'approve' | 'reject') => { recommendation = recommendation ? { ...recommendation, status: action === 'approve' ? 'APPROVED' : 'REJECTED' } : null; return recommendation! },
  feedback: async (_id: string, feedback: string) => { recommendation = recommendation ? { ...recommendation, userFeedback: feedback } : null; return recommendation! },
  dashboard: async (): Promise<DashboardSnapshot> => ({ totalProducts: products.length, totalOpenOrders: orders.length, delayedOrdersCount: orders.filter(order => order.status === 'DELAYED').length, openRiskEventsCount: risks.filter(item => item.status === 'OPEN').length, latestBrief: recommendation ?? undefined, asOf: now() }),
  imports: async (params: { page?: number; size?: number }) => page(importJobs, params),
  startImport: async (file: File, type: string, idempotencyKey: string): Promise<ImportJob> => {
    const replay = importJobs.find(item => item.idempotencyKey === idempotencyKey)
    if (replay) return replay
    const job: ImportJob = { id: crypto.randomUUID(), idempotencyKey, fileHash: `${file.name}-${file.size}`, importType: type, status: 'PENDING', createdAt: now() }
    importJobs = [job, ...importJobs]
    window.setTimeout(() => { importJobs = importJobs.map(item => item.id === job.id ? { ...item, status: 'COMPLETED', totalRows: Math.max(1, file.size ? 1 : 0), successRows: 1, errorRows: 0, startedAt: now(), finishedAt: now() } : item) }, 50)
    return job
  },
  importErrors: async (id: string, params: { page?: number; size?: number }) => page(importErrorsByJob[id] ?? [], params),
  reportDailyOpsBrief: async (): Promise<DailyOpsBriefReport> => ({ id: recommendation?.id, generatedBy: recommendation?.generatedBy, status: recommendation?.status, summary: recommendation?.summary, actions: recommendation?.actions ?? [], messageDrafts: recommendation?.messageDrafts ?? [], generatedAt: recommendation?.createdAt, asOf: now() }),
  reportInventoryRisk: async (): Promise<InventoryRiskReport> => ({ asOf: now(), bySeverity: risks.reduce<Record<string, number>>((result, risk) => ({ ...result, [risk.severity]: (result[risk.severity] ?? 0) + (risk.status === 'OPEN' || risk.status === 'ACKNOWLEDGED' ? 1 : 0) }), {}), byType: risks.reduce<Record<string, number>>((result, risk) => ({ ...result, [risk.riskType]: (result[risk.riskType] ?? 0) + 1 }), {}), topRisks: risks.map(({ id, riskType, severity, entityType, entityId, explanation, recommendedAction, createdAt }) => ({ id, riskType, severity, entityType, entityId, explanation, recommendedAction, createdAt })) }),
  reportSupplierSla: async (): Promise<SupplierSlaReport> => ({ asOf: now(), suppliers: suppliers.map(item => ({ supplierId: item.id, name: item.name, receivedCount: 4, lateCount: 1, lateRatePct: 25, averageLeadTimeDays: item.averageLeadTimeDays })) }),
  reportOrderDelay: async (): Promise<OrderDelayReport> => ({ asOf: now(), byStatus: orders.reduce<Record<string, number>>((result, order) => ({ ...result, [order.status]: (result[order.status] ?? 0) + 1 }), {}), delayedOrders: orders.filter(order => order.status !== 'SHIPPED' && order.status !== 'CANCELLED').map(order => ({ orderId: order.id, orderNumber: order.orderNumber, customerName: order.customerName, expectedShipDate: order.expectedShipDate, daysLate: 1, status: order.status })) }),
  reportProductMargin: async (): Promise<ProductMarginReport> => ({ asOf: now(), lowMarginCount: products.filter(item => item.sellingPrice <= 0 || ((item.sellingPrice - item.cost) / item.sellingPrice) * 100 <= 40).length, products: products.map(item => ({ productId: item.id, sku: item.sku, name: item.name, cost: item.cost, sellingPrice: item.sellingPrice, marginAmount: item.sellingPrice - item.cost, marginPct: item.sellingPrice > 0 ? ((item.sellingPrice - item.cost) / item.sellingPrice) * 100 : undefined, riskLevel: marginRisk(item.cost, item.sellingPrice) })) }),
}
