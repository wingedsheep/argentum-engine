import { describe, expect, it } from 'vitest'
import type { ClientCard, LegalActionInfo } from '@/types'
import { buildActionOptions, costFieldsFor, playCostRange, playLadderOptions } from './actionOptions'

/**
 * A card in hand, as far as `buildActionOptions` cares: a name, a printed cost, and its types.
 * The rest of `ClientCard` is board state the option list never reads.
 */
const card = (manaCost: string, extra: Partial<ClientCard> = {}): ClientCard =>
  ({ name: 'Test Card', manaCost, cardTypes: [], ...extra }) as unknown as ClientCard

/** A server legal action, with only the fields the option list reads. */
const action = (fields: Record<string, unknown>): LegalActionInfo =>
  ({
    action: { type: 'CastSpell' },
    actionType: 'CastSpell',
    description: 'Cast Test Card',
    ...fields,
  }) as unknown as LegalActionInfo

describe('costFieldsFor', () => {
  it('turns a convoke floor into a reduced-to cost and names what it costs to get there', () => {
    // Sun-Dappled Celebrant {4}{W}{W} with two white bodies: the server sends both ends.
    const fields = costFieldsFor(
      action({ manaCostString: '{4}{W}{W}', minimumManaCostString: '{4}', hasConvoke: true }),
      '{4}{W}{W}',
    )
    expect(fields.manaCost).toBe('{4}{W}{W}')
    expect(fields.manaCostReducedTo).toBe('{4}')
    expect(fields.hint).toContain('convoke')
  })

  it('names delve, harmonize, waterbend and improvise rather than saying "reduced" generically', () => {
    const floorFields = (extra: Record<string, unknown>) =>
      costFieldsFor(action({ manaCostString: '{5}{U}', minimumManaCostString: '{2}{U}', ...extra }), null)
    expect(floorFields({ hasDelve: true }).hint).toContain('delve')
    expect(floorFields({ hasHarmonize: true }).hint).toContain('harmonize')
    expect(floorFields({ hasTapForGeneric: true, tapForGenericLabel: 'waterbend' }).hint).toContain('waterbend')
    // The two tap-for-generic keywords share one carrier, so the label is what distinguishes them.
    expect(floorFields({ hasTapForGeneric: true, tapForGenericLabel: 'improvise' }).hint).toContain('improvise')
  })

  it('emerge prices its candidates, and the best of them wins over any generic floor', () => {
    // Emerge (CR 702.119a): the reduction depends on WHICH creature is sacrificed, so the server
    // sends one cost per candidate instead of a single floor.
    const fields = costFieldsFor(
      action({
        action: { type: 'CastSpell', alternativeCostType: 'EMERGE' },
        manaCostString: '{5}{U}',
        additionalCostInfo: { costAfterSacrifice: { a: '{3}{U}', b: '{2}{U}' } },
      }),
      null,
    )
    expect(fields.manaCostReducedTo).toBe('{2}{U}')
    expect(fields.hint).toContain('sacrifice')
  })

  it('a floor equal to the cost is not a reduction', () => {
    const fields = costFieldsFor(
      action({ manaCostString: '{2}{G}', minimumManaCostString: '{2}{G}', hasConvoke: true }),
      null,
    )
    expect(fields.manaCostReducedTo).toBeUndefined()
  })

  it('an explicit free cast shows {0} instead of falling back to the printed cost', () => {
    // A Weftwalking-style permission sends manaCostString: "" — a real price, not a missing one.
    expect(costFieldsFor(action({ manaCostString: '' }), '{6}{R}').manaCost).toBe('{0}')
    // A genuinely absent cost still falls back.
    expect(costFieldsFor(action({}), '{6}{R}').manaCost).toBe('{6}{R}')
  })
})

