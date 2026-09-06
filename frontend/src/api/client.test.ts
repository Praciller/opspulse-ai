import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, authToken } from './client'
import { api } from './api'

describe('API client', () => {
  beforeEach(() => { vi.restoreAllMocks(); sessionStorage.clear(); authToken.set(null) })
  it('adds request and bearer headers', async () => {
    authToken.set('access-token'); const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"ok":true}', { status: 200 }))
    await apiFetch<{ ok: boolean }>('/api/test')
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Headers).get('Authorization')).toBe('Bearer access-token')
    expect((init.headers as Headers).get('X-Request-Id')).toMatch(/^web-/)
  })
  it('refreshes exactly once after an unauthorized response', async () => {
    sessionStorage.setItem('opspulse.refreshToken', 'refresh-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('', { status: 401 })).mockResolvedValueOnce(new Response('{"accessToken":"new","refreshToken":"rotated","user":{"id":"1","email":"a@b.test","fullName":"A","active":true,"roles":["VIEWER"],"createdAt":"now"}}', { status: 200 })).mockResolvedValueOnce(new Response('{"ok":true}', { status: 200 }))
    await apiFetch<{ ok: boolean }>('/api/test')
    expect(fetchMock).toHaveBeenCalledTimes(3); expect(authToken.get()).toBe('new'); expect(sessionStorage.getItem('opspulse.refreshToken')).toBe('rotated')
  })
  it('surfaces the stable error envelope', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"code":"FORBIDDEN","message":"No access","requestId":"r-1"}', { status: 403 }))
    await expect(apiFetch('/api/test')).rejects.toMatchObject({ status: 403, requestId: 'r-1', payload: { code: 'FORBIDDEN' } })
  })
  it('clears the session after refresh is rejected', async () => {
    sessionStorage.setItem('opspulse.refreshToken', 'expired-refresh')
    sessionStorage.setItem('opspulse.user', '{"id":"1"}')
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('', { status: 401 })).mockResolvedValueOnce(new Response('', { status: 401 }))
    const expired = vi.fn()
    window.addEventListener('opspulse:session-expired', expired)
    await expect(apiFetch('/api/test')).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledOnce()
    expect(sessionStorage.getItem('opspulse.refreshToken')).toBeNull()
    expect(authToken.get()).toBeNull()
    window.removeEventListener('opspulse:session-expired', expired)
  })
  it('redacts secret-like values in user-facing errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"message":"provider api_key=sk-live-secret failed"}', { status: 502 }))
    const promise = apiFetch('/api/test')
    await expect(promise).rejects.toThrow('api_key=[redacted]')
    await promise.catch(error => expect((error as Error).message).not.toContain('sk-live-secret'))
  })
  it('serializes paginated risk filters and whitelisted sort values', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"items":[],"page":2,"size":10,"total":0,"totalPages":0}', { status: 200 }))
    await api.risks({ page: 2, size: 10, sort: 'severity,desc', riskType: 'STOCKOUT_RISK', entityType: 'PRODUCT', entityId: 'product-1', from: '2026-07-01T00:00:00Z', to: '2026-07-31T23:59:59Z' })
    const url = String(fetchMock.mock.calls[0][0])
    expect(url).toContain('page=2'); expect(url).toContain('size=10'); expect(url).toContain('sort=severity%2Cdesc'); expect(url).toContain('entityId=product-1'); expect(url).toContain('from=2026-07-01T00%3A00%3A00Z')
  })
  it('sends typed mutation bodies with request IDs', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"id":"order-1","orderNumber":"ORD-2"}', { status: 201 }))
    await api.createOrder({ orderNumber: 'ORD-2', customerName: 'Example', expectedShipDate: '2026-07-20', items: [{ productId: 'product-1', quantity: 2, unitPrice: 10 }] })
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Headers).get('X-Request-Id')).toMatch(/^web-/)
    expect(JSON.parse(String(init.body))).toEqual({ orderNumber: 'ORD-2', customerName: 'Example', expectedShipDate: '2026-07-20', items: [{ productId: 'product-1', quantity: 2, unitPrice: 10 }] })
  })
  it('serializes purchase-order creation against the backend contract', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"id":"po-2"}', { status: 201 }))
    await api.createPurchaseOrder({ poNumber: 'PO-2', supplierId: 'supplier-1', expectedDeliveryDate: '2026-07-25', items: [{ productId: 'product-1', quantity: 4, unitCost: 8 }] })
    expect(String(fetchMock.mock.calls[0][0])).toBe('http://localhost:8080/api/purchase-orders')
    expect(JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body))).toMatchObject({ poNumber: 'PO-2', supplierId: 'supplier-1', items: [{ quantity: 4, unitCost: 8 }] })
  })
  it('preserves conflict status and request ID for stale mutations', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"code":"CONFLICT","message":"Version is stale","requestId":"req-conflict"}', { status: 409 }))
    await expect(apiFetch('/api/products/product-1', { method: 'PUT', body: '{}' })).rejects.toMatchObject({ status: 409, requestId: 'req-conflict', payload: { code: 'CONFLICT' } })
  })
})
