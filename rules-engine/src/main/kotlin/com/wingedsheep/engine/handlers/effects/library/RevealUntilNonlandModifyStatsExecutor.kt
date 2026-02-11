package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.RevealUntilNonlandModifyStatsEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for RevealUntilNonlandModifyStatsEffect.
 *
 * "Reveal cards from the top of your library until you reveal a nonland card.
 * This creature gets +X/+0 until end of turn, where X is that card's mana value.
 * Put the revealed cards on the bottom of your library in any order."
 *
 * Logic:
 * 1. Iterate from top of library until a nonland card is found
 * 2. Collect all revealed cards (lands + the nonland)
 * 3. Emit CardsRevealedEvent
 * 4. Create a floating +X/+0 effect on the source creature where X is the nonland's mana value
 * 5. Remove all revealed cards from the library
 * 6. If 1 card: put on bottom directly. If multiple: ask player to order for bottom.
 *
 * Edge cases:
 * - Empty library: no buff, nothing to do
 * - All lands (no nonland found): reveal all, no buff, put all on bottom
 */
class RevealUntilNonlandModifyStatsExecutor : EffectExecutor<RevealUntilNonlandModifyStatsEffect> {

    override val effectType: KClass<RevealUntilNonlandModifyStatsEffect> =
        RevealUntilNonlandModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: RevealUntilNonlandModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // Empty library: nothing to reveal
        if (library.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Reveal cards from the top until we find a nonland card
        val revealedCards = mutableListOf<EntityId>()
        var nonlandCard: EntityId? = null

        for (cardId in library) {
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            revealedCards.add(cardId)

            if (cardComponent != null && !cardComponent.isLand) {
                nonlandCard = cardId
                break
            }
        }

        // Build events
        val events = mutableListOf<GameEvent>()

        // Emit reveal event
        val cardNames = revealedCards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        }
        val imageUris = revealedCards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.imageUri
        }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        events.add(
            CardsRevealedEvent(
                revealingPlayerId = controllerId,
                cardIds = revealedCards.toList(),
                cardNames = cardNames,
                imageUris = imageUris,
                source = sourceName
            )
        )

        // Buff the source creature if we found a nonland card
        var currentState = state
        if (nonlandCard != null && context.sourceId != null) {
            val manaValue = currentState.getEntity(nonlandCard)?.get<CardComponent>()?.manaValue ?: 0
            if (manaValue > 0) {
                val floatingEffect = ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.POWER_TOUGHNESS,
                        sublayer = Sublayer.MODIFICATIONS,
                        modification = SerializableModification.ModifyPowerToughness(
                            powerMod = manaValue, toughnessMod = 0
                        ),
                        affectedEntities = setOf(context.sourceId)
                    ),
                    duration = Duration.EndOfTurn,
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    timestamp = System.currentTimeMillis()
                )
                currentState = currentState.copy(floatingEffects = currentState.floatingEffects + floatingEffect)

                val targetName = currentState.getEntity(context.sourceId)?.get<CardComponent>()?.name ?: "Unknown"
                events.add(
                    StatsModifiedEvent(
                        targetId = context.sourceId,
                        targetName = targetName,
                        powerChange = manaValue,
                        toughnessChange = 0,
                        sourceName = sourceName ?: "Unknown"
                    )
                )
            }
        }

        // Remove revealed cards from library
        val revealedSet = revealedCards.toSet()
        val remainingLibrary = currentState.getZone(libraryZone).filter { it !in revealedSet }
        currentState = currentState.copy(
            zones = currentState.zones + (libraryZone to remainingLibrary)
        )

        // Put revealed cards on bottom of library
        if (revealedCards.size <= 1) {
            // 0 or 1 card: just append to bottom, no ordering needed
            if (revealedCards.isNotEmpty()) {
                val newLibrary = remainingLibrary + revealedCards
                currentState = currentState.copy(
                    zones = currentState.zones + (libraryZone to newLibrary)
                )
            }
            return ExecutionResult.success(currentState, events)
        }

        // Multiple cards: ask player to order them for bottom of library
        val decisionId = UUID.randomUUID().toString()

        val cardInfoMap = revealedCards.associateWith { cardId ->
            val container = currentState.getEntity(cardId)
            val cardComp = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComp?.name ?: "Unknown",
                manaCost = cardComp?.manaCost?.toString() ?: "",
                typeLine = cardComp?.typeLine?.toString() ?: "",
                imageUri = cardComp?.imageUri
            )
        }

        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Put the revealed cards on the bottom of your library in any order.",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = revealedCards.toList(),
            cardInfo = cardInfoMap
        )

        val continuation = PutOnBottomOfLibraryContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val stateWithDecision = currentState.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        events.add(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = controllerId,
                decisionType = "REORDER_LIBRARY",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(stateWithContinuation, decision, events)
    }
}
