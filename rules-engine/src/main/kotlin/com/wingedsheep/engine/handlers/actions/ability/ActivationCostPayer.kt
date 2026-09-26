package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.CostPaymentChoices
import com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.state.components.stack.captureLastKnown
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Last-known information an activation captures while paying its costs (CR 113.7a / 608.2h), for
 * the resolving ability to read after those costs moved the objects it names.
 */
internal data class ActivationCostSnapshots(
    val sacrificed: List<EntitySnapshot>,
    val tapped: List<EntitySnapshot>,
    val lastKnownSourceCounters: Map<CounterType, Int>,
    val lastKnownSourceSnapshot: EntitySnapshot?,
    val lastKnownSourceAttachments: List<EntityId>,
    val revealedNotedCreatureType: String?,
)

/** What paying an activation's costs produced, for the stack / mana-ability stage to use. */
internal data class ActivationPayment(
    val state: GameState,
    /** The cost-side events, in order: alternative payments, mana abilities, cost atoms, loyalty. */
    val events: List<GameEvent>,
    /** The mana portion actually charged, after convoke/waterbend (null when the cost has none). */
    val manaCost: ManaCost?,
    /** Whether this is a Station-style multi-select tap batch (CR 702.184a). */
    val isTapBatch: Boolean,
    /** The permanents this activation (the first of a batch) tapped for its tap-permanents cost. */
    val firstTapSlice: List<EntityId>,
    /** The cards the cost exiled, as fed to payment. */
    val exileChoices: List<EntityId>,
    /**
     * The cards the cost atoms discarded — chosen, random, or the source itself — read off the
     * atoms' own discard events, so a mana ability tapped to pay the mana portion never adds one.
     */
    val discardedCards: List<EntityId>,
    val snapshots: ActivationCostSnapshots,
)

internal sealed interface ActivationPaymentOutcome {
    data class Paid(val payment: ActivationPayment) : ActivationPaymentOutcome
    data class Failed(val reason: String) : ActivationPaymentOutcome
}

/**
 * Pays an announced activation's total cost (CR 601.2g–h via CR 602.2b): alternative payments
 * (convoke, waterbend), mana abilities for the mana portion — the player's explicit sources or the
 * [ActivationAutoTapper] — then every cost atom through `CostHandler.payAbilityCost`, the X portion
 * from the pool, and the pool writeback. Last-known information about what the costs are about to
 * move is captured just before the atoms are paid.
 */
