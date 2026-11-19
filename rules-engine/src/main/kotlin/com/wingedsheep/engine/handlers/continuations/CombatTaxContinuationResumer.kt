package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.mechanics.mana.ManaAbilityLifeCost
import com.wingedsheep.engine.mechanics.mana.withSpendingColors
import com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker
import com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation
import com.wingedsheep.engine.core.BlockTaxManaSelectionContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.Outcome

/**
 * Resumes attack / block declarations that paused for the player to pick mana sources
 * for a generic mana tax (Propaganda, Ghostly Prison, Windborn Muse, Collective
 * Restraint, Whipgrass Entangler, etc.).
 *
 * The prompt is a [com.wingedsheep.engine.core.SelectManaSourcesDecision] with the
 * auto-pay suggestion pre-selected, so the default response taps the same lands the
 * old auto-tap path used to — the player can swap selections or cancel before any
 * mana is spent.
 *
 * Branches:
 *  - `autoPay = true` → run the solver and tap its suggested sources, commit declaration.
 *  - manual non-empty selection → tap the chosen sources, commit declaration.
 *  - empty manual selection (`autoPay = false`) → clean no-op, declaration cancelled.
 *
 * Sources requiring a sub-cost (e.g. Springleaf Drum's "tap another creature") aren't
 * supported as combat-tax payment yet; selecting one returns an error.
 */
class CombatTaxContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(com.wingedsheep.engine.core.AttackEnlistSelectionContinuation::class) { state, c, response, _ ->
            if (response !is com.wingedsheep.engine.core.CardsSelectedResponse) {
                ExecutionResult.error(state, "Expected enlist card selection")
            } else {
                val chosen = response.selectedCards.map { com.wingedsheep.engine.core.EnlistPayment(c.remaining.first(), it) }
                services.combatManager.attackPhase.chooseEnlist(state, c.attackingPlayer, c.attackers,
                    c.remaining.drop(1), c.chosen + chosen, c.bands, c.rollback)
            }
        },
        resumer(AttackTaxManaSelectionContinuation::class) { state, continuation, response, _ ->
            resumeAttackTaxSelection(state, continuation, response)
        },
        resumer(BlockTaxManaSelectionContinuation::class) { state, continuation, response, _ ->
            resumeBlockTaxSelection(state, continuation, response)
        },
        resumer(com.wingedsheep.engine.core.AttackPermanentCostSelectionContinuation::class) { state, continuation, response, _ ->
            resumeAttackPermanentCostSelection(state, continuation, response)
        },
        resumer(com.wingedsheep.engine.core.AttackExertSelectionContinuation::class) { state, continuation, response, _ ->
            resumeAttackExertSelection(state, continuation, response)
        },
    )

    /**
     * Pay one attacker's `CantAttackUnlessPermanentCost` with the chosen permanents — sacrifice
     * them or return them to their owners' hands — then either ask for the next attacker's cost
     * or commit the declaration.
     *
     * The choice is client-supplied, so each pick is re-checked against the same eligible set the
     * prompt offered. A sacrifice reuses `ForceSacrificeExecutor.sacrificePermanents`, so it emits
     * `PermanentsSacrificedEvent`, snapshots the sacrificed permanents' characteristics, and lets
     * dies/sacrifice triggers fire; a return moves them together through the zone-transition
     * service, which emits their zone-change events — a hand-rolled zone move here would silently
     * skip all of that.
     */
    private fun resumeAttackPermanentCostSelection(
        state: GameState,
        continuation: com.wingedsheep.engine.core.AttackPermanentCostSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is com.wingedsheep.engine.core.CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for attack cost")
        }
        val chosen = response.selectedCards.distinct()
        if (chosen.size != continuation.count) {
            return ExecutionResult.error(
                state,
                "Must choose exactly ${continuation.count} permanents to attack"
            )
        }
        val requirement = com.wingedsheep.engine.mechanics.combat.AttackPermanentCosts.requirementFor(
            state, continuation.payingAttacker, services.cardRegistry
        ) ?: return ExecutionResult.error(state, "Attacker no longer has an attack cost")
        val payable = com.wingedsheep.engine.mechanics.combat.AttackPermanentCosts.payableOptions(
            state, continuation.attackingPlayer, continuation.payingAttacker, requirement,
            continuation.count, continuation.remaining, services.cardRegistry, services.predicateEvaluator
        ).toSet()
        if (!payable.containsAll(chosen)) {
            return ExecutionResult.error(state, "Chosen permanents can't pay this attack cost")
        }

        val paidResult = when (requirement.action) {
            com.wingedsheep.sdk.scripting.AttackPermanentCostAction.SACRIFICE ->
                com.wingedsheep.engine.handlers.effects.zones.ForceSacrificeExecutor(services.zones, dynamicAmountEvaluator = services.dynamicAmountEvaluator)
                    .sacrificePermanents(state, continuation.attackingPlayer, chosen)
                    .toExecutionResult()
            com.wingedsheep.sdk.scripting.AttackPermanentCostAction.RETURN_TO_HAND -> {
                val moved = services.zones.moveToZoneBatch(state, chosen, com.wingedsheep.sdk.core.Zone.HAND)
                ExecutionResult.success(moved.state, moved.events)
            }
        }
        if (paidResult.outcome !is Outcome.Done) return paidResult
        // Each pick was payable on its own; a multi-permanent pick could still jointly take what a
        // later cost needs. Refuse it so the player picks again rather than meeting an empty prompt.
        if (!com.wingedsheep.engine.mechanics.combat.AttackPermanentCosts.remainingAffordable(
                paidResult.state, continuation.attackingPlayer, continuation.remaining,
                services.cardRegistry, services.predicateEvaluator
            )
        ) {
            return ExecutionResult.error(state, "That choice leaves another attacker's cost unpayable")
        }
        // A declared attacker paid away as the cost (an enchantment creature returned, a creature
        // sacrificed) is no longer on the battlefield, so it doesn't become an attacking creature
        // (CR 508.1k): drop it from the declaration and its band.
        val battlefield = paidResult.state.getBattlefield().toSet()
        val attackers = continuation.attackers.filterKeys { it in battlefield }
        val bands = continuation.bands.map { band -> band.filterTo(mutableSetOf()) { it in battlefield } }
            .filter { it.size > 1 }

        val next = continuation.remaining.firstOrNull()
        if (next != null) {
            return services.combatManager.attackPhase.pauseForNextAttackPermanentCost(
                state = paidResult.state,
                attackingPlayer = continuation.attackingPlayer,
                attackers = attackers,
                payingAttacker = next.attackerId,
                count = next.count,
                remaining = continuation.remaining.drop(1),
                bands = bands,
                carryEvents = paidResult.events.toList(),
            )
        }

        return services.combatManager.attackPhase.commitAttackDeclaration(
            state = paidResult.state,
            attackingPlayer = continuation.attackingPlayer,
            attackers = attackers,
            projected = paidResult.state.projectedState,
            taxEvents = paidResult.events.toList(),
            bands = bands,
        )
    }

    /** Commit the declaration with the attackers the player chose to exert (CR 701.43d). */
    private fun resumeAttackExertSelection(
        state: GameState,
        continuation: com.wingedsheep.engine.core.AttackExertSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is com.wingedsheep.engine.core.CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for exert")
        }
        // The decision's own validation already rejects anything outside the offered attackers.
        val chosen = response.selectedCards.toSet()
        return services.combatManager.attackPhase.finishAttackDeclaration(
            state = state,
            attackingPlayer = continuation.attackingPlayer,
            attackers = continuation.attackers,
            projected = state.projectedState,
            taxEvents = emptyList(),
            bands = continuation.bands,
            exerted = chosen,
        )
    }

    private fun resumeAttackTaxSelection(
        state: GameState,
        continuation: AttackTaxManaSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response for attack tax")
        }
        if (response.isDecline(floatingCovers(state, continuation.attackingPlayer, continuation.manaCost))) {
            // Decline: no mana tapped, no AttackingComponent applied. Drop back into
            // DECLARE_ATTACKERS as a clean no-op (no error banner).
            val restored = continuation.rollback?.restore(state) ?: state
            return ExecutionResult.success(restored, listOf(com.wingedsheep.engine.core.AttackDeclarationCancelledEvent(continuation.attackingPlayer)))
        }

        val paid = payTax(state, continuation.attackingPlayer, continuation.manaCost, continuation.availableSources, response,
            continuation.enlistments.mapTo(mutableSetOf()) { it.enlistedId })
            ?: return ExecutionResult.error(state, "Cannot pay attack tax of ${continuation.manaCost}")

        return services.combatManager.attackPhase.payEnlistAndContinue(
            state = paid.state,
            attackingPlayer = continuation.attackingPlayer,
            attackers = continuation.attackers,
            enlistments = continuation.enlistments,
            taxEvents = paid.events,
            bands = continuation.bands,
        )
    }

    private fun resumeBlockTaxSelection(
        state: GameState,
        continuation: BlockTaxManaSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response for block tax")
        }
        if (response.isDecline(floatingCovers(state, continuation.payingPlayer, continuation.manaCost))) {
            return ExecutionResult.success(state.withPriority(continuation.blockingPlayer))
        }

        val selections = continuation.selections + com.wingedsheep.engine.core.BlockTaxSelection(
            continuation.payingPlayer, continuation.manaCost, continuation.availableSources, response)
        // Simulate the selected payments together, without exposing a partially paid declaration.
        // Explicit mana abilities used inside the window remain in `state` if the team cancels.
        var paidState = state
        val events = mutableListOf<GameEvent>()
        for (selection in selections) {
            if (!selection.response.autoPay &&
                !floatingCovers(paidState, selection.playerId, selection.manaCost)) {
                val currentSources = services.manaSolver.findAvailableManaSources(paidState, selection.playerId)
                    .mapTo(mutableSetOf()) { it.entityId }
                if (selection.response.selectedSources.distinct().size != selection.response.selectedSources.size ||
                    selection.response.selectedSources.any { it !in currentSources }) {
                    return ExecutionResult.error(state, "Selected block-tax mana source is no longer available")
                }
            }
            val paid = payTax(paidState, selection.playerId, selection.manaCost,
                selection.availableSources, selection.response)
                ?: return ExecutionResult.error(state, "Cannot pay block tax of ${selection.manaCost}")
            paidState = paid.state
            events.addAll(paid.events)
        }
        if (continuation.remainingTaxes.isNotEmpty()) {
            return services.combatManager.blockPhase.pauseForBlockTaxConfirmation(
                state, continuation.blockingPlayer, continuation.blockers,
                continuation.remainingTaxes, selections)
        }
        return services.combatManager.blockPhase.commitBlockDeclaration(
            state = paidState.withPriority(continuation.blockingPlayer),
            blockingPlayer = continuation.blockingPlayer,
            blockers = continuation.blockers,
            taxEvents = events,
        )
    }

    private data class TaxPayment(val state: GameState, val events: List<GameEvent>)

    private fun payTax(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        availableSources: List<ManaSourceOption>,
        response: ManaSourcesSelectedResponse,
        reserved: Set<EntityId> = emptySet(),
    ): TaxPayment? {
        val playerEntity = state.getEntity(playerId) ?: return null
        val poolComponent = playerEntity.get<ManaPoolComponent>() ?: return null
        var pool = ManaPool(
            poolComponent.white, poolComponent.blue, poolComponent.black,
            poolComponent.red, poolComponent.green, poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana,
            snowMana = poolComponent.snowMana,
            manaUnits = poolComponent.manaUnits,
            snowColorless = poolComponent.snowColorless
        ).withSpendingColors(state, playerId)

        val partial = pool.payPartial(manaCost)
        var remainingCost = partial.remainingCost
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                val solver = services.manaSolver
                val solution = solver.solve(currentState, playerId, remainingCost, excludeSources = reserved) ?: return null
                for (source in solution.sources) {
                    val (tappedState, tapEvents) = ManaAbilityLifeCost.tapForManaPayingLife(services.zones, currentState, source.entityId, playerId, solution.manaProduced[source.entityId])
                    currentState = tappedState
                    events.addAll(tapEvents)
                }
                for ((_, production) in solution.manaProduced) {
                    pool = pool.addProduction(production)
                }
            } else {
                val sourceMap = availableSources.associateBy { it.entityId }
                for (sourceId in response.selectedSources) {
                    val source = sourceMap[sourceId] ?: return null
                    if (source.requiresSacrifice || source.requiresTappingAnotherPermanent) {
                        // Combat-tax payment doesn't support sac / sub-cost sources yet — fall back
                        // to returning null so the caller errors with a clear message.
                        return null
                    }
                    val (tappedState, tapEvents) = ManaAbilityLifeCost.tapForManaPayingLife(services.zones, currentState, sourceId, playerId, ManaAbilityLifeCost.creditedProduction(source.producesColors, source.manaAmount))
                    currentState = tappedState
                    events.addAll(tapEvents)
                    pool = when {
                        source.producesColors.isNotEmpty() -> source.producesColors.first().let { color ->
                            pool.add(color).let { if (ManaProvenanceTracker.isSnowSource(currentState, sourceId)) it.markSnow(color, 1) else it }
                        }
                        source.producesColorless ->
                            pool.addColorless(1).let { if (ManaProvenanceTracker.isSnowSource(currentState, sourceId)) it.markSnow(null, 1) else it }
                        else -> pool
                    }
                }
            }
        }

        val newPool = pool.pay(manaCost) ?: return null
        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white, blue = newPool.blue, black = newPool.black,
                    red = newPool.red, green = newPool.green, colorless = newPool.colorless,
                    restrictedMana = newPool.restrictedMana,
                    snowMana = newPool.snowMana,
                    manaUnits = newPool.manaUnits,
                    snowColorless = newPool.snowColorless
                )
            )
        }
        return TaxPayment(currentState, events)
    }

    /**
     * Whether [playerId]'s floating mana already covers [cost] — see
     * [ManaSourcesSelectedResponse.isDecline]. A player who taps their own sources during the
     * payment window (CR 605.3a) confirms with an empty selection, which must not read as a refusal.
     */
    private fun floatingCovers(state: GameState, playerId: EntityId, cost: ManaCost): Boolean =
        com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.floatingManaCovers(state, playerId, cost)
}
