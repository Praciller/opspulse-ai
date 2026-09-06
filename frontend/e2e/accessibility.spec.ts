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
