import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

test('mock console pages pass the accessibility baseline', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Email').fill('manager@example.test')
  await page.getByLabel('Password').fill('demo-password')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('Operations at a glance')).toBeVisible()

  for (const path of ['/', '/products', '/risks', '/brief', '/reports', '/imports']) {
    await page.goto(path)
    const result = await new AxeBuilder({ page }).analyze()
    expect(result.violations, `${path} accessibility violations`).toEqual([])
  }
})

test('mobile report tables are keyboard reachable and axe clean', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/login')
  await page.getByLabel('Email').fill('manager@example.test')
  await page.getByLabel('Password').fill('demo-password')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await page.goto('/reports')

  for (const { tab, region } of [
    { tab: 'Inventory risk', region: 'Top inventory risks table' },
    { tab: 'Supplier SLA', region: 'Supplier SLA table' },
    { tab: 'Order delay', region: 'Order delay table' },
    { tab: 'Product margin', region: 'Product margin table' },
  ]) {
    await page.getByRole('tab', { name: tab }).click()
    const scrollable = page.getByRole('region', { name: region })
    await expect(scrollable).toBeVisible()
    await scrollable.focus()
    await expect(scrollable).toBeFocused()
    // Wait for the report tab transition before contrast analysis.
    await page.waitForTimeout(250)
    const result = await new AxeBuilder({ page }).analyze()
    expect(result.violations, tab + ' mobile accessibility violations').toEqual([])
  }
})