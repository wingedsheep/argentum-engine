package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Runs the non-mana side effects of an activated mana ability when a source is
 * auto-tapped to pay a cost.
 *
 * Auto-tap fast paths (e.g. spell casting, cycling, combat tax) bypass the normal
 * activated-ability flow: they tap the source and credit its produced mana directly
 * to the payment, skipping the [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler].
 * For most lands that's correct — the ability is just "{T}: Add {X}" — but pain
 * lands like Adarkar Wastes carry damage as part of the ability's effect
 * (`{T}: Add {W} or {U}. This land deals 1 damage to you.`). Without this helper
 * that damage is silently lost.
 *
 * The helper finds the activated mana ability that matches the produced color and
 * executes everything in its effect chain *except* the mana-producing pieces
 * (which the auto-tap path has already accounted for).
 */
class ManaAbilitySideEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) {

    /**
     * Run side effects for a single auto-tapped source.
     *
     * @param state Current game state (already mutated by the caller to reflect tap).
     * @param sourceId Permanent that was tapped.
     * @param producedColor Color the source produced for the payment, or null for colorless.
     * @param controllerId Player who controls the source / paid the cost.
     * @param opponentId The opposing player (for `Player.Opponent`-shaped targets).
     */
    /**
     * Tap every source in [solution] (emitting [TappedEvent]) and run any
     * non-mana side effects of the matching mana ability. This is the
     * one-shot form for callers that already have a [ManaSolution] from
     * [ManaSolver]; the produced mana itself is still consumed separately
     * via [ManaSolution.manaProduced].
     */
    fun tapSourcesWithSideEffects(
        state: GameState,
        solution: ManaSolution,
        controllerId: EntityId
    ): Pair<GameState, List<GameEvent>> {
        var currentState = state
        val events = mutableListOf<GameEvent>()
        val opponentId = currentState.turnOrder.firstOrNull { it != controllerId }
        for (source in solution.sources) {
            currentState = currentState.updateEntity(source.entityId) { c ->
                c.with(TappedComponent)
            }
            events.add(TappedEvent(source.entityId, source.name))

            val production = solution.manaProduced[source.entityId]
            val (after, sideEvents) = runSideEffects(
                state = currentState,
                sourceId = source.entityId,
                producedColor = production?.color,
                controllerId = controllerId,
                opponentId = opponentId
            )
            currentState = after
            events.addAll(sideEvents)
        }
        return currentState to events
    }

    fun runSideEffects(
        state: GameState,
        sourceId: EntityId,
        producedColor: Color?,
        controllerId: EntityId,
        opponentId: EntityId?
    ): Pair<GameState, List<GameEvent>> {
        val card = state.getEntity(sourceId)?.get<CardComponent>()
            ?: return state to emptyList()
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return state to emptyList()

        val matchingAbility = cardDef.script.activatedAbilities
            .filter { it.isManaAbility }
            .firstOrNull { abilityProducesColor(it, producedColor) }
            ?: return state to emptyList()

        val sideEffects = nonManaSubEffects(matchingAbility.effect)
        if (sideEffects.isEmpty()) return state to emptyList()

        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            opponentId = opponentId
        )

        var currentState = state
        val events = mutableListOf<GameEvent>()
        for (sub in sideEffects) {
            val result = effectExecutor(currentState, sub, context)
            currentState = result.state
            events.addAll(result.events)
            // Side effects from auto-tap should never pause for player decisions
            // (mana abilities don't use the stack), so we treat any pause as a
            // no-op and continue. In practice every printed mana-ability side
            // effect is fully resolved with controller info alone.
        }
        return currentState to events
    }

    private fun abilityProducesColor(ability: ActivatedAbility, color: Color?): Boolean =
        manaSubEffects(ability.effect).any { effect -> effectProduces(effect, color) }

    private fun effectProduces(effect: Effect, color: Color?): Boolean = when (effect) {
        is AddManaEffect -> effect.color == color
        is AddColorlessManaEffect -> color == null
        is AddAnyColorManaEffect,
        is AddAnyColorManaSpendOnChosenTypeEffect,
        is AddManaOfColorAmongEffect,
        is AddManaOfChosenColorEffect -> color != null  // any non-null color
        is AddDynamicManaEffect -> color != null && color in effect.allowedColors
        else -> false
    }

    private fun manaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filter { isManaEffect(it) }
        else -> if (isManaEffect(effect)) listOf(effect) else emptyList()
    }

    private fun nonManaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filterNot { isManaEffect(it) }
        else -> emptyList()  // single-effect mana abilities have nothing extra to run
    }

    private fun isManaEffect(effect: Effect): Boolean = effect is AddManaEffect ||
        effect is AddColorlessManaEffect ||
        effect is AddAnyColorManaEffect ||
        effect is AddAnyColorManaSpendOnChosenTypeEffect ||
        effect is AddManaOfColorAmongEffect ||
        effect is AddManaOfChosenColorEffect ||
        effect is AddDynamicManaEffect

    companion object {
        /**
         * Stand-in instance for default-constructed contexts (e.g. a [CombatManager]
         * built without an [EngineServices] wiring). Side effects are dropped on the
         * floor — production code must use the executor wired by [EngineServices].
         */
        fun noOp(cardRegistry: CardRegistry): ManaAbilitySideEffectExecutor =
            ManaAbilitySideEffectExecutor(cardRegistry) { state, _, _ ->
                EffectResult.success(state)
            }
    }
}