describe('playCostRange', () => {
  it('a plain spell with one price is not a range', () => {
    const range = playCostRange(buildActionOptions(card('{2}{G}'), [action({ manaCostString: '{2}{G}' })]))
    expect(range).toMatchObject({ low: '{2}{G}', high: '{2}{G}', isRange: false, optionCount: 1 })
  })

  it('spans both faces of an adventure card', () => {
    // Bumbleflower's Sharepot — creature face {1}{G}, adventure face {G}. The old badge showed
    // whichever CastSpell the enumerator happened to emit first.
    const range = playCostRange(buildActionOptions(card('{1}{G}'), [
      action({ manaCostString: '{1}{G}', description: 'Cast Bumbleflower\'s Sharepot' }),
      action({ manaCostString: '{G}', description: 'Cast Sharepot (Adventure)' }),
    ]))
    expect(range).toMatchObject({ low: '{G}', high: '{1}{G}', isRange: true, optionCount: 2 })
  })

  it('two faces at the same mana value are still two prices', () => {
    // Questing Druid // Seek the Beast — {1}{G} as a creature, {1}{R} as its adventure. Comparing
    // mana values alone would call these one price and silently drop a face.
    const range = playCostRange(buildActionOptions(card('{1}{G}'), [
      action({ manaCostString: '{1}{G}', description: 'Cast Questing Druid' }),
      action({ manaCostString: '{1}{R}', description: 'Cast Seek the Beast (Adventure)' }),
    ]))
    expect(range).toMatchObject({ low: '{1}{G}', high: '{1}{R}', isRange: true, optionCount: 2 })
  })

  it('a kicker widens the top of the range, not the bottom', () => {
    const range = playCostRange(buildActionOptions(card('{1}{G}'), [
      action({ manaCostString: '{1}{G}' }),
      action({ actionType: 'CastWithKicker', manaCostString: '{4}{G}', description: 'Cast (Kicked)' }),
    ]))
    expect(range).toMatchObject({ low: '{1}{G}', high: '{4}{G}', isRange: true })
  })

  it('every kicker variant the server offers becomes its own option ("Kicker {G} and/or {1}{U}")', () => {
    const options = buildActionOptions(card('{1}{C}'), [
      action({ manaCostString: '{1}{C}' }),
      action({ actionType: 'CastWithKicker', manaCostString: '{1}{C}{G}', description: 'Cast (Kicked {G})' }),
      action({ actionType: 'CastWithKicker', manaCostString: '{2}{C}{U}', description: 'Cast (Kicked {1}{U})' }),
      action({ actionType: 'CastWithKicker', manaCostString: '{2}{C}{G}{U}', description: 'Cast (Kicked {G} + {1}{U})' }),
    ])
    const kicked = options.filter((o) => o.actionType === 'castWithKicker')
    expect(kicked.map((o) => o.label)).toEqual(['Cast (Kicked {G})', 'Cast (Kicked {1}{U})', 'Cast (Kicked {G} + {1}{U})'])
    expect(new Set(kicked.map((o) => o.key)).size).toBe(3)
    expect(playCostRange(options)).toMatchObject({ low: '{1}{C}', high: '{2}{C}{G}{U}', isRange: true })
  })

  it('a convoke floor opens a range on a card with only one cast option', () => {
    const range = playCostRange(buildActionOptions(card('{4}{W}{W}'), [
      action({ manaCostString: '{4}{W}{W}', minimumManaCostString: '{4}', hasConvoke: true }),
    ]))
    // The top end is what the cast asks for before the player taps anything.
    expect(range).toMatchObject({ low: '{4}', high: '{4}{W}{W}', isRange: true })
  })

  it('cycling does not widen the range — it is what you do instead of playing the card', () => {
    // A {5}{W} creature with cycling {1} must not read as a "{1} to {5}{W}" spell.
    const range = playCostRange(buildActionOptions(card('{5}{W}'), [
      action({ manaCostString: '{5}{W}' }),
      action({ action: { type: 'CycleCard' }, actionType: 'CycleCard', manaCostString: '{1}' }),
    ]))
    expect(range).toMatchObject({ low: '{5}{W}', high: '{5}{W}', isRange: false, optionCount: 1 })
  })

  it('morph counts as a way to play the card', () => {
    const range = playCostRange(buildActionOptions(card('{5}{G}'), [
      action({ manaCostString: '{5}{G}' }),
      action({ actionType: 'CastFaceDown', manaCostString: '{3}' }),
    ]))
    expect(range).toMatchObject({ low: '{3}', high: '{5}{G}', isRange: true })
  })

  it('reports whether any way to play it is affordable', () => {
    const options = buildActionOptions(card('{1}{G}'), [
      action({ manaCostString: '{1}{G}', isAffordable: false }),
      action({ actionType: 'CastWithKicker', manaCostString: '{4}{G}', isAffordable: false, description: 'Cast (Kicked)' }),
    ])
    expect(playCostRange(options)?.anyAffordable).toBe(false)
    expect(playCostRange(buildActionOptions(card('{1}{G}'), [action({ manaCostString: '{1}{G}' })]))?.anyAffordable).toBe(true)
  })

  it('a land drop has no price, so there is no range to show', () => {
    const range = playCostRange(buildActionOptions(
      card('', { cardTypes: ['LAND'] } as Partial<ClientCard>),
      [action({ action: { type: 'PlayLand' }, actionType: 'PlayLand' })],
    ))
    expect(range).toBeNull()
  })

  it('no actions at all means no range', () => {
    expect(playCostRange([])).toBeNull()
  })
})

