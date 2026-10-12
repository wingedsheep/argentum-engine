import { describe, expect, it } from 'vitest'
import type { CardSummary } from '../cardFilter'
import { namesPhrase, reachesOutsideTheGame, summarizeOutsideTheGame } from '../outsideTheGame'

function card(name: string, oracleText: string, subtypes: string[] = []): CardSummary {
  return {
    name,
    manaCost: '{1}{R}',
    cmc: 2,
    colors: ['RED'],
    colorIdentity: ['RED'],
    cardTypes: ['SORCERY'],
    supertypes: [],
    subtypes,
    basicLand: false,
    rarity: 'COMMON',
    setCode: null,
    collectorNumber: null,
    oracleText,
  } as CardSummary
}

const catalog: Record<string, CardSummary> = {
  'Burning Wish': card(
    'Burning Wish',
    'You may reveal a sorcery card you own from outside the game and put it into your hand. Exile Burning Wish.',
  ),
  Eyetwitch: card('Eyetwitch', 'When Eyetwitch dies, learn. (You may reveal a Lesson card you own from outside the game…)'),
  Shock: card('Shock', 'Shock deals 2 damage to any target.'),
  'Introduction to Annihilation': card('Introduction to Annihilation', 'Exile target nonland permanent.', ['Lesson']),
}

describe('outside-the-game summary', () => {
  it('recognises wishes and Learn cards by their Oracle text', () => {
    expect(reachesOutsideTheGame(catalog['Burning Wish'])).toBe(true)
    expect(reachesOutsideTheGame(catalog['Eyetwitch'])).toBe(true)
    expect(reachesOutsideTheGame(catalog['Shock'])).toBe(false)
    expect(reachesOutsideTheGame(undefined)).toBe(false)
  })

  it('lists the deck cards that reach outside the game and counts Lessons in the sideboard', () => {
    const summary = summarizeOutsideTheGame(
      { 'Burning Wish': 2, Eyetwitch: 1, Shock: 4 },
      { 'Introduction to Annihilation': 1, Shock: 2 },
      catalog,
    )
    expect(summary.reachers).toEqual(['Burning Wish', 'Eyetwitch'])
    expect(summary.learns).toBe(true)
    expect(summary.lessons).toBe(1)
    expect(summary.size).toBe(3)
  })

  it('says nothing about a deck that never reaches outside the game', () => {
    const summary = summarizeOutsideTheGame({ Shock: 4 }, {}, catalog)
    expect(summary.reachers).toEqual([])
    expect(summary.learns).toBe(false)
  })

  it('phrases one, two or many names', () => {
    expect(namesPhrase(['Burning Wish'])).toBe('Burning Wish')
    expect(namesPhrase(['Burning Wish', 'Eyetwitch'])).toBe('Burning Wish and Eyetwitch')
    expect(namesPhrase(['A', 'B', 'C'])).toBe('A and 2 others')
  })
})
