import type { LegalActionInfo, ClientCard } from '@/types'
import { TAP_FOR_GENERIC_LABEL_IMPROVISE } from '@/types'
import { cheapestCost, parseManaCost, totalManaNeeded } from './manaCost.ts'

/**
 * One way to use a card, as offered by the server's legal actions: a cast, an alternative cast, a
 * land drop, a cycle, an activated ability.
 *
 * Built here rather than inside a component because three surfaces need the *same* list, and used to
 * each keep their own partial copy of it: the action menu the player clicks, the cost badge on the
 * card in hand, and the hover preview's cost ladder. The badge and the preview previously picked a
 * single "normal" cast out of `legalActions` with a hand-maintained list of `actionType` strings to
 * exclude, so a card whose only cast was an adventure face, a kicker, or an alternative cost showed
 * its printed cost with no sign that the printed cost wasn't the price.
 */
export interface ActionOption {
  /** Unique key for React */
  key: string
  /** Display label */
  label: string
  /** Mana cost to display */
  manaCost: string | null
  /**
   * The cheapest cost this option can actually end up costing, when the cast's own choices reduce
   * [manaCost]. Two shapes reach here:
   *
   * - Emerge (CR 702.119a) — the sacrificed creature's mana value comes off the emerge cost, so the
   *   server prices every candidate and the floor is the best of them.
   * - Convoke, delve, waterbend and harmonize — the server sends the pre-reduction cost plus a
   *   `minimumManaCostString` floor for spending every tap/exile it offers.
   *
   * Rendered as "{5}{U} → as low as {2}{U}" so the number on a button can't contradict the fact that
   * the button is enabled.
   */
  manaCostReducedTo?: string
  /** One short line under the label explaining why the cost moves. */
  hint?: string
  /** Whether this action is available (affordable) */
  isAvailable: boolean
  /** The legal action info if available */
  action: LegalActionInfo | null
  /** Action type for coloring */
  actionType: 'cast' | 'castFaceDown' | 'castWithKicker' | 'cycle' | 'plot' | 'suspend' | 'playLand' | 'activate' | 'turnFaceUp'
  /**
   * Signed loyalty change for planeswalker loyalty abilities (+1, -2, -8, 0).
   * When present, the button renders a mana-font loyalty icon instead of a text prefix.
   */
  loyaltyChange?: number
  loyaltyX?: boolean
  /**
   * For the impending cast option (CR 702.176): the number of time counters the permanent enters
   * with. When present, the button renders a time-counter glyph + count to mark the option as
   * impending. Undefined for every other option.
   */
  impendingTime?: number
}

/**
 * The `actionType`s that actually put the card onto the stack or the battlefield.
 *
 * Cycling, plotting and suspending are things you do *instead of* playing the card, so their costs
 * must not fold into the price shown on the card — a {1}{G} creature with cycling {W} is not a
 * "{W} to {1}{G}" spell. They still get their own row in the hover ladder.
 */
const PLAY_ACTION_TYPES: ReadonlySet<ActionOption['actionType']> = new Set([
  'cast', 'castFaceDown', 'castWithKicker',
])

/**
 * Everything a hover preview's cost ladder should list: the ways to play the card, plus the
 * alternatives you'd use *instead of* playing it.
 *
 * Wider than [PLAY_ACTION_TYPES] because cycling for {W} genuinely is one of the things this card can
 * do for you and belongs on the list — it just must not move the price shown on the card. Activated
 * abilities, crew and turn-face-up are excluded: those belong to a permanent already on the
 * battlefield, not to a decision about playing the card.
 */
const LADDER_ACTION_TYPES: ReadonlySet<ActionOption['actionType']> = new Set([
  'cast', 'castFaceDown', 'castWithKicker', 'cycle', 'plot', 'suspend', 'playLand',
])

/**
 * The options a cost ladder lists, in the order the action menu shows them: ways to play the card and
 * the alternatives to playing it, each with a price attached. Costless rows (a land drop, a
 * planeswalker's loyalty abilities) are dropped — "Play Forest — (no cost)" is noise.
 */
export function playLadderOptions(options: readonly ActionOption[]): ActionOption[] {
  return options.filter((o) => LADDER_ACTION_TYPES.has(o.actionType) && o.manaCost !== null)
}

