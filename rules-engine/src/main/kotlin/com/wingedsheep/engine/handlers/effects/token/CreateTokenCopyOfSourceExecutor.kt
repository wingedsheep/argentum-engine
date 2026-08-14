package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
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
 *
 * Because a copy has the copied card's abilities (CR 707.2), each token runs the copied card's
 * "enters with counters" (self + global — CR 614.1c) and "as this enters, choose …" (CR 614.12)
 * replacements, plus any granted riot (CR 702.136), via the shared [TokenEntryReplacements] /
 * [PermanentEntryReplacements] pipeline. A token owing an as-enters choice pauses; the rest of the
 * batch resumes through a
 * [com.wingedsheep.engine.core.CreateTokenCopyRemainingContinuation].
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
    ): EffectResult = createTokens(state, effect, context, context.controllerId, effect.count)

    /**
     * Create [count] token copies of the effect's source. Split out of [execute] so the
     * enters-with-choice batch continuation can re-enter here to create the tokens still owed after
     * one paused for a player decision.
     */
    fun createTokens(
        state: GameState,
        effect: CreateTokenCopyOfSourceEffect,
        context: EffectContext,
        controllerId: EntityId,
        count: Int,
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.success(state)

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.success(state)

        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<GameEvent>()
        val createdTokens = mutableListOf<EntityId>()

        // The "except …" clause (CR 707.9b) — "it's not legendary", "it's an artifact in addition to
        // its other types", "it's 1/1" — in the shared vocabulary, applied by the one engine-side
        // implementation. Hoisted out of the loop: the view rebuilds itself on every read.
        val exceptions = effect.copyExceptions
        // Copy the source's CardComponent, re-homing the token to the controller.
        val tokenCard = CopyExceptionApplier.apply(sourceCard, exceptions).copy(ownerId = controllerId)

        val cappedCount = com.wingedsheep.engine.core.GameLimits.cappedTokenCount(count, "source-copy tokens")
        for (index in 0 until cappedCount) {
            val (tokenId, stateWithId) = newState.newEntity()
            newState = stateWithId

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
            // with the same face up as the source. Counters
            // are intentionally not copied (handled by the absence of CountersComponent copy
            // throughout this executor).
            sourceContainer.get<DoubleFacedComponent>()?.let { sourceDfc ->
                components.add(
                    DoubleFacedComponent(
                        frontCardDefinitionId = sourceDfc.frontCardDefinitionId,
                        backCardDefinitionId = sourceDfc.backCardDefinitionId,
                        currentFace = sourceDfc.currentFace
                    )
                )
            }

            var container = ComponentContainer.of(*components.toTypedArray())

            // Add static abilities from the card definition (uses cardDefinitionId lookup)
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

            // As-enters "enters with counters" (CR 614.1c): the copied card's own EntersWithCounters
            // (a copy of a creature that "enters with a +1/+1 counter") plus global grants from other
            // permanents (Gev, Scaled Scorch). BattlefieldEntry.place skips this, so apply it here.
            val (afterCounters, counterEvents) = EntersWithReplacements.applyOnEntry(
                newState, tokenId, controllerId, cardRegistry
            )
            newState = afterCounters
            events.addAll(counterEvents)

            // As-enters "choose X as this enters" (CR 614.12) + granted riot (CR 702.136/702.136b):
            // pause for the player's decision. The entry ZoneChangeEvent is deliberately omitted on a
            // pause — the choice resumer synthesizes it after the choice resolves so ETB triggers fire
            // once. Counters already added ride along as carryEvents; the rest of the batch resumes
            // below the choice's continuation.
            val choicePlan = TokenEntryReplacements.firstEntersWithChoice(newState, tokenId, cardRegistry)
            if (choicePlan != null) {
                val remaining = cappedCount - (index + 1)
                var pausedState = newState
                if (remaining > 0) {
                    pausedState = pausedState.pushContinuation(
                        com.wingedsheep.engine.core.CreateTokenCopyRemainingContinuation(
                            decisionId = "create-token-copy-remaining-${UUID.randomUUID()}",
                            effect = effect,
                            context = context,
                            controllerId = controllerId,
                            remaining = remaining,
                        )
                    )
                }
                val paused = PermanentEntryReplacements.pauseForEntersWithChoice(
                    state = pausedState,
                    entityId = tokenId,
                    controllerId = controllerId,
                    cardComponent = tokenCard,
                    choice = choicePlan.choice,
                    fromZone = null,
                    carryEvents = events,
                    syntheticRiot = choicePlan.syntheticRiot,
                    syntheticRiotRemaining = choicePlan.syntheticRiotRemaining,
                )
                if (paused != null) return EffectResult.from(paused)
                // null → choice couldn't be presented; complete this token's entry normally.
            }

            // CR 714.2b/714.3a: a token copy of a Saga enters as a Saga and gets its on-enter lore
            // counter. BattlefieldEntry.place skips enters-with-counters setup, so apply the shared
            // Saga-entry helper (as the standard moveToZone pipeline does). No-op for non-Sagas.
            val (sagaState, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, tokenId)
            newState = sagaState
            events.addAll(sagaEvents)

            // CR 306.5b: likewise a token copy of a planeswalker enters with the copied printed
            // loyalty (a copiable value, CR 707.2), or state-based actions (CR 704.5i) bin it on
            // arrival. No-op for non-planeswalkers.
            val (loyaltyState, loyaltyEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applyIntrinsicEntryCountersIfNeeded(newState, tokenId, controllerId, cardRegistry)
            newState = loyaltyState
            events.addAll(loyaltyEvents)

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
            createdTokens.add(tokenId)

            // If exileAtStep is set, create a delayed trigger to exile this created token
            // (Stormsplitter: "exile it at the beginning of the next end step").
            val exileStep = effect.exileAtStep
            if (exileStep != null) {
                newState = newState.addDelayedTrigger(
                    DelayedTriggeredAbility(
                        id = UUID.randomUUID().toString(),
                        effect = MoveToZoneEffect(EffectTarget.SpecificEntity(tokenId), Zone.EXILE),
                        fireAtStep = exileStep,
                        sourceId = sourceId,
                        sourceName = sourceCard.name,
                        controllerId = controllerId
                    )
                )
            }
        }

        return EffectResult.success(newState, events)
    }
}
