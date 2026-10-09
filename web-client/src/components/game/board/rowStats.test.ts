import { describe, expect, it } from 'vitest'
import type { GroupedCard } from '@/store/selectors.ts'
import type { ClientCard, EntityId } from '@/types'
import { isStackExpanded, rowStats } from './rowStats'

const card = (id: string, isTapped = false) => ({ id, isTapped }) as unknown as ClientCard

const group = (ids: string[], isTapped = false): GroupedCard => {
  const cards = ids.map((id) => card(id, isTapped))
  return { card: cards[0]!, count: cards.length, cardIds: ids as EntityId[], cards }
}

const NONE: ReadonlySet<EntityId> = new Set()

describe('rowStats', () => {
  it('counts a collapsed stack as one item plus its peeking cards', () => {
    expect(rowStats(NONE, 4, [group(['a', 'b', 'c']), group(['d'])])).toEqual({
      count: 2,
      tapped: 0,
      stackedExtra: 2,
    })
  })

  it('counts an ungrouped stack as every card it renders, with no peeks', () => {
    const expanded = new Set(['b'] as EntityId[])
    expect(rowStats(expanded, 4, [group(['a', 'b', 'c']), group(['d'])])).toEqual({
      count: 4,
      tapped: 0,
      stackedExtra: 0,
    })
  })

  it('caps the peeking cards at the visible-layers preference', () => {
    const horde = group(['a', 'b', 'c', 'd', 'e', 'f'])
    expect(rowStats(NONE, 4, [horde]).stackedExtra).toBe(3)
    expect(rowStats(NONE, 2, [horde]).stackedExtra).toBe(1)
    expect(rowStats(NONE, 8, [horde]).stackedExtra).toBe(5)
  })

  it('an ungrouped tapped stack reserves a sideways footprint for each member', () => {
    const expanded = new Set(['a'] as EntityId[])
    expect(rowStats(expanded, 4, [group(['a', 'b'], true)])).toEqual({ count: 2, tapped: 2, stackedExtra: 0 })
  })
})

describe('isStackExpanded', () => {
  it('never treats a single card as an expanded stack', () => {
    expect(isStackExpanded(group(['a']), new Set(['a'] as EntityId[]))).toBe(false)
  })
})
