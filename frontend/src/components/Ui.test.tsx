import { act, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ColdStartNotice, Deferred, StatusPill } from './Ui'

describe('console UI primitives', () => {
  it('renders status and accessible deferred state', () => {
    render(<><StatusPill value="RULE_BASED" /><Deferred title="Reports pending" description="Backend work is deferred." /></>)
    expect(screen.getByText('RULE BASED')).toBeInTheDocument()
    expect(screen.getByText('Reports pending')).toBeInTheDocument()
    expect(screen.getByText('Backend work is deferred.')).toBeInTheDocument()
  })
  it('explains a long backend wake-up only after ten seconds of waiting', () => {
    vi.useFakeTimers()
    try {
      const first = render(<ColdStartNotice />)
      expect(screen.queryByText(/free demo backend may be waking up/i)).not.toBeInTheDocument()
      act(() => { vi.advanceTimersByTime(9_999) })
      expect(screen.queryByText(/free demo backend may be waking up/i)).not.toBeInTheDocument()
      act(() => { vi.advanceTimersByTime(1) })
      expect(screen.getByRole('alert')).toHaveTextContent(/this can take several minutes/i)
      first.unmount()

      const second = render(<ColdStartNotice />)
      expect(screen.queryByText(/free demo backend may be waking up/i)).not.toBeInTheDocument()
      second.unmount()
    } finally {
      vi.useRealTimers()
    }
  })

})
