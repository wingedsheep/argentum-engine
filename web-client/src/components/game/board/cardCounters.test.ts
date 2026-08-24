import { describe, expect, it } from 'vitest'
import { listCardCounters } from './shared'
import { CounterType } from '../../../types'
import type { ClientCard } from '../../../types'

// The hover preview's counter panel is the only place a player can read a permanent's full counter
// inventory. Before it existed the preview showed just the +1/+1 stat line, inside a box gated on
// the card having power/toughness — so a land's counters (City of Shadows' storage) were
// unreachable in the UI even though the server had been sending them all along.

function cardWith(counters: Partial<Record<CounterType, number>>): ClientCard {
  return { counters } as ClientCard
}

describe('listCardCounters', () => {
  it('lists storage counters on a land, which has no power or toughness', () => {
    const cityOfShadows = cardWith({ [CounterType.STORAGE]: 3 })

    expect(listCardCounters(cityOfShadows)).toEqual([
      { type: CounterType.STORAGE, label: 'Storage', count: 3 },
    ])
  })

  it('returns nothing for a card with no counters', () => {
    expect(listCardCounters(cardWith({}))).toEqual([])
  })

  it('omits counter types that dropped to zero rather than showing "0"', () => {
    expect(listCardCounters(cardWith({ [CounterType.STORAGE]: 0, [CounterType.HUNGER]: 2 })))
      .toEqual([{ type: CounterType.HUNGER, label: 'Hunger', count: 2 }])
  })

  it('lists every type on a card at once, biggest pile first', () => {
    const entries = listCardCounters(cardWith({
      [CounterType.HUNGER]: 1,
      [CounterType.PLUS_ONE_PLUS_ONE]: 4,
      [CounterType.STORAGE]: 2,
    }))

    expect(entries.map((entry) => entry.label)).toEqual(['+1/+1', 'Storage', 'Hunger'])
    expect(entries.map((entry) => entry.count)).toEqual([4, 2, 1])
  })

  it('breaks count ties by label so the order never jitters between renders', () => {
    const entries = listCardCounters(cardWith({
      [CounterType.STORAGE]: 2,
      [CounterType.HUNGER]: 2,
      [CounterType.CHARGE]: 2,
    }))

    expect(entries.map((entry) => entry.label)).toEqual(['Charge', 'Hunger', 'Storage'])
  })

  it('falls back to a readable label for a counter the client mirror has not named yet', () => {
    // Server sends the engine enum name; a client built before that type existed still shows it
    // rather than dropping the row.
    const entries = listCardCounters({ counters: { FUTURE_COUNTER: 5 } } as unknown as ClientCard)

    expect(entries).toEqual([{ type: 'FUTURE_COUNTER', label: 'Future counter', count: 5 }])
  })
})
