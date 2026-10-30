package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateGlobalTriggeredAbilityEffect.
 * Creates a global triggered ability (not attached to any specific permanent) lasting for the
 * effect's [duration] — until end of turn (False Cure), permanently (Dimensional Breach,
 * planeswalker emblems), or any other duration (Season of the Bold).
 *
 * An [CreateGlobalTriggeredAbilityEffect.emblem] ability gets a synthetic emblem entity as its
 * source, built like [CreatePermanentEmblemExecutor]'s: zone-less, controlled by the player who gets
 * the emblem, and carrying a CardComponent with no types, colors or mana cost (CR 114.3), so
 * "this emblem deals 1 damage to you" is damage from a colorless source. Each emblem is its own
 * entity, so two of Chandra, Awakened Inferno's emblems trigger and show separately.
 */
class CreateGlobalTriggeredAbilityExecutor :
    EffectExecutor<CreateGlobalTriggeredAbilityEffect> {

    override val effectType: KClass<CreateGlobalTriggeredAbilityEffect> =
        CreateGlobalTriggeredAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: CreateGlobalTriggeredAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for global triggered ability")

        val sourceName = state.getEntity(sourceId)
            ?.get<CardComponent>()?.name
            ?: "Unknown"
        val (abilitySourceId, stateWithSource) =
            if (effect.emblem) mintEmblem(state, effect, context, sourceId, sourceName)
            else sourceId to state

        val global = GlobalGrantedTriggeredAbility(
            ability = effect.ability,
            controllerId = context.controllerId,
            sourceId = abilitySourceId,
            sourceName = sourceName,
            duration = effect.duration,
            descriptionOverride = effect.descriptionOverride
        )

        val newState = stateWithSource.copy(
            globalGrantedTriggeredAbilities = stateWithSource.globalGrantedTriggeredAbilities + global
        )

        return EffectResult.success(newState)
    }

    private fun mintEmblem(
        state: GameState,
        effect: CreateGlobalTriggeredAbilityEffect,
        context: EffectContext,
        creatorId: EntityId,
        sourceName: String,
    ): Pair<EntityId, GameState> {
        val description = effect.descriptionOverride ?: effect.ability.description
        val (emblemId, stateWithId) = state.newEntity()
        val container = ComponentContainer.EMPTY
            .with(ControllerComponent(context.controllerId))
            .with(
                EmblemSourceComponent(
                    sourceName = sourceName,
                    description = description,
                    creatorId = creatorId,
                    creatorEntryTimestamp = context.sourceBattlefieldTimestamp
                        ?: state.getEntity(creatorId)?.get<BattlefieldEntryTimestampComponent>()?.timestamp,
                )
            )
            .with(
                CardComponent(
                    cardDefinitionId = "$EMBLEM_DEFINITION_PREFIX$sourceName",
                    name = "$sourceName emblem",
                    manaCost = ManaCost.ZERO,
                    typeLine = TypeLine(cardTypes = emptySet()),
                    oracleText = description,
                    // The player who gets an emblem owns it (CR 114.2), so it leaves with them.
                    ownerId = context.controllerId,
                )
            )
        return emblemId to stateWithId.withEntity(emblemId, container)
    }
}
