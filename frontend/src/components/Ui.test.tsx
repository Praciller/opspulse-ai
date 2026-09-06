import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Deferred, StatusPill } from './Ui'

describe('console UI primitives', () => {
  it('renders status and accessible deferred state', () => {
    render(<><StatusPill value="RULE_BASED" /><Deferred title="Reports pending" description="Backend work is deferred." /></>)
    expect(screen.getByText('RULE BASED')).toBeInTheDocument()
    expect(screen.getByText('Reports pending')).toBeInTheDocument()
    expect(screen.getByText('Backend work is deferred.')).toBeInTheDocument()
  })
})
