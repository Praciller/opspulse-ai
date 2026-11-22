import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ReportsPage } from './DeferredPages'

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    reportDailyOpsBrief: vi.fn(),
    reportInventoryRisk: vi.fn(),
    reportSupplierSla: vi.fn(),
    reportOrderDelay: vi.fn(),
    reportProductMargin: vi.fn(),
  },
}))

vi.mock('../api/api', () => ({ api: apiMock }))

describe('Reports page availability', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    apiMock.reportDailyOpsBrief
      .mockRejectedValueOnce(new Error('Service unavailable'))
      .mockResolvedValue({ summary: 'Daily briefing', generatedBy: 'RULE_BASED' })
    apiMock.reportInventoryRisk.mockResolvedValue({
      asOf: '2026-10-24T00:00:00Z',
      bySeverity: { HIGH: 1 },
      topRisks: [],
    })
    apiMock.reportSupplierSla.mockResolvedValue({ suppliers: [] })
    apiMock.reportOrderDelay.mockResolvedValue({ delayedOrders: [] })
    apiMock.reportProductMargin.mockResolvedValue({ lowMarginCount: 0, products: [] })
  })

  it('does not display an empty report as valid data after a failed request, and retries safely', async () => {
    render(<ReportsPage />)

    expect(await screen.findByText(/Service unavailable/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Top inventory risks table' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('region', { name: 'Top inventory risks table' })).toBeInTheDocument()
    expect(apiMock.reportDailyOpsBrief).toHaveBeenCalledTimes(2)
  })
})
