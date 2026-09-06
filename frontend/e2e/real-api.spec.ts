import { expect, test } from '@playwright/test'

const apiBase = process.env.E2E_API_BASE_URL?.replace(/\/$/, '')
const email = process.env.E2E_EMAIL
const password = process.env.E2E_PASSWORD

test.skip(!apiBase || !email || !password, 'Set E2E_API_BASE_URL, E2E_EMAIL, and E2E_PASSWORD to run against a backend')

test('real API smoke covers login, products, risks, and brief generation', async ({ request }) => {
  const health = await request.get(`${apiBase}/actuator/health`)
  expect(health.ok()).toBeTruthy()

  const login = await request.post(`${apiBase}/api/auth/login`, { data: { email, password } })
  expect(login.ok()).toBeTruthy()
  const tokens = await login.json() as { accessToken: string }
  const headers = { Authorization: `Bearer ${tokens.accessToken}` }

  const products = await request.get(`${apiBase}/api/products?page=0&size=5&sort=createdAt,desc`, { headers })
  expect(products.ok()).toBeTruthy()

  const risks = await request.get(`${apiBase}/api/risks?page=0&size=5&sort=createdAt,desc`, { headers })
  expect(risks.ok()).toBeTruthy()

  const dashboard = await request.get(`${apiBase}/api/dashboard`, { headers })
  expect(dashboard.ok()).toBeTruthy()

  const brief = await request.post(`${apiBase}/api/ai/recommendations/generate`, {
    headers,
    data: { topN: 5 },
  })
  expect(brief.ok()).toBeTruthy()
  const body = await brief.json() as { generatedBy?: string }
  expect(['AI', 'RULE_BASED']).toContain(body.generatedBy)

  for (const path of [
    '/api/imports?page=0&size=5',
    '/api/reports/daily-ops-brief',
    '/api/reports/inventory-risk',
    '/api/reports/supplier-sla',
    '/api/reports/order-delay',
    '/api/reports/product-margin',
  ]) {
    const response = await request.get(`${apiBase}${path}`, { headers })
    expect(response.ok(), path).toBeTruthy()
  }
})
