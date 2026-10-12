import { describe, expect, it } from 'vitest'
import {
  canCardBeCommander,
  canCrown,
  canPairAsCommanders,
  commanderIdentityOf,
  isWithinIdentity,
  toggleCommanderDesignation,
} from './commanderPairing'
import type { CardSummary } from './cardFilter'

type Card = Pick<CardSummary, 'supertypes' | 'cardTypes' | 'subtypes' | 'canBeCommander' | 'partnerAbilities'>

const legend = (extra: Partial<Card> = {}): Card => ({
  supertypes: ['LEGENDARY'], cardTypes: ['CREATURE'], subtypes: [], canBeCommander: true, ...extra,
})
const partner = legend({ partnerAbilities: ['Partner'] })
const friends = legend({ partnerAbilities: ['Partner—Friends forever'] })
const chooser = legend({ partnerAbilities: ['Choose a Background'] })
const background: Card = {
  supertypes: ['LEGENDARY'], cardTypes: ['ENCHANTMENT'], subtypes: ['Background'], canBeCommander: false,
}
const companion = legend({ partnerAbilities: ["Doctor's companion"] })
const doctor = legend({ subtypes: ['Time Lord', 'Doctor'] })
const splitDoctor = legend({ subtypes: ['Time', 'Lord', 'Doctor'] })
const planeswalker: Card = { supertypes: ['LEGENDARY'], cardTypes: ['PLANESWALKER'], subtypes: [] }

describe('canCardBeCommander', () => {
  it('trusts the server flag, and falls back to legendary creature only', () => {
    expect(canCardBeCommander(legend())).toBe(true)
    expect(canCardBeCommander(legend({ canBeCommander: false }))).toBe(false)
    // No flag: a legendary planeswalker is not a commander by default.
    expect(canCardBeCommander(planeswalker)).toBe(false)
    expect(canCardBeCommander({ ...planeswalker, canBeCommander: true })).toBe(true)
    const { canBeCommander: _unused, ...unflagged } = legend()
    expect(canCardBeCommander(unflagged)).toBe(true)
  })
})

describe('canPairAsCommanders', () => {
  it('pairs two cards with partner abilities', () => {
    expect(canPairAsCommanders(partner, partner)).toBe(true)
    expect(canPairAsCommanders(friends, friends)).toBe(true)
    expect(canPairAsCommanders(partner, legend())).toBe(false)
  })

  it('pairs Choose a Background with a legendary Background, either way round', () => {
    expect(canPairAsCommanders(chooser, background)).toBe(true)
    expect(canPairAsCommanders(background, chooser)).toBe(true)
    expect(canPairAsCommanders(partner, background)).toBe(false)
  })

  it("pairs a Doctor's companion with a legendary Time Lord Doctor", () => {
    expect(canPairAsCommanders(companion, doctor)).toBe(true)
    expect(canPairAsCommanders(splitDoctor, companion)).toBe(true)
    expect(canPairAsCommanders(companion, legend())).toBe(false)
  })
})

describe('canCrown', () => {
  it('offers a Background a crown only beside a single Choose a Background commander', () => {
    expect(canCrown(background, false, [])).toBe(false)
    expect(canCrown(background, false, [partner])).toBe(false)
    expect(canCrown(background, false, [chooser])).toBe(true)
    expect(canCrown(background, true, [chooser, background])).toBe(true)
  })

  it('does not crown a planeswalker that lacks the server flag', () => {
    expect(canCrown(planeswalker, false, [])).toBe(false)
  })
})

describe('toggleCommanderDesignation', () => {
  const pairs = (a: string, b: string) => a.startsWith('P') && b.startsWith('P')

  it('designates, pairs, replaces and caps at two', () => {
    expect(toggleCommanderDesignation([], 'P1', pairs)).toEqual(['P1'])
    expect(toggleCommanderDesignation(['P1'], 'P2', pairs)).toEqual(['P1', 'P2'])
    expect(toggleCommanderDesignation(['P1'], 'Solo', pairs)).toEqual(['Solo'])
    expect(toggleCommanderDesignation(['P1', 'P2'], 'P3', pairs)).toEqual(['P3'])
  })

  it('un-designates a commander, moving the second up', () => {
    expect(toggleCommanderDesignation(['P1', 'P2'], 'P1', pairs)).toEqual(['P2'])
    expect(toggleCommanderDesignation(['P1', 'P2'], 'P2', pairs)).toEqual(['P1'])
    expect(toggleCommanderDesignation(['P1'], 'P1', pairs)).toEqual([])
  })
})

describe('commander colour identity', () => {
  it('unions a partner pair and keeps a card only when every colour is inside', () => {
    const identity = commanderIdentityOf([{ colorIdentity: ['GREEN'] }, { colorIdentity: ['RED', 'WHITE'] }])
    expect([...identity].sort()).toEqual(['GREEN', 'RED', 'WHITE'])
    expect(isWithinIdentity({ colorIdentity: ['RED', 'GREEN'] }, identity)).toBe(true)
    expect(isWithinIdentity({ colorIdentity: ['BLUE'] }, identity)).toBe(false)
    expect(isWithinIdentity({ colorIdentity: [] }, identity)).toBe(true)
  })

  it('lets only colourless cards into a colourless commander deck', () => {
    const identity = commanderIdentityOf([{ colorIdentity: [] }])
    expect(isWithinIdentity({ colorIdentity: [] }, identity)).toBe(true)
    expect(isWithinIdentity({ colorIdentity: ['BLACK'] }, identity)).toBe(false)
  })
})
