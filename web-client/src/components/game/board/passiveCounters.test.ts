import { describe, expect, it } from 'vitest'
import { PASSIVE_COUNTER_TYPES } from './shared'
import { fallbackCounterPalette, passiveCounterBadgeStyle } from './styles'
import { counterManaClass } from '../../../assets/icons/keywords'
import { CounterType, CounterTypeDisplayNames } from '../../../types'

// A passive counter's badge is assembled from three independent maps keyed by the CounterType
// name — the icon (`counterManaClass`), the colors (`passiveCounterPalette`, reached through
// `passiveCounterBadgeStyle`), and the label (`CounterTypeDisplayNames`). Adding a counter to
// `PASSIVE_COUNTER_TYPES` while forgetting one of them fails silently: a missing icon renders
// `ms ms-undefined` (no glyph at all), a missing palette renders anonymous grey. Both are only
// visible by putting the card on a battlefield and looking, which is how omen counters
// (Soulcipher Board) and bore counters (Brass's Tunnel-Grinder) went unrendered.

describe('passive counter badge wiring', () => {
  it.each(PASSIVE_COUNTER_TYPES)('%s has a mana-font icon class', (type) => {
    expect(counterManaClass[type]).toBeTruthy()
  })

  it.each(PASSIVE_COUNTER_TYPES)('%s has its own palette, not the grey fallback', (type) => {
    expect(passiveCounterBadgeStyle(type).backgroundColor).not.toBe(fallbackCounterPalette.bg)
  })

  it('renders omen counters (Soulcipher Board)', () => {
    expect(PASSIVE_COUNTER_TYPES).toContain(CounterType.OMEN)
    expect(CounterTypeDisplayNames[CounterType.OMEN]).toBe('Omen')
  })
})
