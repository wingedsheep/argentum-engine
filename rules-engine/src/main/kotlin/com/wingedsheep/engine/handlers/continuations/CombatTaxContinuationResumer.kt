package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation
import com.wingedsheep.engine.core.BlockTaxManaSelectionContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
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
    )

    private fun resumeAttackTaxSelection(
        state: GameState,
        continuation: AttackTaxManaSelectionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response for attack tax")
        }
        if (!response.autoPay && response.selectedSources.isEmpty()) {
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
        if (!response.autoPay && response.selectedSources.isEmpty()) {
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
                    currentState = currentState.updateEntity(source.entityId) { it.with(TappedComponent) }
                    events.add(TappedEvent(source.entityId, source.name))
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
                    currentState = currentState.updateEntity(sourceId) { it.with(TappedComponent) }
                    events.add(TappedEvent(sourceId, source.name))
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
}
