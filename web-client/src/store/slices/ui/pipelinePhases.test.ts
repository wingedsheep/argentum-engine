import { describe, it, expect } from 'vitest'
import { computePhases } from './pipelinePhases'
import type { LegalActionInfo } from '@/types/messages'

/**
 * Minimal CastSpell LegalActionInfo factory — only the fields computePhases reads matter.
 */
function castAction(over: Record<string, unknown>): LegalActionInfo {
  return {
    actionType: 'CastSpellModal',
    description: 'Cast Test',
    action: { type: 'CastSpell', playerId: 'p1', cardId: 'c1' },
    ...over,
  } as unknown as LegalActionInfo
}

describe('computePhases — choose-N modal', () => {
  it('plain choose-N modal (Spree) runs only the modalModes phase', () => {
    const info = castAction({
      modalEnumeration: {
        chooseCount: 3,
        minChooseCount: 1,
        allowRepeat: true,
        modes: [],
      },
    })
    expect(computePhases(info)).toEqual([{ type: 'modalModes' }])
  })

  it('"choose both if you blight" modal (Pyrrhic Strike) also collects the blight target', () => {
    // The engine forces every mode on the blight variant but only unlocks the extra modes
    // once the submitted action carries blightTargets. The client must therefore run a
    // costPayment phase to pick the creature to blight — otherwise the action submits with
    // no blight and the engine rejects it ("Too many modes chosen").
    const info = castAction({
      modalEnumeration: {
        chooseCount: 2,
        minChooseCount: 2,
        allowRepeat: false,
        modes: [],
      },
      additionalCostInfo: {
        costType: 'Blight',
        description: 'creature to blight',
        validBlightTargets: ['fodder1'],
        blightAmount: 2,
      },
    })
    expect(computePhases(info)).toEqual([{ type: 'modalModes' }, { type: 'costPayment' }])
  })
})

describe('computePhases — emerge sacrifice', () => {
  function emergeAction(): LegalActionInfo {
    return castAction({
      actionType: 'CastWithAlternativeCost',
      action: {
        type: 'CastSpell',
        playerId: 'p1',
        cardId: 'c1',
        useAlternativeCost: true,
        alternativeCostType: 'EMERGE',
      },
      manaCostString: '{5}{U}',
      additionalCostInfo: {
        costType: 'SacrificePermanent',
        description: 'a creature to sacrifice (its mana value reduces the emerge cost)',
        validSacrificeTargets: ['bear', 'ogre'],
        sacrificeCount: 1,
        costAfterSacrifice: { bear: '{3}{U}', ogre: '{U}' },
      },
      availableManaSources: [{ entityId: 'island', producesColors: ['U'] }],
    })
  }

  it('picks the sacrifice BEFORE manual mana-source selection, since it changes the cost owed', () => {
    // With auto-tap off the mana step would otherwise run first and price the cast against the
    // un-reduced emerge cost — the player would be asked to tap for {5}{U} and then discover the
    // sacrifice made it {U}.
    expect(computePhases(emergeAction(), { autoTapEnabled: false })).toEqual([
      { type: 'costPayment' },
      { type: 'manaSource' },
    ])
  })

  it('runs the sacrifice step exactly once when auto-tap handles the mana', () => {
    expect(computePhases(emergeAction(), { autoTapEnabled: true })).toEqual([
      { type: 'costPayment' },
    ])
  })
})

describe('computePhases — tap-for-generic (improvise / waterbend)', () => {
  function tapAction(): LegalActionInfo {
    return castAction({
      actionType: 'CastSpell',
      manaCostString: '{4}{U}',
      hasTapForGeneric: true,
      tapForGenericLabel: 'improvise',
      validTapForGenericPermanents: [{ entityId: 'rock', name: 'Arc Reactor', isCreature: false }],
      availableManaSources: [{ entityId: 'island', producesColors: ['U'] }],
    })
  }

  it('offers the tap step but leaves auto-tap alone', () => {
    // Improvise is grantable over a whole card type (Ironheart, Clever Champion gives every
    // noncreature spell you cast improvise), so forcing the manaSource phase the way delve and
    // convoke do would silently disable auto-tap for the rest of the game. The server applies the
    // taps and auto-solves the remainder, so the extra confirmation buys nothing.
    expect(computePhases(tapAction(), { autoTapEnabled: true })).toEqual([
      { type: 'tapForGeneric' },
    ])
  })

  it('still runs manual mana selection after the taps when auto-tap is off', () => {
    expect(computePhases(tapAction(), { autoTapEnabled: false })).toEqual([
      { type: 'tapForGeneric' },
      { type: 'manaSource' },
    ])
  })

  it('waterbend gets the same treatment — the taps do not force a mana-source confirm', () => {
    // Waterbend used to sit alongside delve and convoke in the force-manaSource list. It was
    // moved out with improvise deliberately, not incidentally: the server applies the taps and
    // then auto-solves the remainder for both mechanics, so under auto-tap the extra confirm
    // step bought nothing on either. Pinned here so the older mechanic's UX can't drift back
    // unnoticed.
    const waterbendAction = castAction({
      actionType: 'CastSpell',
      manaCostString: '{3}{U}',
      hasTapForGeneric: true,
      tapForGenericLabel: 'waterbend',
      tapForGenericAmount: 2,
      validTapForGenericPermanents: [{ entityId: 'bender', name: 'Katara', isCreature: true }],
      availableManaSources: [{ entityId: 'island', producesColors: ['U'] }],
    })
    expect(computePhases(waterbendAction, { autoTapEnabled: true })).toEqual([
      { type: 'tapForGeneric' },
    ])
  })
})