/** The span of prices for playing a card, across every way the server offers to play it. */
export interface PlayCostRange {
  /** Cheapest reachable cost, spending every reduction on the cheapest option. */
  readonly low: string
  /** Dearest price among the options; equal to [low] when they all cost the same. */
  readonly high: string
  /** True when [low] and [high] are different costs — i.e. there is more than one price to show. */
  readonly isRange: boolean
  /** How many distinct ways to play the card were offered (cast variants, faces, kickers). */
  readonly optionCount: number
  /** Whether at least one of those ways is affordable right now. */
  readonly anyAffordable: boolean
}

/**
 * The span of mana it could take to play this card right now, or null when no option plays it.
 *
 * A single mana value is a lie for most of the interesting cards: an adventure or split card has one
 * cost per face, kicker and offspring have a paid and an unpaid price, and convoke/delve/emerge each
 * carry a floor well under the cost the server advertises. This reduces the whole option list to two
 * ends, which is what a card-sized badge has room to say honestly; the hover ladder lists the rest.
 *
 * Both a *reachable floor* and a plain *alternative price* count as an end, which is why the two ends
 * are picked out of the distinct prices rather than "cheapest floor, dearest ceiling". Questing Druid
 * is the case that forces it: {1}{G} as a creature or {1}{R} as its adventure is two real prices at
 * the same mana value, and comparing mana values alone would show one and silently drop the other.
 * Where more than two distinct prices exist, the outer two win — the span stays true and the ladder
 * carries the detail.
 */
export function playCostRange(options: readonly ActionOption[]): PlayCostRange | null {
  const playOptions = options.filter(
    (o) => PLAY_ACTION_TYPES.has(o.actionType) && o.manaCost !== null
  )
  if (playOptions.length === 0) return null

  // Sorted by mana, then by the cost string so ties break the same way every time. Two ends of equal
  // mana value are common (Questing Druid's {1}{G} and {1}{R}) and picking "whichever the enumerator
  // emitted first" would let the badge swap its two halves between servers or releases.
  const prices = [...new Set(playOptions.flatMap((o) => [o.manaCost!, o.manaCostReducedTo ?? o.manaCost!]))]
    .sort((a, b) => totalManaNeeded(parseManaCost(a)) - totalManaNeeded(parseManaCost(b)) || a.localeCompare(b))
  const low = prices[0]!
  const high = prices[prices.length - 1]!

  return {
    low,
    high,
    isRange: low !== high,
    optionCount: playOptions.length,
    anyAffordable: playOptions.some((o) => o.isAvailable),
  }
}

/**
 * The cost fields for a cast option, accounting for a cost the cast's own choices will reduce.
 *
 * Two independent sources of a floor, because they answer different questions. Emerge
 * (CR 702.119a) depends on *which* creature is sacrificed, so the server prices each candidate
 * (`additionalCostInfo.costAfterSacrifice`) and the floor is the best of them. Convoke, delve,
 * waterbend and harmonize depend on *how much* the player spends, so the server sends a single
 * `minimumManaCostString` for spending everything. Both exist server-side because each reduction is
 * a rule — emerge's is generic-only, convoke matches colors, harmonize taps one creature — and rules
 * don't live in the client.
 *
 * Showing only the un-reduced cost is what made a four-mana Wretched Gryff read as a bug: the button
 * said `{5}{U}` and was enabled on four lands.
 */
export function costFieldsFor(
  action: LegalActionInfo,
  fallbackCost: string | null,
): Pick<ActionOption, 'manaCost' | 'manaCostReducedTo' | 'hint'> {
  // An explicit `{0}` arrives as the empty symbol string. That is a real price — a free cast off a
  // Weftwalking-style permission — so it must not fall through to the printed cost the way a missing
  // cost does.
  const manaCost = action.manaCostString === '' ? '{0}' : (action.manaCostString || fallbackCost || null)

  const costAfterSacrifice = action.additionalCostInfo?.costAfterSacrifice
  const isEmerge = (action.action as { alternativeCostType?: string }).alternativeCostType === 'EMERGE'
  if (isEmerge && costAfterSacrifice) {
    const cheapest = cheapestCost(Object.values(costAfterSacrifice))
    return {
      manaCost,
      // A single candidate leaves one price, not a range — show it as the cost outright.
      ...(cheapest && cheapest !== manaCost ? { manaCostReducedTo: cheapest } : {}),
      hint: 'sacrifice a creature — its mana value comes off',
    }
  }

  const floor = action.minimumManaCostString
  if (floor && floor !== manaCost) {
    return { manaCost, manaCostReducedTo: floor, ...(reductionHintFor(action) ?? {}) }
  }
  return { manaCost }
}

