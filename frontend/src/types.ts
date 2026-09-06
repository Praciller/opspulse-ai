export type Role = 'ADMIN' | 'MANAGER' | 'OPERATOR' | 'VIEWER'

export type User = { id: string; email: string; fullName: string; active: boolean; roles: Role[]; createdAt: string }
export type AuthResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; user: User }
export type ApiError = { code?: string; message?: string; requestId?: string; fields?: Array<{ field: string; message: string }> }
export type Paged<T> = { items: T[]; page: number; size: number; total: number; totalPages: number }

export type Product = {
  id: string; sku: string; name: string; category: string; unit: string; currentStock: number
  safetyStock: number; reorderPoint: number; cost: number; sellingPrice: number; active: boolean
  createdAt: string; version: number
}
export type Supplier = {
  id: string; name: string; contactInfo: Record<string, string>; averageLeadTimeDays: number
  expectedSlaDays: number; active: boolean; createdAt: string; updatedAt: string; version: number
}
export type Order = {
  id: string; orderNumber: string; customerName: string; status: string; expectedShipDate: string
  actualShipDate?: string; totalAmount: number; items: Array<{ id: string; productId: string; quantity: number; unitPrice: number; lineTotal: number }>
  createdAt: string; updatedAt: string; version: number
}
export type PurchaseOrder = {
  id: string; poNumber: string; supplierId: string; status: string; expectedDeliveryDate: string
  actualDeliveryDate?: string; items: Array<{ id: string; productId: string; quantity: number; unitCost: number; lineTotal: number; receivedQuantity: number }>
  totalAmount: number; createdAt: string; updatedAt: string; version: number
}
export type Risk = {
  id: string; riskType: string; severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'; entityType: string
  entityId: string; sourceMetrics: Record<string, unknown>; explanation: string; recommendedAction: string
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'DISMISSED'; createdAt: string; resolvedAt?: string; resolvedBy?: string
}
export type Recommendation = {
  id: string; promptVersion: string; modelProviderName: string; generatedBy: 'AI' | 'RULE_BASED'; summary: string
  actions: Array<{ riskEventId: string; action: string; priority: string }>
  messageDrafts: Array<{ type: string; to?: string; subject?: string; body: string }>
  confidence?: number; inputRiskEventIds: string[]; createdAt: string; status: 'GENERATED' | 'APPROVED' | 'REJECTED' | 'ARCHIVED'
  userFeedback?: string; createdBy?: string
}
export type DashboardSnapshot = { totalProducts: number; totalOpenOrders: number; delayedOrdersCount: number; openRiskEventsCount: number; latestBrief?: Recommendation; asOf: string }

export type ImportJob = { id: string; idempotencyKey: string; fileHash: string; importType: string; status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'; totalRows?: number; successRows?: number; errorRows?: number; startedAt?: string; finishedAt?: string; createdAt: string }
export type ImportRowError = { id: string; rowNumber: number; rawRow: string; errorCode: string; errorMessage: string; createdAt: string }
export type DailyOpsBriefReport = { id?: string; generatedBy?: string; status?: string; summary?: string; actions?: unknown; messageDrafts?: unknown; generatedAt?: string; asOf: string }
export type InventoryRiskReport = { asOf: string; bySeverity: Record<string, number>; byType: Record<string, number>; topRisks: Array<{ id: string; riskType: string; severity: string; entityType: string; entityId: string; explanation: string; recommendedAction: string; createdAt: string }> }
export type SupplierSlaReport = { asOf: string; suppliers: Array<{ supplierId: string; name: string; receivedCount: number; lateCount: number; lateRatePct: number; averageLeadTimeDays: number }> }
export type OrderDelayReport = { asOf: string; byStatus: Record<string, number>; delayedOrders: Array<{ orderId: string; orderNumber: string; customerName: string; expectedShipDate: string; daysLate: number; status: string }> }
export type ProductMarginReport = { asOf: string; lowMarginCount: number; products: Array<{ productId: string; sku: string; name: string; cost: number; sellingPrice: number; marginAmount: number; marginPct?: number; riskLevel: string }> }

export type CreateProductInput = { sku: string; name: string; category?: string; unit: string; currentStock: number; safetyStock: number; reorderPoint: number; cost: number; sellingPrice: number }
export type UpdateProductInput = Omit<CreateProductInput, 'sku' | 'currentStock'> & { version: number }
export type SupplierInput = { name: string; contactInfo?: { email?: string; phone?: string; address?: string }; averageLeadTimeDays?: number; expectedSlaDays: number; version?: number }
export type OrderItemInput = { productId: string; quantity: number; unitPrice: number }
export type CreateOrderInput = { orderNumber: string; customerName: string; expectedShipDate: string; items: OrderItemInput[] }
export type UpdateOrderInput = Omit<CreateOrderInput, 'orderNumber'> & { version: number }
export type PurchaseOrderItemInput = { productId: string; quantity: number; unitCost: number }
export type CreatePurchaseOrderInput = { poNumber: string; supplierId: string; expectedDeliveryDate: string; items: PurchaseOrderItemInput[] }
export type UpdatePurchaseOrderInput = Omit<CreatePurchaseOrderInput, 'poNumber'> & { version: number }

export type ListQuery = { page?: number; size?: number; search?: string; sort?: string; status?: string; severity?: string; riskType?: string; entityType?: string; entityId?: string; from?: string; to?: string }
