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
