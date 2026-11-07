import { describe, expect, it } from 'vitest'
import { adjustBasic, countCards, deckChanges, deckGroupOf, matchesColorFilter, moveOne } from './sideboarding'
import type { SealedCardInfo } from '@/types'

describe('sideboarding helpers', () => {
  it('moves one copy between the piles', () => {
    const moved = moveOne({ Shock: 2 }, { Bear: 1 }, 'Shock')
    expect(moved).toEqual([{ Shock: 1 }, { Bear: 1, Shock: 1 }])
    expect(moveOne({}, {}, 'Shock')).toBeNull()
  })

  it('drops a card from its pile when the last copy leaves', () => {
    expect(moveOne({ Shock: 1 }, {}, 'Shock')?.[0]).toEqual({})
  })

  it('never takes a basic land count below zero', () => {
    expect(adjustBasic({ Forest: 1 }, 'Forest', -2)).toEqual({})
    expect(adjustBasic({}, 'Island', 1)).toEqual({ Island: 1 })
  })

  it('lists cards brought in before cards taken out', () => {
    const changes = deckChanges({ Bear: 2, Shock: 1, Forest: 17 }, { Bear: 1, Pacifism: 1, Forest: 18, Shock: 1 })
    expect(changes).toEqual([
      { name: 'Forest', delta: 1 },
      { name: 'Pacifism', delta: 1 },
      { name: 'Bear', delta: -1 },
    ])
    expect(countCards({ Bear: 2, Forest: 17 })).toBe(19)
  })

  it('groups the deck list by creatures, spells and lands', () => {
    const card = (typeLine: string) => ({ typeLine } as SealedCardInfo)
    expect(deckGroupOf(card('Creature — Bear'))).toBe('Creatures')
    expect(deckGroupOf(card('Artifact Creature — Golem'))).toBe('Creatures')
    expect(deckGroupOf(card('Land Creature — Forest Dryad'))).toBe('Creatures')
    expect(deckGroupOf(card('Basic Land — Forest'))).toBe('Lands')
    expect(deckGroupOf(card('Enchantment — Aura'))).toBe('Spells')
  })
})

describe('sideboard colour filter', () => {
  const card = (colorIdentity: string[]) => ({ colorIdentity } as unknown as SealedCardInfo)
  it('shows everything with no chip selected', () => {
    expect(matchesColorFilter(card(['R']), new Set())).toBe(true)
  })
  it('matches any shared colour, and colourless only under C', () => {
    expect(matchesColorFilter(card(['R', 'G']), new Set(['G'] as const))).toBe(true)
    expect(matchesColorFilter(card(['U']), new Set(['G'] as const))).toBe(false)
    expect(matchesColorFilter(card([]), new Set(['G'] as const))).toBe(false)
    expect(matchesColorFilter(card([]), new Set(['C'] as const))).toBe(true)
  })
  it('reads the colour enum names the server sends', () => {
    expect(matchesColorFilter(card(['BLUE']), new Set(['U'] as const))).toBe(true)
    expect(matchesColorFilter(card(['BLUE']), new Set(['G'] as const))).toBe(false)
  })
})
