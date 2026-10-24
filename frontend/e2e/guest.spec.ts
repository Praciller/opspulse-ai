import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

test('HR can explore the guest demo immediately without a login or backend traffic', async ({ page }) => {
  const backendCalls: string[] = []
  page.on('request', request => {
    if (request.url().includes('opspulse-ai-7gle.onrender.com') || request.url().includes('/api/')) backendCalls.push(request.url())
  })

  await page.goto('/')
  await expect(page).toHaveURL(/\/demo$/)
  await expect(page.getByRole('heading', { name: 'Operations at a glance' })).toBeVisible()
  await expect(page.getByText(/synthetic data · public guest demo/i)).toBeVisible()

  await page.getByRole('link', { name: 'Products', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Inventory and product health' })).toBeVisible()
  await page.getByRole('textbox', { name: 'Search example products' }).fill('widget')
  await expect(page.getByText('Warehouse Widget')).toBeVisible()

  await page.getByRole('link', { name: 'Risk events' }).click()
  await page.getByRole('button', { name: 'High' }).click()
  await expect(page.getByText('Widget stock below threshold')).toBeVisible()
  await expect(page.getByText('Supplier SLA below target')).toHaveCount(0)

  await page.getByRole('link', { name: 'Reports' }).click()
  await page.getByRole('tab', { name: 'Supplier SLA' }).click()
  await expect(page.getByRole('region', { name: 'Sample supplier SLA' })).toBeVisible()

  await page.getByRole('link', { name: 'AI brief' }).click()
  await expect(page.getByRole('heading', { name: 'A daily action plan, grounded in data' })).toBeVisible()
  await expect(page.getByText(/static example of a rule-based fallback brief/i)).toBeVisible()
  expect(backendCalls).toEqual([])
})

test('guest demo deep links are public and keyboard-accessible', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/demo/reports')
  await expect(page.getByRole('heading', { name: 'Operational reports' })).toBeVisible()
  await page.getByRole('tab', { name: 'Product margin' }).click()
  await expect(page.getByRole('region', { name: 'Sample product margins' })).toBeVisible()
  const results = await new AxeBuilder({ page }).analyze()
  expect(results.violations).toEqual([])
})