/**
 * What the player has to spend to reach an option's floor. Named per keyword rather than generically,
 * because "as low as {G}" is only actionable once you know it costs you your whole board's taps.
 */
function reductionHintFor(action: LegalActionInfo): { hint: string } | null {
  if (action.hasConvoke) return { hint: 'convoke — tap creatures to help pay' }
  if (action.hasDelve) return { hint: 'delve — exile cards from your graveyard' }
  if (action.hasHarmonize) return { hint: 'harmonize — tap one creature for its power' }
  if (action.hasTapForGeneric) {
    return action.tapForGenericLabel === TAP_FOR_GENERIC_LABEL_IMPROVISE
      ? { hint: 'improvise — tap artifacts' }
      : { hint: 'waterbend — tap artifacts or creatures' }
  }
  return null
}

/**
 * A keyword alternative cost the *player* chooses when casting — impending (CR 702.176) and evoke
 * (CR 702.74). Both read "you may cast this card by paying [cost] rather than paying its mana
 * cost", which makes the printed cost and the keyword cost two live prices for the same card, not
 * a price and a fallback.
 *
 * That is why the cost lives on [ClientCard] and not just in `legalActions`: the server enumerates
 * only the casts it can *afford*, so the side the player can't pay for arrives as nothing at all.
 * Building the button off the card lets the menu show both prices and gray out the unpayable one —
 * and, just as importantly, stops the drag-to-play path from silently firing the lone affordable
 * cast (see `shouldShowCastModal`). Evoking a Mulldrifter you meant to hard-cast is not a choice
 * the player should make by accident.
 */
export interface KeywordAlternativeCost {
  /** Option key, also the discriminator the tests and the menu identify the row by. */
  readonly key: 'impending' | 'evoke' | 'bestow'
  /** Button label. */
  readonly label: string
  /** The alternative mana cost, e.g. "{2}{U}". */
  readonly cost: string
  /** `CastSpell.alternativeCostType` naming this cast on the server side. */
  readonly alternativeCostType: string
  /** One short line under the label saying what paying this cost does to you. */
  readonly hint?: string
  /** Impending only: time counters the permanent enters with, rendered as a glyph. */
  readonly impendingTime?: number
}

/**
 * The keyword alternative cost this card offers, or null when it has none. At most one: no printed
 * card carries two of these keywords, and a second one would need its own row rather than a
 * different shape of this one.
 */
export function keywordAlternativeCostFor(cardInfo: ClientCard): KeywordAlternativeCost | null {
  if (cardInfo.bestow) {
    return {
      key: 'bestow',
      label: `Bestow ${cardInfo.name}`,
      cost: cardInfo.bestow.cost || '{0}',
      alternativeCostType: 'BESTOW',
      hint: ['cast as an Aura enchanting a creature', cardInfo.bestow.additionalCostDescription].filter(Boolean).join(' — '),
    }
  }
  if (cardInfo.impending) {
    return {
      key: 'impending',
      label: 'Cast for Impending',
      cost: cardInfo.impending.cost,
      alternativeCostType: 'IMPENDING',
      impendingTime: cardInfo.impending.time,
    }
  }
  if (cardInfo.evoke) {
    return {
      key: 'evoke',
      label: `Evoke ${cardInfo.name}`,
      cost: cardInfo.evoke,
      alternativeCostType: 'EVOKE',
      // The sacrifice is the whole point of the cheaper price, and it is the half a player who
      // dragged the card out to hard-cast it needs to see before clicking.
      hint: 'evoke — sacrificed as it enters, keeping only its ETB',
    }
  }
  return null
}

/**
 * Build all potential action options for a card from server-sent legal actions.
 * The server sends ALL potential actions with isAffordable flags.
 */
