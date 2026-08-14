package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfEquippedCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfEquippedCreatureEffect.
 * Creates a token that's a copy of the creature equipped by the source equipment.
 *
 * The source must have an AttachedToComponent pointing to a creature.
 * The token copies the creature's CardComponent. Optionally:
 * - Removes legendary supertype if [CreateTokenCopyOfEquippedCreatureEffect.removeLegendary] is true
 * - Grants haste if [CreateTokenCopyOfEquippedCreatureEffect.grantHaste] is true
 */
class CreateTokenCopyOfEquippedCreatureExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenCopyOfEquippedCreatureEffect> {

    override val effectType: KClass<CreateTokenCopyOfEquippedCreatureEffect> =
        CreateTokenCopyOfEquippedCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfEquippedCreatureEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.success(state)

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.success(state)

        // Find the equipped creature via AttachedToComponent
        val attachedTo = sourceContainer.get<AttachedToComponent>()
            ?: return EffectResult.success(state) // Not equipped, do nothing

        val equippedId = attachedTo.targetId
        val equippedContainer = state.getEntity(equippedId)
            ?: return EffectResult.success(state)

        val equippedCard = equippedContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val controllerId = context.controllerId

        val (tokenId, stateWithId) = state.newEntity()
        var newState = stateWithId

        // Copy the equipped creature's CardComponent
        var tokenCard = equippedCard.copy(ownerId = controllerId)

        // Remove legendary if requested
        if (effect.removeLegendary) {
            val newTypeLine = tokenCard.typeLine.withoutLegendary()
            tokenCard = tokenCard.copy(typeLine = newTypeLine)
        }

        // Grant haste if requested
        if (effect.grantHaste) {
            val newKeywords = tokenCard.baseKeywords + Keyword.HASTE
            tokenCard = tokenCard.copy(baseKeywords = newKeywords)
        }

        val components = mutableListOf<Component>(
            tokenCard,
            TokenComponent,
            ControllerComponent(controllerId),
            SummoningSicknessComponent
        )

        // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
        // with the same face up as the source.
        equippedContainer.get<DoubleFacedComponent>()?.let { sourceDfc ->
            components.add(
                DoubleFacedComponent(
                    frontCardDefinitionId = sourceDfc.frontCardDefinitionId,
                    backCardDefinitionId = sourceDfc.backCardDefinitionId,
                    currentFace = sourceDfc.currentFace
                )
            )
        }

        var container = ComponentContainer.of(*components.toTypedArray())

        // Add static abilities from the card definition
        if (staticAbilityHandler != null) {
            container = staticAbilityHandler.addContinuousEffectComponent(container)
            container = staticAbilityHandler.addReplacementEffectComponent(container)
        }

        newState = newState.withEntity(tokenId, container)

        // Add to battlefield
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, controllerId, tokenId)

        // A token copy honors global "[filter] enter tapped" replacements (Authority of the
        // Consuls / Dauntless Dismantler on an opponent's token copy).
        newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
            .applyCreatedTokenEntryTap(newState, tokenId, controllerId)

        // As-enters "enters with counters" (CR 614.1c): the copied creature's own EntersWithCounters
        // (a copy of a creature that "enters with a +1/+1 counter") plus global grants from other
        // permanents (Gev, Scaled Scorch). BattlefieldEntry.place skips this, so apply it here.
        val (afterCounters, counterEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements
            .applyOnEntry(newState, tokenId, controllerId, cardRegistry)
        newState = afterCounters

        // As-enters "choose X as this enters" (CR 614.12) + granted riot (CR 702.136): pause for the
        // player's decision. Exactly one token is created, so there is no batch to resume; the entry
        // ZoneChangeEvent is omitted on a pause (the choice resumer synthesizes it after the choice
        // resolves so ETB triggers fire once). Counters ride along as carryEvents.
        val choicePlan = com.wingedsheep.engine.handlers.effects.token.TokenEntryReplacements
            .firstEntersWithChoice(newState, tokenId, cardRegistry)
        if (choicePlan != null) {
            val paused = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                .pauseForEntersWithChoice(
                    state = newState,
                    entityId = tokenId,
                    controllerId = controllerId,
                    cardComponent = tokenCard,
                    choice = choicePlan.choice,
                    fromZone = null,
                    carryEvents = counterEvents,
                    syntheticRiot = choicePlan.syntheticRiot,
                    syntheticRiotRemaining = choicePlan.syntheticRiotRemaining,
                )
            if (paused != null) return EffectResult.from(paused)
        }

        val events = counterEvents + listOf(
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = tokenCard.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = controllerId
            )
        )

        // CR 714.2b/714.3a: if the copied permanent is a Saga (e.g. an Enchantment Creature — Saga),
        // the token enters as a Saga with its on-enter lore counter. BattlefieldEntry.place skips
        // enters-with-counters setup, so apply the shared Saga-entry helper. No-op for non-Sagas.
        val (sagaState, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
            .applySagaEntryIfNeeded(newState, tokenId)

        // CR 306.5b: the equipped creature can be a planeswalker card animated by its own ability
        // (Gideon Blackblade) — the copy keeps the printed planeswalker types and loyalty (copiable
        // values, CR 707.2) but not the animation, so it needs its loyalty counters or state-based
        // actions (CR 704.5i) bin it on arrival. No-op for non-planeswalkers.
        val (loyaltyState, loyaltyEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
            .applyIntrinsicEntryCountersIfNeeded(sagaState, tokenId, controllerId, cardRegistry)

        return EffectResult.success(loyaltyState, events + sagaEvents + loyaltyEvents)
    }
}