describe('buildActionOptions — modal double-faced lands', () => {
  const pathwayCard = card('', { name: 'Riverglide Pathway', cardTypes: ['LAND'] } as Partial<ClientCard>)

  it('lists one entry per land face, labelled by the face rather than by the card', () => {
    // CR 712.12: the server sends one PlayLand per land face, each described by the face it plays.
    // `cardInfo.name` is the front face's name for both, so the descriptions are the only thing
    // telling them apart — a `find` here would silently drop the back face.
    const options = buildActionOptions(pathwayCard, [
      action({
        action: { type: 'PlayLand' },
        actionType: 'PlayLand',
        description: 'Play Riverglide Pathway',
      }),
      action({
        action: { type: 'PlayLand', asBackFace: true },
        actionType: 'PlayLand',
        description: 'Play Lavaglide Pathway',
      }),
    ])
    expect(options.map((o) => o.label)).toEqual([
      'Play Riverglide Pathway',
      'Play Lavaglide Pathway',
    ])
    expect(options.map((o) => o.key)).toEqual(['playLand', 'playLand-1'])
    expect(options.every((o) => o.actionType === 'playLand')).toBe(true)
  })

  it('an ordinary land still reads "Play <card name>"', () => {
    const options = buildActionOptions(
      card('', { name: 'Island', cardTypes: ['LAND'] } as Partial<ClientCard>),
      [action({ action: { type: 'PlayLand' }, actionType: 'PlayLand', description: 'Play Island' })],
    )
    expect(options.map((o) => o.label)).toEqual(['Play Island'])
  })
})

describe('buildActionOptions — casting a single face', () => {
  it('a prepare-spell copy is labelled and priced by the face it casts, not by the creature', () => {
    // The exiled copy of Bloodline Recollector casts its prepare spell (faceIndex 0). The server
    // names and prices that face; the card itself still reads as the {1}{B} creature.
    const options = buildActionOptions(
      card('{1}{B}', { name: 'Bloodline Recollector' } as Partial<ClientCard>),
      [action({
        action: { type: 'CastSpell', faceIndex: 0 },
        description: 'Cast Ancestral Craving',
        manaCostString: '{B}',
      })],
    )
    expect(options.map((o) => [o.label, o.manaCost])).toEqual([['Cast Ancestral Craving', '{B}']])
  })

  it('an ordinary cast still reads "Cast <card name>"', () => {
    const options = buildActionOptions(
      card('{1}{B}', { name: 'Leech Collector' } as Partial<ClientCard>),
      [action({ action: { type: 'CastSpell', faceIndex: null }, description: 'Cast Leech Collector' })],
    )
    expect(options.map((o) => o.label)).toEqual(['Cast Leech Collector'])
  })
})

describe('playLadderOptions', () => {
  it('lists cycling alongside the cast, even though cycling stays out of the range', () => {
    const options = buildActionOptions(card('{5}{W}'), [
      action({ manaCostString: '{5}{W}' }),
      action({ action: { type: 'CycleCard' }, actionType: 'CycleCard', manaCostString: '{1}' }),
    ])
    expect(playLadderOptions(options).map((o) => o.actionType)).toEqual(['cast', 'cycle'])
  })

  it('leaves a battlefield permanent\'s activated abilities off the "ways to play" list', () => {
    // Hovering a creature on the battlefield: its {2} tap ability is not a way to play the card, and
    // would read as a price for casting it.
    const options = buildActionOptions(card('{1}{G}'), [
      action({ action: { type: 'ActivateAbility' }, actionType: 'ActivateAbility', manaCostString: '{2}', description: 'Draw a card' }),
    ])
    expect(playLadderOptions(options)).toEqual([])
  })

  it('drops costless rows so a land drop never reads as a price', () => {
    const options = buildActionOptions(
      card('', { cardTypes: ['LAND'] } as Partial<ClientCard>),
      [action({ action: { type: 'PlayLand' }, actionType: 'PlayLand' })],
    )
    expect(playLadderOptions(options)).toEqual([])
  })
})

