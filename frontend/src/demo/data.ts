/**
 * Public portfolio example data. Deliberately independent from API clients,
 * authentication, browser storage, and backend database contents.
 * Never put customer data, secrets, or live metrics in this fixture.
 */
export const demoProducts = [
  { sku: 'WIDGET-001', name: 'Warehouse Widget', category: 'Components', stock: 18, reorderPoint: 30, price: 19.99, cost: 12, supplier: 'Northstar Supply' },
  { sku: 'SENSOR-008', name: 'Temperature Sensor', category: 'Electronics', stock: 7, reorderPoint: 15, price: 45, cost: 28, supplier: 'Summit Components' },
  { sku: 'PACK-012', name: 'Protective Packaging', category: 'Packaging', stock: 210, reorderPoint: 80, price: 3.5, cost: 1.8, supplier: 'Harbor Materials' },
  { sku: 'VALVE-023', name: 'Control Valve', category: 'Components', stock: 44, reorderPoint: 20, price: 38, cost: 21, supplier: 'Northstar Supply' },
  { sku: 'CABLE-104', name: 'Industrial Cable', category: 'Electronics', stock: 12, reorderPoint: 25, price: 16, cost: 11, supplier: 'Summit Components' },
  { sku: 'LABEL-205', name: 'Shipment Labels', category: 'Packaging', stock: 145, reorderPoint: 50, price: 5, cost: 1.5, supplier: 'Harbor Materials' },
] as const

export const demoOrders = [
  { id: 'ORD-1001', customer: 'Acme Retail', status: 'DELAYED', daysLate: 3, amount: 1250 },
  { id: 'ORD-1002', customer: 'Pioneer Stores', status: 'DELAYED', daysLate: 1, amount: 760 },
  { id: 'ORD-1003', customer: 'Atlas Distribution', status: 'IN_PROGRESS', daysLate: 0, amount: 2380 },
  { id: 'ORD-1004', customer: 'Evergreen Trading', status: 'NEW', daysLate: 0, amount: 520 },
] as const

export const demoSuppliers = [
  { name: 'Northstar Supply', deliveries: 28, late: 4, leadDays: 8, targetDays: 7 },
  { name: 'Summit Components', deliveries: 20, late: 5, leadDays: 10, targetDays: 7 },
  { name: 'Harbor Materials', deliveries: 35, late: 1, leadDays: 4, targetDays: 5 },
] as const

export const demoRisks = [
  { id: 'RISK-01', type: 'STOCKOUT_RISK', label: 'Widget stock below threshold', severity: 'HIGH', entity: 'WIDGET-001', evidence: '18 units available; reorder point is 30.', action: 'Review replenishment quantity and supplier lead time.' },
  { id: 'RISK-02', type: 'STOCKOUT_RISK', label: 'Sensor inventory may run out', severity: 'HIGH', entity: 'SENSOR-008', evidence: '7 units available; reorder point is 15.', action: 'Prioritize a purchase order for the next delivery window.' },
  { id: 'RISK-03', type: 'ORDER_DELAY_RISK', label: 'Customer order is overdue', severity: 'MEDIUM', entity: 'ORD-1001', evidence: 'Acme Retail order is 3 days late.', action: 'Confirm the shipping date and notify the customer.' },
  { id: 'RISK-04', type: 'SUPPLIER_DELAY_RISK', label: 'Supplier SLA below target', severity: 'MEDIUM', entity: 'Summit Components', evidence: '5 of 20 deliveries missed the SLA.', action: 'Review the supplier lead-time trend before reordering.' },
] as const

export const demoMetrics = [
  { label: 'Products monitored', value: demoProducts.length.toString(), hint: 'Sample SKUs' },
  { label: 'Open orders', value: demoOrders.length.toString(), hint: '2 delayed in example' },
  { label: 'Open risks', value: demoRisks.length.toString(), hint: '2 high-priority signals' },
  { label: 'Suppliers tracked', value: demoSuppliers.length.toString(), hint: 'Lead times & SLA' },
] as const

export const demoBrief = {
  title: 'Prioritize replenishment before the next order window',
  summary: 'Two SKUs have fallen below their reorder points. At the same time, an overdue customer shipment and a supplier SLA issue require follow-up. Review the evidence-backed risks before acting.',
  actions: [
    { priority: 'HIGH', label: 'Replenish Warehouse Widget', evidence: 'RISK-01 · 18 units versus a 30-unit reorder point' },
    { priority: 'HIGH', label: 'Replenish Temperature Sensor', evidence: 'RISK-02 · 7 units versus a 15-unit reorder point' },
    { priority: 'MEDIUM', label: 'Confirm delivery for Acme Retail', evidence: 'RISK-03 · order is 3 days late' },
    { priority: 'MEDIUM', label: 'Review Summit Components SLA', evidence: 'RISK-04 · 25% of sample deliveries were late' },
  ],
} as const
