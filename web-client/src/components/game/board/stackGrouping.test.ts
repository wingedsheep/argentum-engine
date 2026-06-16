import { describe, it, expect } from 'vitest'
import { groupStackCards, stackGroupKey } from './stackGrouping'
import type { ClientCard } from '@/types/gameState'

/** Minimal ClientCard factory — only the fields stackGroupKey reads matter here. */
function card(over: { id: string } & Record<string, unknown>): ClientCard {
  return {
    name: 'Lightning Bolt',
    typeLine: 'Instant',
    controllerId: 'p1',
    oracleText: 'Deal 3 damage.',
    ...over,
  } as unknown as ClientCard
}

describe('stackGroupKey', () => {
  it('matches identical spells regardless of target', () => {
    const a = card({ id: '1' })
    const b = card({ id: '2' })
    expect(stackGroupKey(a)).toBe(stackGroupKey(b))
  })

  it('distinguishes different controllers', () => {
    expect(stackGroupKey(card({ id: '1', controllerId: 'p1' })))
      .not.toBe(stackGroupKey(card({ id: '2', controllerId: 'p2' })))
  })

  it('distinguishes different effect text and chosen X', () => {
    expect(stackGroupKey(card({ id: '1', stackText: 'Draw a card.' })))
      .not.toBe(stackGroupKey(card({ id: '2', stackText: 'Gain 3 life.' })))
    expect(stackGroupKey(card({ id: '1', chosenX: 2 })))
      .not.toBe(stackGroupKey(card({ id: '2', chosenX: 5 })))
  })

  it('ignores copyIndex so storm copies group together', () => {
    expect(stackGroupKey(card({ id: '1', copyIndex: 1, copyTotal: 3 })))
      .toBe(stackGroupKey(card({ id: '2', copyIndex: 2, copyTotal: 3 })))
  })
})

describe('groupStackCards', () => {
  it('returns one single-member group per distinct item', () => {
    const groups = groupStackCards([
      card({ id: '1', name: 'Bolt' }),
      card({ id: '2', name: 'Counterspell' }),
    ])
    expect(groups).toHaveLength(2)
    expect(groups.every((g) => g.items.length === 1)).toBe(true)
  })

  it('collapses a contiguous run of identical items', () => {
    const groups = groupStackCards([
      card({ id: '1' }),
      card({ id: '2' }),
      card({ id: '3' }),
    ])
    expect(groups).toHaveLength(1)
    expect(groups[0]!.items.map((c) => c.id)).toEqual(['1', '2', '3'])
    expect(groups[0]!.groupId).toBe('1')
  })

  it('does NOT merge identical items split by a different item (order is meaningful)', () => {
    const groups = groupStackCards([
      card({ id: '1', name: 'Bolt' }),
      card({ id: '2', name: 'Counterspell' }),
      card({ id: '3', name: 'Bolt' }),
    ])
    expect(groups).toHaveLength(3)
  })

  it('preserves overall stack order across groups', () => {
    const groups = groupStackCards([
      card({ id: '1', name: 'Bolt' }),
      card({ id: '2', name: 'Bolt' }),
      card({ id: '3', name: 'Counterspell' }),
    ])
    expect(groups.map((g) => g.items.length)).toEqual([2, 1])
    expect(groups[1]!.items[0]!.name).toBe('Counterspell')
  })

  it('handles an empty stack', () => {
    expect(groupStackCards([])).toEqual([])
  })
})
