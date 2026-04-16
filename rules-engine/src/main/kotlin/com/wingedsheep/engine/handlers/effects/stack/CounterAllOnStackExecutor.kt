package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterAllOnStackEffect
import kotlin.reflect.KClass

/**
 * Executor for [CounterAllOnStackEffect].
 *
 * Enumerates every spell and/or ability currently on the stack, filters by controller
 * (defaulting to opponents of the effect's controller), and counters each matched
 * entity. Spells go to the graveyard and abilities are removed outright.
 *
 * If the effect specifies [CounterAllOnStackEffect.storeCountAs], the countered
 * entity IDs are returned via [EffectResult.updatedCollections] so downstream
 * pipeline effects can reference the count via
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.VariableReference]
 * using `"${storeCountAs}_count"`.
 */
class CounterAllOnStackExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<CounterAllOnStackEffect> {

    override val effectType: KClass<CounterAllOnStackEffect> = CounterAllOnStackEffect::class

    override fun execute(
        state: GameState,
        effect: CounterAllOnStackEffect,
        context: EffectContext
    ): EffectResult {
        val sourceController = context.controllerId
        val opponents = state.turnOrder.filter { it != sourceController }.toSet()

        val targets = state.stack.mapNotNull { entityId ->
            classifyStackEntity(state, entityId, effect, sourceController, opponents)?.let { kind ->
                entityId to kind
            }
        }

        if (targets.isEmpty()) {
            return maybeStoreCount(EffectResult.success(state), effect, emptyList())
        }

        val resolver = StackResolver(cardRegistry = cardRegistry)
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        val countered = mutableListOf<EntityId>()

        for ((entityId, kind) in targets) {
            // Re-check presence: a prior counter in this loop doesn't affect other stack
            // objects directly, but defensive coding handles unexpected chain reactions.
            if (!currentState.stack.contains(entityId)) continue

            val result = when (kind) {
                StackEntityKind.Spell -> EffectResult.from(resolver.counterSpell(currentState, entityId))
                StackEntityKind.Ability -> EffectResult.from(resolver.counterAbility(currentState, entityId))
            }

            if (result.error != null) {
                // Skip this one — continue countering the rest
                continue
            }

            currentState = result.newState
            allEvents.addAll(result.events)
            countered.add(entityId)
        }

        return maybeStoreCount(EffectResult.success(currentState, allEvents), effect, countered)
    }

    private fun classifyStackEntity(
        state: GameState,
        entityId: EntityId,
        effect: CounterAllOnStackEffect,
        sourceController: EntityId,
        opponents: Set<EntityId>
    ): StackEntityKind? {
        val entity = state.getEntity(entityId) ?: return null

        val spellComponent = entity.get<SpellOnStackComponent>()
        if (spellComponent != null) {
            if (!effect.spells) return null
            val controller = spellComponent.casterId
            if (!controllerMatches(effect, controller, sourceController, opponents)) return null
            return StackEntityKind.Spell
        }

        val triggered = entity.get<TriggeredAbilityOnStackComponent>()
        if (triggered != null) {
            if (!effect.abilities) return null
            if (!controllerMatches(effect, triggered.controllerId, sourceController, opponents)) return null
            return StackEntityKind.Ability
        }

        val activated = entity.get<ActivatedAbilityOnStackComponent>()
        if (activated != null) {
            if (!effect.abilities) return null
            if (!controllerMatches(effect, activated.controllerId, sourceController, opponents)) return null
            return StackEntityKind.Ability
        }

        return null
    }

    private fun controllerMatches(
        effect: CounterAllOnStackEffect,
        entityController: EntityId,
        sourceController: EntityId,
        opponents: Set<EntityId>
    ): Boolean {
        if (!effect.opponentsOnly) return true
        return entityController in opponents && entityController != sourceController
    }

    private fun maybeStoreCount(
        result: EffectResult,
        effect: CounterAllOnStackEffect,
        countered: List<EntityId>
    ): EffectResult {
        val storeAs = effect.storeCountAs ?: return result
        return result.copy(
            updatedCollections = result.updatedCollections + (storeAs to countered)
        )
    }

    private enum class StackEntityKind { Spell, Ability }
}
