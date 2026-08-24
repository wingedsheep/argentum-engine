package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation
import com.wingedsheep.engine.core.BlockTaxManaSelectionContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

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
        resumer(AttackTaxManaSelectionContinuation::class) { state, continuation, response, _ ->
            resumeAttackTaxSelection(state, continuation, response)
        },
        resumer(BlockTaxManaSelectionContinuation::class) { state, continuation, response, _ ->
            resumeBlockTaxSelection(state, continuation, response)
        },
        resumer(com.wingedsheep.engine.core.AttackSacrificeSelectionContinuation::class) { state, continuation, response, _ ->
            resumeAttackSacrificeSelection(state, continuation, response)
        },
    )

    /**
     * Sacrifice the chosen permanents for one attacker's `CantAttackUnlessSacrifice` cost, then
     * either ask for the next attacker's cost or commit the declaration.
     *
     * The sacrifice reuses `ForceSacrificeExecutor.sacrificePermanents`, so it emits
     * `PermanentsSacrificedEvent`, snapshots the sacrificed permanents' characteristics, and lets
     * dies/sacrifice triggers fire — a hand-rolled zone move here would silently skip all three.
     */
    private fun resumeAttackSacrificeSelection(
        state: GameState,
        continuation: com.wingedsheep.engine.core.AttackSacrificeSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is com.wingedsheep.engine.core.CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for attack sacrifice")
        }
        if (response.selectedCards.size != continuation.count) {
            return ExecutionResult.error(
                state,
                "Must sacrifice exactly ${continuation.count} permanents to attack"
            )
        }

        val sacrificeResult = com.wingedsheep.engine.handlers.effects.zones.ForceSacrificeExecutor()
            .sacrificePermanents(state, continuation.attackingPlayer, response.selectedCards)
            .toExecutionResult()
        if (!sacrificeResult.isSuccess) return sacrificeResult

        val next = continuation.remaining.firstOrNull()
        if (next != null) {
            return services.combatManager.attackPhase.pauseForNextAttackSacrifice(
                state = sacrificeResult.state,
                attackingPlayer = continuation.attackingPlayer,
                attackers = continuation.attackers,
                payingAttacker = next.attackerId,
                count = next.count,
                remaining = continuation.remaining.drop(1),
                bands = continuation.bands,
                carryEvents = sacrificeResult.events.toList(),
            )
        }

        return services.combatManager.attackPhase.commitAttackDeclaration(
            state = sacrificeResult.state,
            attackingPlayer = continuation.attackingPlayer,
            attackers = continuation.attackers,
            projected = sacrificeResult.state.projectedState,
            taxEvents = sacrificeResult.events.toList(),
            bands = continuation.bands,
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
            return ExecutionResult.success(state)
        }

        val paid = payTax(state, continuation.attackingPlayer, continuation.manaCost, continuation.availableSources, response)
            ?: return ExecutionResult.error(state, "Cannot pay attack tax of ${continuation.manaCost}")

        return services.combatManager.attackPhase.commitAttackDeclaration(
            state = paid.state,
            attackingPlayer = continuation.attackingPlayer,
            attackers = continuation.attackers,
            projected = paid.state.projectedState,
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
        if (response.isDecline(floatingCovers(state, continuation.blockingPlayer, continuation.manaCost))) {
            return ExecutionResult.success(state)
        }

        val paid = payTax(state, continuation.blockingPlayer, continuation.manaCost, continuation.availableSources, response)
            ?: return ExecutionResult.error(state, "Cannot pay block tax of ${continuation.manaCost}")

        return services.combatManager.blockPhase.commitBlockDeclaration(
            state = paid.state,
            blockingPlayer = continuation.blockingPlayer,
            blockers = continuation.blockers,
            taxEvents = paid.events,
        )
    }

    private data class TaxPayment(val state: GameState, val events: List<GameEvent>)

    private fun payTax(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        availableSources: List<ManaSourceOption>,
        response: ManaSourcesSelectedResponse,
    ): TaxPayment? {
        val playerEntity = state.getEntity(playerId) ?: return null
        val poolComponent = playerEntity.get<ManaPoolComponent>() ?: return null
        var pool = ManaPool(
            poolComponent.white, poolComponent.blue, poolComponent.black,
            poolComponent.red, poolComponent.green, poolComponent.colorless,
        )

        val partial = pool.payPartial(manaCost)
        var remainingCost = partial.remainingCost
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                val solver = ManaSolver(services.cardRegistry)
                val solution = solver.solve(currentState, playerId, remainingCost) ?: return null
                for (source in solution.sources) {
                    val (tappedState, tapEvent) = tap(currentState, source.entityId)
                    currentState = tappedState
                    tapEvent?.let(events::add)
                }
                for ((_, production) in solution.manaProduced) {
                    pool = if (production.color != null) {
                        pool.add(production.color, production.amount)
                    } else {
                        pool.addColorless(production.colorless)
                    }
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
                    val (tappedState, tapEvent) = tap(currentState, sourceId)
                    currentState = tappedState
                    tapEvent?.let(events::add)
                    pool = when {
                        source.producesColors.isNotEmpty() -> pool.add(source.producesColors.first())
                        source.producesColorless -> pool.addColorless(1)
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
