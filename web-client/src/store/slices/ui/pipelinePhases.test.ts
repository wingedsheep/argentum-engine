import { describe, it, expect } from 'vitest'
import { computePhases, enterPhase, mergeResult } from './pipelinePhases'
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

describe('computePhases — Phyrexian mana', () => {
  it('opens manual payment even when auto-tap is enabled so life is selectable', () => {
    const info = castAction({
      manaCostString: '{1}{B/P}{B/P}',
      availableManaSources: [{ entityId: 'swamp', producesColors: ['B'] }],
    })

    expect(computePhases(info, { autoTapEnabled: true })).toEqual([{ type: 'manaSource' }])
  })

  it('opens manual payment for a Phyrexian-only cost with no mana sources', () => {
    const info = castAction({
      manaCostString: '{B/P}',
      availableManaSources: [],
    })

    expect(computePhases(info, { autoTapEnabled: true })).toEqual([{ type: 'manaSource' }])
  })
})

describe('Force of Vigor hand-exile payment', () => {
  const info = castAction({
    actionType: 'CastWithAlternativeCost',
    description: 'Cast Force of Vigor ({0})',
    action: {
      type: 'CastSpell',
      playerId: 'p1',
      cardId: 'force',
      useAlternativeCost: true,
      alternativeCostType: 'SELF_ALTERNATIVE',
    },
    additionalCostInfo: {
      costType: 'ExileFromHand',
      description: 'Exile a green card from your hand',
      validExileTargets: ['green-card'],
      exileMinCount: 1,
      exileMaxCount: 1,
    },
  })

  it('opens a hand selection phase and exposes the eligible green card', () => {
    expect(computePhases(info, { autoTapEnabled: true })).toEqual([{ type: 'costPayment' }])

    let captured: Record<string, unknown> | null = null
    const store = {
      startTargeting: (arg: Record<string, unknown>) => { captured = arg },
    } as unknown as Parameters<typeof enterPhase>[3]
    enterPhase({ type: 'costPayment' }, info, info.action, store)

    expect(captured).toMatchObject({
      validTargets: ['green-card'],
      minTargets: 1,
      maxTargets: 1,
      targetZone: 'Hand',
    })
  })

  it('submits the chosen card through additionalCostPayment.exiledCards', () => {
    const action = mergeResult(
      info.action,
      info,
      { type: 'costPayment', costType: 'ExileFromHand', selectedTargets: ['green-card' as never] },
      {} as never,
    )
    expect(action).toMatchObject({
      additionalCostPayment: { exiledCards: ['green-card'] },
    })
  })
})

describe('enterPhase — sum-gated graveyard exile costs', () => {
  /**
   * Collect evidence N (CR 701.59a) and Baron Helmut Zemo's pip total are the same picker: any
   * number of graveyard cards, gated on a summed measure the *server* computes. Both are pinned
   * here because the two used to be separate client branches — one summing card mana values it
   * looked up itself, one summing a server weight table — and the shared branch is only correct
   * as long as the server ships weights for both.
   */
  function exileCostAction(costType: string, costInfo: Record<string, unknown>): LegalActionInfo {
    return castAction({
      actionType: 'ActivateAbility',
      description: 'Activate Baron Helmut Zemo',
      additionalCostInfo: { costType, description: 'Exile cards', ...costInfo },
    })
  }

  function captureTargeting(info: LegalActionInfo): Record<string, unknown> | null {
    let captured: Record<string, unknown> | null = null
    const store = {
      startTargeting: (arg: Record<string, unknown>) => {
        captured = arg
      },
    } as unknown as Parameters<typeof enterPhase>[3]
    enterPhase({ type: 'costPayment' }, info, info.action, store)
    return captured
  }

  it('collect evidence gates Confirm on the server weight table, not on client-side mana values', () => {
    const captured = captureTargeting(
      exileCostAction('CollectEvidence', {
        validExileTargets: ['a', 'b'],
        exileMinCount: 1,
        exileMaxCount: 2,
        exileMinTotalWeight: 6,
        exileCardWeights: { a: 4, b: 2 },
        exileWeightUnit: 'mana value',
      }),
    )
    expect(captured).toMatchObject({
      validTargets: ['a', 'b'],
      minTargets: 1,
      maxTargets: 2,
      minTotalWeight: 6,
      cardWeights: { a: 4, b: 2 },
      weightUnit: 'mana value',
      targetZone: 'Graveyard',
    })
  })

  it('an ExileForTotal cost takes the identical path with its own unit', () => {
    const captured = captureTargeting(
      exileCostAction('ExileForTotal', {
        validExileTargets: ['x', 'y'],
        exileMinCount: 1,
        exileMaxCount: 2,
        exileMinTotalWeight: 15,
        exileCardWeights: { x: 9, y: 6 },
        exileWeightUnit: 'black mana symbols',
      }),
    )
    expect(captured).toMatchObject({
      minTotalWeight: 15,
      cardWeights: { x: 9, y: 6 },
      weightUnit: 'black mana symbols',
    })
  })
})

describe('computePhases — per-target mana tax (Officious Interrogation)', () => {
  /**
   * `manaCostString` on a per-target-taxed spell is only the one-target minimum, so picking mana
   * sources before targeting would always under-tap and the server would reject the cast. The
   * targeting phase therefore has to come first, exactly as an X cost puts `xSelection` first.
   */
  function interrogation(over: Record<string, unknown> = {}): LegalActionInfo {
    return castAction({
      actionType: 'CastSpell',
      manaCostString: '{W}{U}',
      requiresTargets: true,
      validTargets: ['p1', 'p2'],
      availableManaSources: [{ entityId: 'plains1' }, { entityId: 'island1' }],
      ...over,
    })
  }

  it('defers the manaSource phase past targeting when the cost scales with targets', () => {
    const phases = computePhases(interrogation({ manaCostPerExtraTarget: '{W}{U}' }), {
      autoTapEnabled: false,
    })
    expect(phases).toEqual([{ type: 'targeting' }, { type: 'manaSource' }])
  })

  it('keeps manaSource before targeting for an ordinary spell', () => {
    const phases = computePhases(interrogation(), { autoTapEnabled: false })
    expect(phases).toEqual([{ type: 'manaSource' }, { type: 'targeting' }])
  })

  it('runs no manaSource phase at all under auto-tap — the server prices the targets', () => {
    const phases = computePhases(interrogation({ manaCostPerExtraTarget: '{W}{U}' }), {
      autoTapEnabled: true,
    })
    expect(phases).toEqual([{ type: 'targeting' }])
  })
})
