import { describe, it, expect } from 'vitest'
import type { SealedCardInfo } from '@/types'
import { parseQuery } from '../deckbuilder/query'
import { parseTypeLine, poolCardToSummary } from './poolCardSummary'

function card(overrides: Partial<SealedCardInfo> & { name: string; typeLine: string }): SealedCardInfo {
  return {
    manaCost: null,
    rarity: 'COMMON',
    imageUri: null,
    ...overrides,
  }
}

const elf = card({
  name: 'Llanowar Elves',
  manaCost: '{G}',
  typeLine: 'Creature — Elf Druid',
  power: 1,
  toughness: 1,
  oracleText: '{T}: Add {G}.',
  colorIdentity: ['GREEN'],
})
const angel = card({
  name: 'Serra Angel',
  manaCost: '{3}{W}{W}',
  typeLine: 'Creature — Angel',
  rarity: 'UNCOMMON',
  power: 4,
  toughness: 4,
  oracleText: 'Flying, vigilance',
  keywords: ['FLYING', 'VIGILANCE'],
  colorIdentity: ['WHITE'],
})
const bolt = card({
  name: 'Lightning Bolt',
  manaCost: '{R}',
  typeLine: 'Instant',
  oracleText: 'Lightning Bolt deals 3 damage to any target.',
  colorIdentity: ['RED'],
})
const legend = card({
  name: 'Isamaru, Hound of Konda',
  manaCost: '{W}',
  typeLine: 'Legendary Creature — Dog',
  rarity: 'RARE',
  power: 2,
  toughness: 2,
  colorIdentity: ['WHITE'],
})
const werewolf = card({
  name: 'Village Ironsmith',
  manaCost: '{1}{R}',
  typeLine: 'Creature — Human Werewolf',
  power: 1,
  toughness: 1,
  oracleText: 'First strike',
  isDoubleFaced: true,
  backFaceName: 'Ironfang',
  backFaceTypeLine: 'Creature — Werewolf',
  backFaceOracleText: 'First strike\nAt the beginning of each upkeep, transform Ironfang.',
  colorIdentity: ['RED'],
})
const forest = card({ name: 'Forest', typeLine: 'Basic Land — Forest', colorIdentity: ['GREEN'] })

const POOL = [elf, angel, bolt, legend, werewolf, forest]

function search(query: string): string[] {
  const { predicate, errors } = parseQuery(query)
  expect(errors).toEqual([])
  return POOL.filter((c) => predicate(poolCardToSummary(c))).map((c) => c.name)
}

describe('parseTypeLine', () => {
  it('splits supertypes, card types and subtypes around the dash', () => {
    expect(parseTypeLine('Legendary Artifact Creature — Golem Construct')).toEqual({
      supertypes: ['LEGENDARY'],
      cardTypes: ['ARTIFACT', 'CREATURE'],
      subtypes: ['Golem', 'Construct'],
    })
  })

  it('reads both halves of a split type line', () => {
    expect(parseTypeLine('Instant // Sorcery').cardTypes).toEqual(['INSTANT', 'SORCERY'])
  })

  it('treats Tribal as Kindred', () => {
    expect(parseTypeLine('Tribal Instant — Goblin').cardTypes).toEqual(['KINDRED', 'INSTANT'])
  })
})

describe('poolCardToSummary', () => {
  it('derives mana value and cost colours from the printed cost when the server omits cmc', () => {
    const summary = poolCardToSummary(card({ name: 'X', manaCost: '{X}{2/W}{U/B}{1}', typeLine: 'Sorcery' }))
    expect(summary.cmc).toBe(4)
    expect(summary.colors).toEqual(['WHITE', 'BLUE', 'BLACK'])
  })

  it('prefers the server-stamped mana value', () => {
    expect(poolCardToSummary({ ...angel, cmc: 7 }).cmc).toBe(7)
  })
})

describe('pool search', () => {
  it('matches barewords against the card name only, like Scryfall', () => {
    expect(search('bolt')).toEqual(['Lightning Bolt'])
  })

  it('filters by type, colour, mana value and power', () => {
    expect(search('t:creature c:w')).toEqual(['Serra Angel', 'Isamaru, Hound of Konda'])
    expect(search('t:elf')).toEqual(['Llanowar Elves'])
    expect(search('cmc<=1 -t:land')).toEqual(['Llanowar Elves', 'Lightning Bolt', 'Isamaru, Hound of Konda'])
    expect(search('pow>=4')).toEqual(['Serra Angel'])
    expect(search('is:legendary')).toEqual(['Isamaru, Hound of Konda'])
  })

  it('reads rarity, keywords, oracle text and boolean grouping', () => {
    expect(search('r>=uncommon')).toEqual(['Serra Angel', 'Isamaru, Hound of Konda'])
    expect(search('kw:flying')).toEqual(['Serra Angel'])
    expect(search('o:"3 damage"')).toEqual(['Lightning Bolt'])
    expect(search('(t:instant or t:dog) -c:r')).toEqual(['Isamaru, Hound of Konda'])
  })

  it('searches a double-faced card by its back face too', () => {
    expect(search('o:transform')).toEqual(['Village Ironsmith'])
    expect(search('is:dfc')).toEqual(['Village Ironsmith'])
  })

  it('knows basic lands', () => {
    expect(search('is:basic')).toEqual(['Forest'])
  })

  it('reports unknown filters instead of silently matching', () => {
    expect(parseQuery('foo:bar').errors).toHaveLength(1)
  })
})
