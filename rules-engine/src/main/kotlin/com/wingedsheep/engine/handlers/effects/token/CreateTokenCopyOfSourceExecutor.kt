package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfSourceEffect.
 * Creates a token that's a copy of the source permanent (the permanent with this ability).
 *
 * The token copies the source's CardComponent (name, mana cost, types, stats, keywords, colors)
 * and uses the same cardDefinitionId so the engine picks up triggered/static abilities automatically.
 */
class CreateTokenCopyOfSourceExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenCopyOfSourceEffect> {

    override val effectType: KClass<CreateTokenCopyOfSourceEffect> = CreateTokenCopyOfSourceEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.success(state)

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.success(state)

        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val controllerId = context.controllerId

        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(effect.count) {
            val tokenId = EntityId.generate()
            createdTokens.add(tokenId)

            // Copy the source's CardComponent, setting the token's owner to the controller
            // Apply P/T overrides if specified (e.g., Offspring creates 1/1 copies)
            val op = effect.overridePower
            val ot = effect.overrideToughness
            val overrideStats = if (op != null && ot != null) {
                CreatureStats(op, ot)
            } else null
            val tokenCard = sourceCard.copy(
                ownerId = controllerId,
                baseStats = overrideStats ?: sourceCard.baseStats
            )

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )

            var container = ComponentContainer.of(*components.toTypedArray())

            // Add static abilities from the card definition (uses cardDefinitionId lookup)
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        // Apply "enters with counters" replacement effects from other battlefield permanents
        // (e.g., Gev, Scaled Scorch granting +1/+1 counters to Offspring token copies).
        val counterEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        for (tokenId in createdTokens) {
            val (nextState, events) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
                newState, tokenId, controllerId
            )
            newState = nextState
            counterEvents.addAll(events)
        }

        // If exileAtStep is set, create delayed triggers to exile each created token
        val exileStep = effect.exileAtStep
        if (exileStep != null) {
            val sourceName = sourceCard.name
            for (tokenId in createdTokens) {
                val delayedTrigger = DelayedTriggeredAbility(
                    id = UUID.randomUUID().toString(),
                    effect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE),
                    fireAtStep = exileStep,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId
                )
                newState = newState.addDelayedTrigger(delayedTrigger)
            }
        }

        val events = createdTokens.map { tokenId ->
            val entity = newState.getEntity(tokenId)!!
            val card = entity.get<CardComponent>()!!
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = card.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = controllerId
            )
        }

        return EffectResult.success(newState, events + counterEvents)
    }
}
