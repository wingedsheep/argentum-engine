import { describe, it, expect } from 'vitest'
import { groupCards, computeCardGroupKey, visibleStackDepth, MAX_VISUAL_STACK_DEPTH } from './cardGrouping'
import type { ClientCard } from '@/types'
import { entityId } from '@/types'

/**
 * Minimal battlefield-permanent factory. Defaults every field that
 * computeCardGroupKey reads so a bare token groups with its twins; override only
 * the divergence axis under test.
 */
function token({ id, ...over }: { id: string } & Record<string, unknown>): ClientCard {
  return {
    id: entityId(id),
    name: 'Saproling',
    counters: {},
    power: 1,
    toughness: 1,
    basePower: 1,
    baseToughness: 1,
    damage: 0,
    attachments: [],
    linkedExile: [],
    keywords: [],
    abilityFlags: [],
    protections: [],
    hexproofFromColors: [],
    isTapped: false,
    isAttacking: false,
    isBlocking: false,
    attackingTarget: null,
    blockingTarget: null,
    hasSummoningSickness: false,
    isPhasedOut: false,
    isTransformed: false,
    isFaceDown: false,
    isSuspected: false,
    isRingBearer: false,
    isCommander: false,
    nonLegendaryCopy: false,
    ...over,
  } as unknown as ClientCard
}

describe('groupCards — token quantity aggregation (display layer)', () => {
  it('collapses N identical tokens into ONE group regardless of size', () => {
    const cards = Array.from({ length: 10_000 }, (_, i) => token({ id: `e${i}` }))
    const groups = groupCards(cards)
    expect(groups).toHaveLength(1)
    expect(groups[0]!.count).toBe(10_000)
    // Every member is retained for action handling, even though only a few render.
    expect(groups[0]!.cardIds).toHaveLength(10_000)
    expect(groups[0]!.cards).toHaveLength(10_000)
    // Representative is the first member.
    expect(groups[0]!.card.id).toBe(entityId('e0'))
  })

  it.each([
    ['a +1/+1 counter', { counters: { PLUS_ONE_PLUS_ONE: 1 }, power: 2, toughness: 2 }],
    ['tapped', { isTapped: true }],
    ['marked damage', { damage: 1 }],
    ['attacking', { isAttacking: true, attackingTarget: entityId('opp') }],
    ['blocking', { isBlocking: true, blockingTarget: entityId('atk') }],
    ['summoning sickness', { hasSummoningSickness: true }],
    ['a granted keyword', { keywords: ['FLYING'] }],
    ['transformed', { isTransformed: true }],
  ])('splits a member that diverges by %s back into its own group', (_label, diff) => {
    const groups = groupCards([
      token({ id: 'plain1' }),
      token({ id: 'plain2' }),
      token({ id: 'diverged', ...diff }),
    ])
    expect(groups).toHaveLength(2)
    const plain = groups.find((g) => g.cardIds.includes(entityId('plain1')))!
    expect(plain.count).toBe(2)
    const diverged = groups.find((g) => g.cardIds.includes(entityId('diverged')))!
    expect(diverged.count).toBe(1)
  })

  it('never groups a permanent that has attachments (auras/equipment)', () => {
    const groups = groupCards([
      token({ id: 'bare1' }),
      token({ id: 'bare2' }),
      token({ id: 'enchanted', attachments: [entityId('aura')] }),
    ])
    // bare1+bare2 collapse; the enchanted one stands alone (key uses its unique id).
    expect(groups).toHaveLength(2)
    expect(groups.find((g) => g.cardIds.includes(entityId('enchanted')))!.count).toBe(1)
  })

  it('keeps distinct token kinds in separate groups', () => {
    const groups = groupCards([
      token({ id: 's1', name: 'Saproling' }),
      token({ id: 'z1', name: 'Zombie' }),
      token({ id: 's2', name: 'Saproling' }),
    ])
    expect(groups).toHaveLength(2)
    expect(groups.find((g) => g.card.name === 'Saproling')!.count).toBe(2)
    expect(groups.find((g) => g.card.name === 'Zombie')!.count).toBe(1)
  })

  it('handles an empty battlefield', () => {
    expect(groupCards([])).toEqual([])
  })

  it('splits a targeted member out of its group so its arrow can anchor', () => {
    const groups = groupCards(
      [token({ id: 't1' }), token({ id: 't2' }), token({ id: 'targeted' }), token({ id: 't4' })],
      new Set([entityId('targeted')]),
    )
    // The three untouched twins still collapse; the targeted one stands alone.
    expect(groups).toHaveLength(2)
    const solo = groups.find((g) => g.cardIds.includes(entityId('targeted')))!
    expect(solo.count).toBe(1)
    expect(groups.find((g) => g.cardIds.includes(entityId('t1')))!.count).toBe(3)
  })

  it('splits two identical targeted members into separate singletons', () => {
    const groups = groupCards(
      [token({ id: 'a' }), token({ id: 'b' })],
      new Set([entityId('a'), entityId('b')]),
    )
    expect(groups).toHaveLength(2)
    expect(groups.every((g) => g.count === 1)).toBe(true)
  })
})

describe('visibleStackDepth', () => {
  it('caps the rendered depth at MAX_VISUAL_STACK_DEPTH', () => {
    expect(visibleStackDepth(10_000)).toBe(MAX_VISUAL_STACK_DEPTH)
    expect(visibleStackDepth(MAX_VISUAL_STACK_DEPTH + 1)).toBe(MAX_VISUAL_STACK_DEPTH)
  })

  it('returns the true count when below the cap', () => {
    expect(visibleStackDepth(1)).toBe(1)
    expect(visibleStackDepth(3)).toBe(3)
    expect(visibleStackDepth(0)).toBe(0)
  })
})

describe('computeCardGroupKey', () => {
  it('is identical for two bare twins and differs once one is tapped', () => {
    expect(computeCardGroupKey(token({ id: 'a' }))).toBe(computeCardGroupKey(token({ id: 'b' })))
    expect(computeCardGroupKey(token({ id: 'a' }))).not.toBe(
      computeCardGroupKey(token({ id: 'b', isTapped: true })),
    )
  })
})