export function buildActionOptions(
  cardInfo: ClientCard | null,
  legalActions: readonly LegalActionInfo[]
): ActionOption[] {
  const options: ActionOption[] = []
  if (!cardInfo) return options

  const keywordAltCost = keywordAlternativeCostFor(cardInfo)

  // Find each type of action - server sends all potential actions with isAffordable flag
  const castActions = legalActions.filter(
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown' && a.actionType !== 'CastWithKicker'
  )
  const castAction = castActions[0] ?? null
  const kickerAction = legalActions.find((a) => a.actionType === 'CastWithKicker')
  const morphAction = legalActions.find((a) => a.actionType === 'CastFaceDown')
  const cycleAction = legalActions.find((a) => a.action.type === 'CycleCard')
  const typecycleAction = legalActions.find((a) => a.action.type === 'TypecycleCard')
  const plotAction = legalActions.find((a) => a.action.type === 'PlotCard')
  const suspendAction = legalActions.find((a) => a.action.type === 'SuspendCardFromHand')
  // A modal double-faced land (the Zendikar Rising Pathway cycle) sends *two* PlayLand actions
  // for one card — one per land face, CR 712.12 — so this is a filter, not a find.
  const playLandActions = legalActions.filter((a) => a.action.type === 'PlayLand')

  // 1. Modal spell modes — show one button per mode instead of a single "Cast" button
  const modeActions = legalActions.filter((a) => a.actionType === 'CastSpellMode')
  // Sibling CastSpellModal actions (e.g., Pyrrhic Strike's blight path forces every mode)
  // need to render alongside the per-mode buttons so the player can pick the
  // alternative cost variant.
  const modalCastActions = legalActions.filter((a) => a.actionType === 'CastSpellModal')
  if (modeActions.length > 0) {
    modeActions.forEach((modeAction, index) => {
      options.push({
        key: `mode-${index}`,
        label: modeAction.description,
        ...costFieldsFor(modeAction, cardInfo.manaCost),
        isAvailable: modeAction.isAffordable !== false,
        action: modeAction,
        actionType: 'cast',
      })
    })
    modalCastActions.forEach((modalAction, index) => {
      options.push({
        key: `modal-${index}`,
        label: modalAction.description,
        ...costFieldsFor(modalAction, cardInfo.manaCost),
        isAvailable: modalAction.isAffordable !== false,
        action: modalAction,
        actionType: 'cast',
      })
    })
  } else if (keywordAltCost && castActions.length > 0) {
    // 1a-bis. A keyword alternative cost the player chooses — impending (CR 702.176), evoke
    // (CR 702.74). Both mean "you may cast this for <cost> rather than its mana cost", so the menu
    // presents BOTH the normal cast and the alternative every time, graying out whichever can't be
    // paid. The server only emits the cast actions it can afford, so whichever side it left out is
    // synthesized here as a disabled placeholder (action: null) — the alternative's cost comes off
    // the card rather than off a legal action for exactly that reason.
    const altCast = castActions.find(
      (a) => (a.action as { alternativeCostType?: string }).alternativeCostType === keywordAltCost.alternativeCostType
    ) ?? null
    const normalCast = castActions.find((a) => a.actionType === 'CastSpell') ?? null

    options.push(
      normalCast
        ? {
            key: 'cast',
            label: `Cast ${cardInfo.name}`,
            ...costFieldsFor(normalCast, cardInfo.manaCost),
            isAvailable: normalCast.isAffordable !== false,
            action: normalCast,
            actionType: 'cast',
          }
        : {
            key: 'cast',
            label: `Cast ${cardInfo.name}`,
            manaCost: cardInfo.manaCost || null,
            isAvailable: false,
            action: null,
            actionType: 'cast',
          }
    )
    options.push(
      altCast
        ? {
            key: keywordAltCost.key,
            label: keywordAltCost.label,
            ...costFieldsFor(altCast, keywordAltCost.cost),
            ...(keywordAltCost.hint ? { hint: keywordAltCost.hint } : {}),
            isAvailable: altCast.isAffordable !== false,
            action: altCast,
            actionType: 'cast',
            ...(keywordAltCost.impendingTime !== undefined ? { impendingTime: keywordAltCost.impendingTime } : {}),
          }
        : {
            key: keywordAltCost.key,
            label: keywordAltCost.label,
            manaCost: keywordAltCost.cost || null,
            ...(keywordAltCost.hint ? { hint: keywordAltCost.hint } : {}),
            isAvailable: false,
            action: null,
            actionType: 'cast',
            ...(keywordAltCost.impendingTime !== undefined ? { impendingTime: keywordAltCost.impendingTime } : {}),
          }
    )
    // Any further cost variant the server offered (e.g. a gift promise per opponent — CR 702.174a)
    // has to survive this branch too, or picking impending's sibling silently drops the option.
    castActions
      .filter((ca) => ca !== normalCast && ca !== altCast)
      .forEach((ca, index) => {
        options.push({
          key: `cast-extra-${index}`,
          label: ca.description,
          ...costFieldsFor(ca, cardInfo.manaCost),
          isAvailable: ca.isAffordable !== false,
          action: ca,
          actionType: 'cast',
        })
      })
  } else if (castActions.length > 1) {
    // 1b. Multiple cast options (e.g., BlightOrPay — blight path vs pay path, or a hard cast
    // alongside an alternative cost like emerge)
    castActions.forEach((ca, index) => {
      options.push({
        key: `cast-${index}`,
        label: ca.description,
        ...costFieldsFor(ca, cardInfo.manaCost),
        isAvailable: ca.isAffordable !== false,
        action: ca,
        actionType: 'cast',
      })
    })
  } else if (castAction) {
    // 1c. Normal cast (for non-land, non-modal cards). When the *only* offered cast is an
    // alternative-cost one (emerge with the hard cast unaffordable), the server's description names
    // the mechanic — "Cast X" would hide that the cast eats a creature. Likewise a cast of one
    // specific face (a prepare-spell copy in exile, an adventure-only permission) is named by the
    // server after that face: "Cast Bloodline Recollector" would hide that it casts Ancestral Craving.
    const castFaceIndex = (castAction.action as { faceIndex?: number | null }).faceIndex
    options.push({
      key: 'cast',
      label: castAction.actionType === 'CastWithAlternativeCost' || castFaceIndex != null
        ? castAction.description
        : `Cast ${cardInfo.name}`,
      ...costFieldsFor(castAction, cardInfo.manaCost),
      isAvailable: castAction.isAffordable !== false, // default true if not set
      action: castAction,
      actionType: 'cast',
    })
  } else if ((cycleAction || typecycleAction || plotAction || suspendAction) && !cardInfo.cardTypes.includes('LAND')) {
    // Non-land card with cycling/plot but no CastSpell action — show grayed-out cast option
    // so the action menu always presents both choices
    options.push({
      key: 'cast',
      label: `Cast ${cardInfo.name}`,
      manaCost: cardInfo.manaCost || null,
      isAvailable: false,
      action: null,
      actionType: 'cast',
    })
  }

  // 2. Play land (for land cards)
  if (playLandActions.length > 0) {
    playLandActions.forEach((playLandAction, index) => {
      options.push({
        // One entry per land face. The server's description already names the face being
        // played ("Play Lavaglide Pathway"), which is the only thing telling the two apart —
        // `cardInfo.name` is the front face's name for both.
        key: index === 0 ? 'playLand' : `playLand-${index}`,
        label: playLandActions.length > 1 ? playLandAction.description : `Play ${cardInfo.name}`,
        manaCost: null,
        isAvailable: playLandAction.isAffordable !== false,
        action: playLandAction,
        actionType: 'playLand',
      })
    })
  } else if (cycleAction && cardInfo.cardTypes.includes('LAND')) {
    // Land with cycling but no PlayLand action (already played a land this turn)
    // Show grayed-out "Play land" so the action menu always has both options
    options.push({
      key: 'playLand',
      label: `Play ${cardInfo.name}`,
      manaCost: null,
      isAvailable: false,
      action: null,
      actionType: 'playLand',
    })
  }

  // 3. Cast face-down (morph)
  if (morphAction) {
    options.push({
      key: 'castFaceDown',
      label: 'Cast Face-Down',
      manaCost: morphAction.manaCostString || '{3}',
      isAvailable: morphAction.isAffordable !== false,
      action: morphAction,
      actionType: 'castFaceDown',
    })
  }

  // 3b. Cast with kicker
  if (kickerAction) {
    options.push({
      key: 'castWithKicker',
      // Server picks the suffix — "(Kicked)", "(Offspring)", "(Bargained)", or "(with Flash)" for
      // flash-timing kickers like Ghitu Fire / Molten Exhale. Fall back to an unlabelled cast if
      // absent rather than guessing a mechanic.
      label: kickerAction.description || `Cast ${cardInfo.name}`,
      ...costFieldsFor(kickerAction, null),
      isAvailable: kickerAction.isAffordable !== false,
      action: kickerAction,
      actionType: 'castWithKicker',
    })
  }

  // 4. Cycling
  if (cycleAction) {
    options.push({
      key: 'cycle',
      label: 'Cycle',
      manaCost: cycleAction.manaCostString || null,
      isAvailable: cycleAction.isAffordable !== false,
      action: cycleAction,
      actionType: 'cycle',
    })
  }

  // 4b. Typecycling (e.g., Islandcycling, Swampcycling)
  if (typecycleAction) {
    options.push({
      key: 'typecycle',
      label: typecycleAction.description,
      manaCost: typecycleAction.manaCostString || null,
      isAvailable: typecycleAction.isAffordable !== false,
      action: typecycleAction,
      actionType: 'cycle',
    })
  }

  // 4c. Plot (CR 718) — sorcery-speed special action; sits alongside the cast option.
  if (plotAction) {
    options.push({
      key: 'plot',
      label: 'Plot',
      manaCost: plotAction.manaCostString || null,
      isAvailable: plotAction.isAffordable !== false,
      action: plotAction,
      actionType: 'plot',
    })
  }

  // 4d. Suspend (CR 702.62) — special action; sits alongside the (often unavailable) cast option.
  if (suspendAction) {
    options.push({
      key: 'suspend',
      label: 'Suspend',
      manaCost: suspendAction.manaCostString || null,
      isAvailable: suspendAction.isAffordable !== false,
      action: suspendAction,
      actionType: 'suspend',
    })
  }

  // 5. Activated abilities (for permanents on battlefield)
  const activateActions = legalActions.filter((a) => a.action.type === 'ActivateAbility')

  // 5a. Planeswalker: show the full loyalty ability menu, with unavailable abilities grayed
  // out (not enough loyalty, sorcery-speed restriction, already activated this turn, etc.).
  // This overrides the default "only show legal activate actions" rendering so the player
  // sees all three abilities on the card every time they click it.
  const pwAbilities = cardInfo.planeswalkerAbilities
  const renderedActivateActions = new Set<LegalActionInfo>()
  if (pwAbilities && pwAbilities.length > 0) {
    pwAbilities.forEach((pw, index) => {
      const match = activateActions.find(
        (a) => (a.action as { abilityId?: string }).abilityId === pw.abilityId
      )
      if (match) renderedActivateActions.add(match)
      options.push({
        key: `pw-${pw.abilityId}-${index}`,
        label: pw.description,
        manaCost: null,
        isAvailable: match !== undefined && match.isAffordable !== false,
        action: match ?? null,
        actionType: 'activate',
        loyaltyChange: pw.loyaltyChange,
        loyaltyX: pw.loyaltyX ?? false,
      })
    })
  }

  // 5b. Remaining (non-planeswalker) activated abilities — activated abilities on non-
  // planeswalker permanents, or anything not already rendered above.
  activateActions.forEach((activateAction, index) => {
    if (renderedActivateActions.has(activateAction)) return
    options.push({
      key: `activate-${index}`,
      label: activateAction.description,
      manaCost: activateAction.manaCostString || null,
      isAvailable: activateAction.isAffordable !== false,
      action: activateAction,
      actionType: 'activate',
    })
  })

  // 6. Turn face-up (morph)
  const turnFaceUpAction = legalActions.find((a) => a.action.type === 'TurnFaceUp')
  if (turnFaceUpAction) {
    options.push({
      key: 'turnFaceUp',
      label: 'Turn Face-Up',
      manaCost: turnFaceUpAction.manaCostString || null,
      isAvailable: turnFaceUpAction.isAffordable !== false,
      action: turnFaceUpAction,
      actionType: 'turnFaceUp',
    })
  }

  // 7. Crew (for Vehicles) and Saddle (for Mounts) — both are tap-for-power keyword actions
  const crewActions = legalActions.filter((a) => a.action.type === 'CrewVehicle')
  crewActions.forEach((crewAction, index) => {
    options.push({
      key: `crew-${index}`,
      label: crewAction.description,
      manaCost: null,
      isAvailable: crewAction.isAffordable !== false,
      action: crewAction,
      actionType: 'activate',
    })
  })

  const saddleActions = legalActions.filter((a) => a.action.type === 'SaddleMount')
  saddleActions.forEach((saddleAction, index) => {
    options.push({
      key: `saddle-${index}`,
      label: saddleAction.description,
      manaCost: null,
      isAvailable: saddleAction.isAffordable !== false,
      action: saddleAction,
      actionType: 'activate',
    })
  })

  // 8. Unlock Room door (CR 709.5e — sorcery-speed special action). Reuses 'activate'
  // styling so it lives in the same visual cluster as activated abilities.
  const unlockRoomActions = legalActions.filter((a) => a.action.type === 'UnlockRoomDoor')
  unlockRoomActions.forEach((unlockAction, index) => {
    options.push({
      key: `unlock-room-${index}`,
      label: unlockAction.description,
      manaCost: unlockAction.manaCostString || null,
      isAvailable: unlockAction.isAffordable !== false,
      action: unlockAction,
      actionType: 'activate',
    })
  })

  return options
}
