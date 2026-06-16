package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap

/**
 * Applies the [AdditionalManaOnSourceTap] *mirror* bonus (`color = null` — "add one mana of any
 * type that source produced") for a tap whose produced color is only known after a color choice
 * resolves.
 *
 * Roxanne, Starfall Savant's "Whenever you tap an artifact token for mana, add one mana of any
 * type that artifact token produced" pairs with the Meteorite's "{T}: Add one mana of any color".
 * The Meteorite's mana ability pauses for the color choice, so the inline mirror resolution in
 * [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler.resolveAdditionalManaOnSourceTap]
 * (which only runs on the non-pausing path, e.g. Lavaleaper's fixed-color basic lands) is bypassed.
 * The color-choice continuation resumer calls this once the produced color is known.
 *
 * Only the **mirror** (`color = null`) case is handled here — a fixed-color
 * `AdditionalManaOnSourceTap` doesn't depend on the produced color, so it already fires on the
 * synchronous path. Returns the accumulated mana-added events for the bonus.
 */
object AdditionalManaOnSourceTapMirror {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    fun applyForResolvedTap(
        services: EngineServices,
        state: GameState,
        sourceId: EntityId?,
        tappingPlayerId: EntityId,
        producedColor: Color,
    ): ExecutionResult {
        if (sourceId == null) return ExecutionResult.success(state)
        val predicateEvaluator = services.predicateEvaluator
        var currentState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = services.cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val onSourceTap = staticAbility as? AdditionalManaOnSourceTap ?: continue
                // Only the mirror form is resolved here; fixed-color forms fire synchronously.
                if (onSourceTap.color != null) continue

                val staticController = currentState.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (!predicateEvaluator.matches(
                        currentState, currentState.projectedState, sourceId, onSourceTap.sourceFilter, filterContext
                    )) continue

                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = tappingPlayerId,
                    targets = emptyList(),
                    xValue = null
                )
                val bonusAmount = dynamicAmountEvaluator.evaluate(currentState, onSourceTap.amount, effectContext)
                if (bonusAmount <= 0) continue

                currentState = currentState.updateEntity(tappingPlayerId) { c ->
                    val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    c.with(pool.add(producedColor, bonusAmount))
                }
                events.add(
                    ManaAddedEvent(
                        playerId = tappingPlayerId,
                        sourceId = entityId,
                        sourceName = card.name,
                        white = if (producedColor == Color.WHITE) bonusAmount else 0,
                        blue = if (producedColor == Color.BLUE) bonusAmount else 0,
                        black = if (producedColor == Color.BLACK) bonusAmount else 0,
                        red = if (producedColor == Color.RED) bonusAmount else 0,
                        green = if (producedColor == Color.GREEN) bonusAmount else 0,
                        colorless = 0
                    )
                )
            }
        }
        return ExecutionResult.success(currentState, events)
    }
}
