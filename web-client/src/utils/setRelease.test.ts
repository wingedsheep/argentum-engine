import { describe, expect, it } from 'vitest'
import { isUnreleasedSet, todayIso } from './setRelease'

describe('todayIso', () => {
  it('formats a date as a zero-padded ISO calendar day', () => {
    expect(todayIso(new Date(2025, 0, 5))).toBe('2025-01-05')
    expect(todayIso(new Date(2026, 11, 31))).toBe('2026-12-31')
  })
})

describe('isUnreleasedSet', () => {
  const today = '2026-06-30'

  it('treats a future release date as unreleased', () => {
    expect(isUnreleasedSet('2026-07-01', today)).toBe(true)
    expect(isUnreleasedSet('2030-01-01', today)).toBe(true)
  })

  it('treats today and past dates as released', () => {
    expect(isUnreleasedSet('2026-06-30', today)).toBe(false)
    expect(isUnreleasedSet('2025-09-26', today)).toBe(false)
    expect(isUnreleasedSet('1993-08-05', today)).toBe(false)
  })

  it('treats missing or malformed dates as released (old / undated sets, never future spoilers)', () => {
    expect(isUnreleasedSet(null, today)).toBe(false)
    expect(isUnreleasedSet(undefined, today)).toBe(false)
    expect(isUnreleasedSet('', today)).toBe(false)
    expect(isUnreleasedSet('2026', today)).toBe(false)
    expect(isUnreleasedSet('not-a-date', today)).toBe(false)
  })
})