describe('buildActionOptions — keyword alternative costs (evoke, impending)', () => {
  const mulldrifter = card('{4}{U}', { name: 'Mulldrifter', evoke: '{2}{U}' } as Partial<ClientCard>)

  it('offers the evoke price next to the printed one when only evoke is affordable', () => {
    // The server enumerates only the casts it can pay for, so an unaffordable hard cast arrives as
    // nothing at all. Before this, that left one option and the drag-to-play path fired it — you
    // evoked a Mulldrifter you meant to hard-cast, and sacrificed it, without being asked.
    const options = buildActionOptions(mulldrifter, [
      action({
        action: { type: 'CastSpell', alternativeCostType: 'EVOKE' },
        actionType: 'CastWithAlternativeCost',
        description: 'Evoke Mulldrifter ({2}{U})',
        manaCostString: '{2}{U}',
      }),
    ])
    expect(options.map((o) => o.key)).toEqual(['cast', 'evoke'])
    expect(options[0]).toMatchObject({ label: 'Cast Mulldrifter', manaCost: '{4}{U}', isAvailable: false, action: null })
    expect(options[1]).toMatchObject({ label: 'Evoke Mulldrifter', manaCost: '{2}{U}', isAvailable: true })
    expect(options[1]!.hint).toContain('sacrificed')
  })

  it('offers the evoke price grayed out when only the hard cast is affordable', () => {
    // The other direction: seven lands and no evoke action enumerated (it is affordable too, but a
    // server that omits it must not cost the player the choice).
    const options = buildActionOptions(mulldrifter, [
      action({ manaCostString: '{4}{U}' }),
    ])
    expect(options.map((o) => o.key)).toEqual(['cast', 'evoke'])
    expect(options[0]).toMatchObject({ isAvailable: true })
    expect(options[1]).toMatchObject({ label: 'Evoke Mulldrifter', manaCost: '{2}{U}', isAvailable: false, action: null })
  })

  it('wires both cast actions to their own option when the player can afford either', () => {
    const options = buildActionOptions(mulldrifter, [
      action({ manaCostString: '{4}{U}' }),
      action({
        action: { type: 'CastSpell', alternativeCostType: 'EVOKE' },
        actionType: 'CastWithAlternativeCost',
        description: 'Evoke Mulldrifter ({2}{U})',
        manaCostString: '{2}{U}',
      }),
    ])
    expect(options.map((o) => o.key)).toEqual(['cast', 'evoke'])
    expect(options.every((o) => o.isAvailable && o.action !== null)).toBe(true)
  })

  it('still pairs impending with the printed cost, keeping its time-counter glyph', () => {
    const overlord = card('{5}{W}{W}', {
      name: 'Overlord of the Mistmoors',
      impending: { cost: '{2}{W}{W}', time: 4 },
    } as Partial<ClientCard>)
    const options = buildActionOptions(overlord, [
      action({
        action: { type: 'CastSpell', alternativeCostType: 'IMPENDING' },
        actionType: 'CastWithAlternativeCost',
        description: 'Impending Overlord of the Mistmoors ({2}{W}{W})',
        manaCostString: '{2}{W}{W}',
      }),
    ])
    expect(options.map((o) => o.key)).toEqual(['cast', 'impending'])
    expect(options[1]).toMatchObject({ label: 'Cast for Impending', manaCost: '{2}{W}{W}', impendingTime: 4 })
  })

  it('keeps any further cost variant the server offered alongside the pair', () => {
    const options = buildActionOptions(mulldrifter, [
      action({ manaCostString: '{4}{U}' }),
      action({
        action: { type: 'CastSpell', alternativeCostType: 'EVOKE' },
        actionType: 'CastWithAlternativeCost',
        description: 'Evoke Mulldrifter ({2}{U})',
        manaCostString: '{2}{U}',
      }),
      action({
        action: { type: 'CastSpell', alternativeCostType: 'GRANTED' },
        actionType: 'CastWithAlternativeCost',
        description: 'Cast Mulldrifter (Omniscience)',
        manaCostString: '{0}',
      }),
    ])
    expect(options.map((o) => o.key)).toEqual(['cast', 'evoke', 'cast-extra-0'])
  })
})


describe('buildActionOptions — bestow', () => {
  const creature = card('{1}{G}', { name: 'Test Bestow', bestow: { cost: '{3}{G}' } })
  const normal = action({ manaCostString: '{1}{G}' })
  const bestow = action({
    action: { type: 'CastSpell', alternativeCostType: 'BESTOW', useAlternativeCost: true },
    actionType: 'CastWithAlternativeCost',
    manaCostString: '{3}{G}',
    validTargets: [{ targetId: 'creature' }],
  })

  it.each([
    { actions: [normal], enabled: [true, false] },
    { actions: [bestow], enabled: [false, true] },
    { actions: [normal, bestow], enabled: [true, true] },
  ])('shows both prices and follows server availability: $enabled', ({ actions, enabled }) => {
    const options = buildActionOptions(creature, actions)
    expect(options.map(o => o.key)).toEqual(['cast', 'bestow'])
    expect(options.map(o => o.isAvailable)).toEqual(enabled)
    expect(options[1]).toMatchObject({ label: 'Bestow Test Bestow', manaCost: '{3}{G}' })
    expect(options[1]!.hint).toContain('Aura')
    if (enabled[1]) expect(options[1]!.action).toBe(bestow)
    else expect(options[1]!.action).toBeNull()
  })

  it('shows the nonmana payment even when bestow is unavailable', () => {
    const options = buildActionOptions(card('{1}{G}', {
      bestow: { cost: '{G}', additionalCostDescription: 'Pay 2 life' },
    }), [normal])
    expect(options[1]).toMatchObject({ manaCost: '{G}', isAvailable: false, action: null })
    expect(options[1]!.hint).toContain('Pay 2 life')
  })
})
