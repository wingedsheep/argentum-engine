import { describe, expect, it } from 'vitest'
import { shouldShowCastModal } from './shared'
import type { LegalActionInfo } from '../../../types'
import { entityId } from '../../../types'

// --- Fixture builders -------------------------------------------------------
// Minimal LegalActionInfo objects — `shouldShowCastModal` only reads `actionType`
// and `action.type`, so we keep these as small as the type allows.

const PLAYER = entityId('p1')
const CARD = entityId('c1')

function castSpell(opts: { affordable?: boolean } = {}): LegalActionInfo {
  return {
    actionType: 'CastSpell',
    description: 'Cast',
    action: { type: 'CastSpell', playerId: PLAYER, cardId: CARD },
    ...(opts.affordable !== undefined ? { isAffordable: opts.affordable } : {}),
  }
}

function cycle(opts: { affordable?: boolean } = {}): LegalActionInfo {
  return {
    actionType: 'CycleCard',
    description: 'Cycle',
    action: { type: 'CycleCard', playerId: PLAYER, cardId: CARD },
    ...(opts.affordable !== undefined ? { isAffordable: opts.affordable } : {}),
  }
}

function typecycle(): LegalActionInfo {
  return {
    actionType: 'TypecycleCard',
    description: 'Plainscycling',
    action: { type: 'TypecycleCard', playerId: PLAYER, cardId: CARD },
  }
}

function plot(): LegalActionInfo {
  return {
    actionType: 'PlotCard',
    description: 'Plot',
    action: { type: 'PlotCard', playerId: PLAYER, cardId: CARD },
  }
}

function morph(): LegalActionInfo {
  return {
    actionType: 'CastFaceDown',
    description: 'Cast Face-Down',
    action: { type: 'CastSpell', playerId: PLAYER, cardId: CARD, castFaceDown: true },
  }
}

function playLand(): LegalActionInfo {
  return {
    actionType: 'PlayLand',
    description: 'Play land',
    action: { type: 'PlayLand', playerId: PLAYER, cardId: CARD },
  }
}

describe('shouldShowCastModal', () => {
  it('does not open the menu when there are no legal actions', () => {
    expect(shouldShowCastModal([])).toBe(false)
  })

  it('does not open the menu for a single plain cast', () => {
    expect(shouldShowCastModal([castSpell()])).toBe(false)
  })

  it('opens the menu when both cast and cycle are affordable (two actions)', () => {
    expect(shouldShowCastModal([castSpell(), cycle()])).toBe(true)
  })

  // The core fix: a cycling card the player can only afford to *cycle*. The server omits
  // the unaffordable normal cast, so the only legal action is CycleCard — but dragging it
  // must still open the menu (with a grayed-out "Cast") so the player can choose or cancel
  // rather than silently cycling a card they meant to hard-cast.
  it('opens the menu for a card with only the cycle action affordable', () => {
    expect(shouldShowCastModal([cycle({ affordable: true })])).toBe(true)
  })

  it('opens the menu even when the cycle action itself is unaffordable', () => {
    expect(shouldShowCastModal([cycle({ affordable: false })])).toBe(true)
  })

  it('opens the menu for a card with only typecycling', () => {
    expect(shouldShowCastModal([typecycle()])).toBe(true)
  })

  it('opens the menu for a card with only a plot action', () => {
    expect(shouldShowCastModal([plot()])).toBe(true)
  })

  it('opens the menu for a cycling land that already played a land (cycle only)', () => {
    expect(shouldShowCastModal([cycle()])).toBe(true)
  })

  it('opens the menu for a cycling land with both play-land and cycle', () => {
    expect(shouldShowCastModal([playLand(), cycle()])).toBe(true)
  })

  it('does not open the menu for a plain land with only a play-land action', () => {
    expect(shouldShowCastModal([playLand()])).toBe(false)
  })

  it('opens the menu for multiple casting variants (morph + normal cast)', () => {
    expect(shouldShowCastModal([castSpell(), morph()])).toBe(true)
  })
})