internal class ActivationCostPayer(
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val autoTapper: ActivationAutoTapper,
    private val predicateEvaluator: PredicateEvaluator
) {

    fun pay(
        state: GameState,
        activation: Activation,
        paymentContext: SpellPaymentContext?,
    ): ActivationPaymentOutcome {
        val action = activation.action
        val ability = activation.ability
        val effectiveCost = activation.effectiveCost
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Get player's mana pool
        val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        var manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana,
            // Carry mana-source provenance through the activation. pay()/spend() decrement colours
            // but leave these maps untouched; the writeback below consumes them proportional to the
            // floating mana actually spent, so tags for mana floated from earlier sources survive an
            // ability activation instead of being wiped (mirrors CastPaymentProcessor's threading).
            manaBySubtype = poolComponent.manaBySubtype,
            manaBySource = poolComponent.manaBySource
        )

        // For an VariablePermanents cost, X is the exiled permanents' total mana value (computed at
        // announcement); otherwise it's the action's chosen X. Identical to `action.xValue ?: 0` for
        // every other card.
        val xValue = activation.effectiveXValue ?: 0

        // Pay mana costs before paying other costs
        val (afterAlternative, manaCost) =
            applyAlternativePayments(currentState, activation, effectiveCost.extractManaCost(), events)
        currentState = afterAlternative

        if (manaCost != null) {
            when (val tapped = activateManaAbilities(currentState, activation, manaPool, manaCost, xValue, paymentContext)) {
                is ManaTapOutcome.Failed -> return ActivationPaymentOutcome.Failed(tapped.reason)
                is ManaTapOutcome.Tapped -> {
                    currentState = tapped.state
                    manaPool = tapped.pool
                    events.addAll(tapped.events)
                }
            }
        }

        // Station-style multi-select batch (CR 702.184a): when repeatCount > 1 over a single-
        // creature tap cost, `tappedPermanents` holds one creature per queued activation. Each
        // activation taps exactly its own creature, so slice the list — this activation gets the
        // first creature; the repeat loop consumes the rest one at a time. For every other
        // ability (no tap cost, or repeatCount == 1) the slice is the whole list, unchanged.
        val tapBatchAtom = if (action.repeatCount > 1) effectiveCost.firstTapPermanentsAtomOrNull() else null
        val isTapBatch = tapBatchAtom != null && tapBatchAtom.count == 1 &&
            (action.costPayment?.tappedPermanents?.size ?: 0) == action.repeatCount
        val firstTapSlice = if (isTapBatch) {
            listOf(action.costPayment!!.tappedPermanents.first())
        } else {
            action.costPayment?.tappedPermanents ?: emptyList()
        }

        val exileChoices = resolveExileChoices(currentState, action, effectiveCost)

        // Build cost payment choices from the action
        val costChoices = CostPaymentChoices(
            sacrificeChoices = action.costPayment?.sacrificedPermanents ?: emptyList(),
            discardChoices = action.costPayment?.discardedCards ?: emptyList(),
            putOnLibraryChoices = action.costPayment?.cardsPutOnLibrary ?: emptyList(),
            exileChoices = exileChoices,
            variablePermanentChoices = action.costPayment?.variableCostPermanents ?: emptyList(),
            tapChoices = firstTapSlice,
            bounceChoices = action.costPayment?.bouncedPermanents ?: emptyList(),
            xValue = xValue,
            distributedCounterRemovals = action.costPayment?.distributedCounterRemovals ?: emptyList(),
            blightChoices = action.costPayment?.blightTargets ?: emptyList(),
            granterId = activation.staticGranterId
        )

        val snapshots = captureCostSnapshots(currentState, action, effectiveCost, firstTapSlice)

        // When using Explicit payment, mana sources were already tapped above —
        // strip the Mana portion so payAbilityCost doesn't try to deduct from the pool.
        // When convoke was applied, replace the mana portion with the reduced cost.
        val costForPayment = if (action.paymentStrategy is PaymentStrategy.Explicit) {
            effectiveCost.stripManaCost()
        } else if ((ability.hasConvoke || ability.hasWaterbend) && action.alternativePayment != null && !action.alternativePayment.isEmpty && manaCost != null) {
            // Convoke/waterbend reduced the mana cost — update the cost structure so payAbilityCost
            // deducts the reduced amount from the pool instead of the original full amount
            effectiveCost.withManaPortion(manaCost)
        } else {
            effectiveCost
        }

        // Pay the cost (using effective cost with text replacements applied)
        val costResult = costHandler.payAbilityCost(
            currentState,
            costForPayment,
            action.sourceId,
            action.playerId,
            manaPool,
            costChoices,
            paymentContext,
        )

        if (!costResult.success) {
            return ActivationPaymentOutcome.Failed(costResult.error ?: "Failed to pay ability cost")
        }

        currentState = costResult.newState!!
        manaPool = costResult.newManaPool!!

        // Collect events from cost payment (e.g., sacrifice events)
        events.addAll(costResult.events)

        // Deduct X mana from the pool. ManaPool.pay() skips X symbols ("handled by caller"),
        // so we must explicitly spend the X portion here (same pattern as CastSpellHandler.autoPay).
        // Skip for Explicit payment — sources were already tapped to cover the full cost including X.
        if (action.paymentStrategy !is PaymentStrategy.Explicit && manaCost != null && manaCost.hasX && xValue > 0) {
            manaPool = spendXFromPool(manaPool, manaCost, xValue, ability.xManaRestriction)
        }

        currentState = writeBackPool(currentState, action.playerId, poolComponent, manaPool)

        // Emit events for cost types. Tap/TapAttachedCreature/TapXPermanents taps are emitted by
        // the tap atom inside costHandler.payAbilityCost (folded in via costResult.events above), so
        // only the loyalty change — which payAbilityCost mutates without an event — is emitted here.
        val abilityCost = ability.cost
        if (abilityCost is AbilityCost.Loyalty && abilityCost.change > 0) {
            // A [+N] cost *puts* N loyalty counters on the planeswalker (CR 606.4), and the
            // activating player is the one putting them (CR 122.6). Emitting the ordinary
            // counters-placed event lets "whenever you put one or more loyalty counters on a
            // planeswalker" (Inspired Tethermage) and any-kind counter triggers see it. It is a
            // cost, not an effect, so counter-placement replacements (Doubling Season) don't
            // apply — CostHandler already added exactly `change` counters.
            val (marked, firstThisTurn) = com.wingedsheep.engine.handlers.effects.DamageUtils.recordCounterPlacement(
                currentState, action.sourceId, CounterType.LOYALTY, placerId = action.playerId
            )
            currentState = marked
            events.add(
                com.wingedsheep.engine.core.CountersAddedEvent(
                    action.sourceId, CounterType.LOYALTY, abilityCost.change, activation.sourceName,
                    firstThisTurn, placedBy = action.playerId
                )
            )
        } else if (abilityCost is AbilityCost.Loyalty) {
            events.add(LoyaltyChangedEvent(action.sourceId, activation.sourceName, abilityCost.change))
        } else if (abilityCost == AbilityCost.LoyaltyX) {
            events.add(LoyaltyChangedEvent(action.sourceId, activation.sourceName, -xValue))
        }

        return ActivationPaymentOutcome.Paid(
            ActivationPayment(
                state = currentState,
                events = events.toList(),
                manaCost = manaCost,
                isTapBatch = isTapBatch,
                firstTapSlice = firstTapSlice,
                exileChoices = exileChoices,
                discardedCards = costResult.events.filterIsInstance<CardsDiscardedEvent>().flatMap { it.cardIds },
                snapshots = snapshots,
            )
        )
    }

    /**
     * Convoke and waterbend: tap permanents to pay part of the mana portion (CR 702.51a / 701.67),
     * returning the state after those taps and the mana still owed.
     */
    private fun applyAlternativePayments(
        state: GameState,
        activation: Activation,
        initialManaCost: ManaCost?,
        events: MutableList<GameEvent>,
    ): Pair<GameState, ManaCost?> {
        val action = activation.action
        val ability = activation.ability
        var currentState = state
        var effectiveManaCost = initialManaCost

        // Apply convoke payment for abilities with hasConvoke (e.g., Heirloom Epic)
        if (effectiveManaCost != null && ability.hasConvoke && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val convokeResult = alternativePaymentHandler.applyConvokeForAbility(
                currentState, effectiveManaCost, action.alternativePayment, action.playerId
            )
            effectiveManaCost = convokeResult.reducedCost
            currentState = convokeResult.newState
            events.addAll(convokeResult.events)
        }

        // Apply waterbend payment for abilities with hasWaterbend (Avatar: The Last Airbender) —
        // tap untapped artifacts/creatures you control, each paying {1} of the generic cost.
        if (effectiveManaCost != null && ability.hasWaterbend && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val waterbendResult = alternativePaymentHandler.applyWaterbendForAbility(
                currentState, effectiveManaCost, action.alternativePayment, action.playerId
            )
            effectiveManaCost = waterbendResult.reducedCost
            currentState = waterbendResult.newState
            events.addAll(waterbendResult.events)
        }
        // CR 701.67c: paying an ability's waterbend cost (however paid — taps above and/or the mana
        // paid below) fires "whenever you waterbend". The waterbend cost is applied before mana
        // payment, so this reaches every hasWaterbend activation; a later mana failure rolls the
        // whole activation (and this event) back.
        if (ability.hasWaterbend) {
            val (bendState, bendEvent) = BendEvents.record(currentState, action.playerId, BendType.WATER)
            currentState = bendState
            events.add(bendEvent)
        }
        return currentState to effectiveManaCost
    }

    private sealed interface ManaTapOutcome {
        data class Tapped(val state: GameState, val pool: ManaPool, val events: List<GameEvent>) : ManaTapOutcome
        data class Failed(val reason: String) : ManaTapOutcome
    }

    /**
     * Activate mana abilities for the mana portion (CR 601.2g): the player's explicitly chosen
     * sources, or the auto-tap fast path.
     */
    private fun activateManaAbilities(
        state: GameState,
        activation: Activation,
        pool: ManaPool,
        manaCost: ManaCost,
        xValue: Int,
        paymentContext: SpellPaymentContext?,
    ): ManaTapOutcome {
        val action = activation.action
        val ability = activation.ability
        // Only pass xValue to auto-tap when X is in the mana cost itself (not in a non-mana cost like counter removal)
        val manaXValue = if (manaCost.hasX) xValue else 0
        // If the outer ability's cost includes Tap, the source itself cannot also be used
        // as a mana source — the single "tap" it has is already consumed by the outer cost.
        val selfExcludedSources = if (activation.effectiveCost.hasTapCost()) setOf(action.sourceId) else emptySet()
        return when (action.paymentStrategy) {
            is PaymentStrategy.Explicit -> {
                // Spend floating mana first, then tap only the minimum subset of chosen
                // sources required to cover what the pool can't — parity with the auto-tap
                // branch below (ActivationAutoTapper) and CastPaymentProcessor.autoPay. Without
                // the payPartial, mana already in the pool is stranded: the solver would tap
                // sources for the whole cost and the pool deduction is skipped (Mana stripped
                // in costForPayment), so pre-floated mana is never spent. This bit
                // waterbend/convoke abilities in particular — the client always routes them
                // through Explicit payment, and the enumerator deems them affordable counting
                // pool + sources, so ignoring the pool here made a legal activation fail
                // ("Selected mana sources cannot pay this ability's cost") or over-tap lands.
                // The reduced pool flows into payAbilityCost and is persisted afterward.
                var currentState = state
                val events = mutableListOf<GameEvent>()
                val partialResult = pool.payPartial(manaCost, paymentContext)
                val remainingCost = partialResult.remainingCost
                if (!remainingCost.isEmpty() || manaXValue > 0) {
                    // Solve the remainder against the chosen sources only (non-chosen excluded),
                    // matching CastPaymentProcessor.explicitPay so we never tap more than needed.
                    // The client's auto-tap preview is computed against the full cost and may
                    // over-select; excluding the rest keeps validation and execution in sync.
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(currentState, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet() + selfExcludedSources
                    val solution = manaSolver.solve(
                        currentState, action.playerId, remainingCost, manaXValue, excludeSources = excluded, xManaRestriction = ability.xManaRestriction
                    ) ?: return ManaTapOutcome.Failed("Selected mana sources cannot pay this ability's cost")
                    for (source in solution.sources) {
                        val (tappedState, tapEvent) = tap(currentState, source.entityId)
                        currentState = tappedState
                        tapEvent?.let(events::add)
                    }
                }
                ManaTapOutcome.Tapped(currentState, partialResult.newPool, events)
            }
            else -> {
                val autoTapResult = autoTapper.autoTapForManaCost(
                    state, action.playerId, pool, manaCost, manaXValue, selfExcludedSources,
                    paymentContext, ability.xManaRestriction
                ) ?: return ManaTapOutcome.Failed("Not enough mana to activate this ability")
                ManaTapOutcome.Tapped(autoTapResult.newState, autoTapResult.newPool, autoTapResult.events)
            }
        }
    }

    /**
     * The cards an exile cost will exile.
     *
     * A sum-gated graveyard exile cost (`ExileFromGraveyardForTotal`) decides *here* which cards
     * it will exile, rather than leaving CostHandler to fall back on its own pick during
     * payment. Two things need the same answer and would otherwise diverge: the payment, and
     * the record of "those exiled cards" that rides the stack to resolution. Resolving it once
     * and feeding it into `exileChoices` makes them the same list by construction.
     *
     * This *replaces* the submitted list rather than merging into it, which is only correct
     * because such a cost is the ability's sole exile atom — see
     * [firstExileForTotalAtomOrNull]. A composite pairing it with another exile atom would
     * drop that atom's selection here.
     */
    private fun resolveExileChoices(
        state: GameState,
        action: ActivateAbility,
        effectiveCost: AbilityCost,
    ): List<EntityId> {
        val totalExileAtom = effectiveCost.firstExileForTotalAtomOrNull()
            ?: return action.costPayment?.exiledCards ?: emptyList()
        val resolver = GraveyardTotalExileResolver
        return resolver.resolveSelection(
            resolver.candidates(
                state, action.playerId, totalExileAtom.measure, totalExileAtom.filter,
                predicateEvaluator = predicateEvaluator
            ),
            totalExileAtom.minTotal,
            action.costPayment?.exiledCards ?: emptyList(),
        )
    }

    /**
     * Capture, before the cost atoms are paid, the last-known information the resolving ability
     * may need about the objects those atoms move.
     */
    private fun captureCostSnapshots(
        state: GameState,
        action: ActivateAbility,
        effectiveCost: AbilityCost,
        firstTapSlice: List<EntityId>,
    ): ActivationCostSnapshots {
        // Snapshot projected subtypes and P/T of sacrifice targets before zone change
        // (Rule 113.7a / 608.2h — "as it last existed on the battlefield"). Covers both the
        // fixed-count sacrifice cost and a variable-count one, which moves permanents just the same.
        //
        // A *forced* sacrifice (candidates <= count) never pauses for a choice, so the action
        // arrives with no chosen permanents and CostHandler auto-picks them during payment. Snapshot
        // that same set here, or "deals damage equal to the sacrificed creature's power" (Brion
        // Stoutarm with exactly one other creature) resolves EffectTarget.SacrificedAsCost to nothing
        // and deals 0.
        val sacrificeCost = effectiveCost.extractSacrificeCost()
        val chosenSacrifices = action.costPayment?.sacrificedPermanents ?: emptyList()
        val forcedSacrifices = if (chosenSacrifices.isEmpty() && sacrificeCost != null) {
            costHandler
                .findMatchingCardsUnified(
                    state, state.getBattlefield(action.playerId), sacrificeCost.filter,
                    action.playerId, sourceId = action.sourceId,
                )
                .let { if (sacrificeCost.excludeSelf) it.filter { id -> id != action.sourceId } else it }
                .takeIf { it.size <= sacrificeCost.count && !sacrificeCost.distinctNames }
                .orEmpty()
        } else {
            emptyList()
        }
        // "Sacrifice all …" chooses nothing either — every matching permanent goes.
        val sacrificeAllTargets = effectiveCost.extractSacrificeAllCost()
            ?.let { costHandler.sacrificeAllCandidates(state, it, action.playerId, action.sourceId) }
            .orEmpty()
        val sacrificeTargetIds = chosenSacrifices + forcedSacrifices + sacrificeAllTargets +
            (action.costPayment?.variableCostPermanents ?: emptyList())
        val sacrificedSnapshots = captureEntitySnapshots(sacrificeTargetIds, state.projectedState)

        // Mirror sacrifice snapshots for tapped-as-cost permanents — they may leave the
        // battlefield in response while the ability is on the stack.
        val tappedSnapshots = captureEntitySnapshots(firstTapSlice, state.projectedState)

        val movesSource = effectiveCost.exilesOrSacrificesSelf()

        // Snapshot the source's counters before a self-exile / self-sacrifice cost wipes them
        // (CR 113.7a / 122.2), so the effect can read the pre-cost count via
        // DynamicAmount.LastKnownSourceCounters (Lost Isle Calling).
        val lastKnownSourceCounters: Map<CounterType, Int> =
            if (movesSource) {
                state.getEntity(action.sourceId)
                    ?.get<CountersComponent>()
                    ?.counters
                    ?.filterValues { it > 0 } ?: emptyMap()
            } else emptyMap()

        // Snapshot the source's projected characteristics before a self-exile / self-sacrifice cost
        // moves it off the battlefield (CR 113.7a / 608.2h), so an effect that reads its own power —
        // e.g. "Sacrifice this creature: it deals damage equal to its power" (Ghitu Fire-Eater,
        // Cinder Shade, Blazing Bomb's Blow Up) — sees the pre-sacrifice power rather than zero.
        // Mirrors lastKnownSourceCounters above.
        //
        // The projected *type line* and token-ness ride along because for a **token** source this
        // snapshot is the only surviving record of the object at all: CR 704.5d sweeps a token out
        // of any non-battlefield zone as a state-based action and the entity is deleted outright,
        // so by the time the ability sits on the stack `state.getEntity(sourceId)` is null. That is
        // what lets "copy target activated ability you control from an artifact source" (Scientist
        // Supreme of A.I.M.) still see a cracked Clue as an artifact source — see
        // `CardPredicate.AbilitySourceMatches` in PredicateEvaluator. Reading the *projected* type
        // line here also gets the animated-artifact / crewed-Vehicle source right.
        //
        val lastKnownSourceSnapshot: EntitySnapshot? =
            if (movesSource) captureLastKnown(state, action.sourceId) else null

        // Snapshot the entity ids attached to the source before a self-exile / self-sacrifice cost
        // moves it off the battlefield (CR 113.7a). The host's live AttachmentsComponent is gone by
        // resolution, so capture it now — read via CardSource.LastKnownEquipmentAttachedToSource to
        // re-attach "an Equipment that was attached to it" (Zack Fair). Mirrors lastKnownSourceCounters.
        val lastKnownSourceAttachments: List<EntityId> =
            if (movesSource) {
                state.getEntity(action.sourceId)
                    ?.get<AttachmentsComponent>()
                    ?.attachedIds
                    ?: emptyList()
            } else emptyList()

        // Snapshot the creature type this permanent's controller secretly noted, before the same
        // cost's self-sacrifice takes the permanent — and the note with it — off the battlefield
        // (CR 113.7a). Read at resolution as chosenValues["chosenCreatureType"], which is what lets
        // A Killer Among Us still ask "is the target the chosen type?" after it is in the graveyard.
        // Mirrors lastKnownSourceCounters above.
        val revealedNotedCreatureType: String? =
            if (effectiveCost.revealsNotedCreatureType()) {
                state.getEntity(action.sourceId)
                    ?.get<NotedCreatureTypesComponent>()
                    ?.types
                    ?.firstOrNull()
            } else null

        return ActivationCostSnapshots(
            sacrificed = sacrificedSnapshots,
            tapped = tappedSnapshots,
            lastKnownSourceCounters = lastKnownSourceCounters,
            lastKnownSourceSnapshot = lastKnownSourceSnapshot,
            lastKnownSourceAttachments = lastKnownSourceAttachments,
            revealedNotedCreatureType = revealedNotedCreatureType,
        )
    }

    /**
     * Spend the X portion of [manaCost] from [pool]: X per X symbol, colorless first unless X is
     * color-restricted ("spend only [colors] on X"), then the allowed colors.
     */
    private fun spendXFromPool(pool: ManaPool, manaCost: ManaCost, xValue: Int, xManaRestriction: Set<Color>): ManaPool {
        var manaPool = pool
        val xSymbolCount = manaCost.xCount.coerceAtLeast(1)
        var xRemainingToPay = xValue * xSymbolCount
        val xColorsAllowed: Set<Color> =
            if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction

        // Spend colorless first for X — never allowed when X is color-restricted ("spend only [colors] on X").
        if (xManaRestriction.isEmpty()) {
            while (xRemainingToPay > 0 && manaPool.colorless > 0) {
                manaPool = manaPool.spendColorless()!!
                xRemainingToPay--
            }
        }

        // Spend colored mana for remaining X (restricted to allowed colors).
        for (color in Color.entries) {
            if (color !in xColorsAllowed) continue
            while (xRemainingToPay > 0 && manaPool.get(color) > 0) {
                manaPool = manaPool.spend(color)!!
                xRemainingToPay--
            }
        }
        return manaPool
    }

    /**
     * Always update mana pool on state after cost payment.
     * The auto-tapper writes the enriched (pre-payment) pool to state,
     * so we must unconditionally write the post-payment pool.
     * Consume mana-source provenance for the floating mana this activation spent (the maps ride
     * `manaPool` untouched by pay()/spend()), so the remaining tags reflect only the mana still
     * in the pool — a mana ability that only adds mana consumes nothing and keeps prior tags.
     */
    private fun writeBackPool(
        state: GameState,
        playerId: EntityId,
        poolComponent: ManaPoolComponent,
        manaPool: ManaPool,
    ): GameState {
        val originalUnrestricted = poolComponent.white + poolComponent.blue + poolComponent.black +
            poolComponent.red + poolComponent.green + poolComponent.colorless
        val finalUnrestricted = manaPool.white + manaPool.blue + manaPool.black +
            manaPool.red + manaPool.green + manaPool.colorless
        val (poolAfterProvenance, _) = manaPool.consumeProvenance(maxOf(0, originalUnrestricted - finalUnrestricted))
        return state.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = manaPool.white,
                blue = manaPool.blue,
                black = manaPool.black,
                red = manaPool.red,
                green = manaPool.green,
                colorless = manaPool.colorless,
                restrictedMana = manaPool.restrictedMana,
                manaBySubtype = poolAfterProvenance.manaBySubtype,
                manaBySource = poolAfterProvenance.manaBySource
            ))
        }
    }
}
