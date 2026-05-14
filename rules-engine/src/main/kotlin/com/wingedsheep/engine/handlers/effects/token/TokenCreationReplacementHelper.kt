package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TokenReplacementOfferedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DoubleTokenCreation
import com.wingedsheep.sdk.scripting.ModifyTokenCount
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithEquippedCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID

/**
 * Checks for token creation replacement effects (e.g., Mirrormind Crown)
 * before tokens are created. If a replacement applies, returns a paused
 * EffectResult with a yes/no decision; otherwise returns null.
 */
object TokenCreationReplacementHelper {

    /**
     * Apply token-count replacement effects controlled by [tokenControllerId]:
     * - [DoubleTokenCreation] (Anointed Procession / Exalted Sunborn) multiplies
     *   the count by 2 per source; stacks multiplicatively when several are in play.
     * - [ModifyTokenCount] shifts the count by a fixed amount per source (clamped at zero).
     *
     * Both replacements live on permanents with [ReplacementEffectSourceComponent].
     * Token-doubling cards are uncommon enough that "the affected player chooses
     * the order" only matters when both modifier kinds are present, which is rare
     * — we apply doublers first, then modifiers, which is the same order any
     * sensible player would pick.
     */
    fun applyCountReplacements(
        state: GameState,
        tokenControllerId: EntityId,
        baseCount: Int
    ): Int {
        if (baseCount <= 0) return baseCount

        var doublings = 0
        var modifier = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val sourceController = container.get<ControllerComponent>()?.playerId ?: continue
            if (sourceController != tokenControllerId) continue
            val repl = container.get<ReplacementEffectSourceComponent>() ?: continue
            for (effect in repl.replacementEffects) {
                when (effect) {
                    is DoubleTokenCreation -> doublings += 1
                    is ModifyTokenCount -> modifier += effect.modifier
                    else -> {}
                }
            }
        }

        var count = baseCount
        repeat(doublings) { count *= 2 }
        count += modifier
        return count.coerceAtLeast(0)
    }


    /**
     * Check if any equipment controlled by the token creator has a
     * ReplaceTokenCreationWithEquippedCopy replacement effect that applies.
     *
     * @return A paused EffectResult if a replacement decision is needed, or null
     */
    fun checkReplacement(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        tokenCount: Int,
        tokenControllerId: EntityId,
        cardRegistry: CardRegistry? = null,
        staticAbilityHandler: StaticAbilityHandler? = null
    ): EffectResult? {
        if (tokenCount <= 0) return null

        val controllerId = tokenControllerId

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId ?: continue
            if (entityController != controllerId) continue

            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue

            for (re in replacementComponent.replacementEffects) {
                if (re !is ReplaceTokenCreationWithEquippedCopy) continue

                // Check once-per-turn
                if (re.oncePerTurn && container.has<TokenReplacementOfferedThisTurnComponent>()) continue

                // Check attached to creature
                val attachedTo = container.get<AttachedToComponent>() ?: continue
                val equippedContainer = state.getEntity(attachedTo.targetId) ?: continue
                val equippedCard = equippedContainer.get<CardComponent>() ?: continue
                if (!equippedCard.typeLine.isCreature) continue

                val cardName = container.get<CardComponent>()?.name ?: "Equipment"

                // Mark as offered this turn (prevents re-offering on decline)
                var newState = state.withEntity(entityId, container.with(TokenReplacementOfferedThisTurnComponent))

                if (re.optional) {
                    val decisionId = UUID.randomUUID().toString()
                    val prompt = "Use $cardName? Create ${if (tokenCount == 1) "a token that's a copy" else "$tokenCount tokens that are copies"} of ${equippedCard.name} instead?"

                    val decision = YesNoDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = prompt,
                        context = DecisionContext(
                            sourceId = entityId,
                            sourceName = cardName,
                            phase = DecisionPhase.RESOLUTION
                        )
                    )

                    val continuation = TokenCreationReplacementContinuation(
                        decisionId = decisionId,
                        equipmentId = entityId,
                        equippedCreatureId = attachedTo.targetId,
                        originalEffect = effect,
                        tokenCount = tokenCount,
                        effectContext = context
                    )

                    newState = newState.withPendingDecision(decision)
                    newState = newState.pushContinuation(continuation)

                    return EffectResult.paused(
                        newState,
                        decision,
                        listOf(
                            DecisionRequestedEvent(
                                decisionId = decisionId,
                                playerId = controllerId,
                                decisionType = "YES_NO",
                                prompt = prompt
                            )
                        )
                    )
                } else {
                    // Mandatory replacement — create copies directly
                    return createEquippedCreatureCopies(
                        newState, attachedTo.targetId, controllerId, tokenCount,
                        cardRegistry, staticAbilityHandler
                    )
                }
            }
        }
        return null
    }

    /**
     * Create N token copies of the equipped creature.
     *
     * Per the printed rulings, Mirrormind Crown's tokens copy exactly what was printed
     * on the equipped creature. That includes printed "enters with N counters" replacement
     * effects (e.g., Burdened Stoneback's "this creature enters with two -1/-1 counters"),
     * which apply to the token via [EntersWithCountersHelper.applyEntersWithCounters].
     */
    fun createEquippedCreatureCopies(
        state: GameState,
        equippedCreatureId: EntityId,
        controllerId: EntityId,
        count: Int,
        cardRegistry: CardRegistry? = null,
        staticAbilityHandler: StaticAbilityHandler? = null
    ): EffectResult {
        val equippedContainer = state.getEntity(equippedCreatureId)
            ?: return EffectResult.success(state)

        val equippedCard = equippedContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        repeat(count) {
            val tokenId = EntityId.generate()
            val tokenCard = equippedCard.copy(ownerId = controllerId)

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )

            var container = ComponentContainer.of(*components.toTypedArray())
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }
            newState = newState.withEntity(tokenId, container)

            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)

            // Apply the equipped creature's printed enters-with-counters replacement effects
            // (and any global ones from other permanents).
            if (cardRegistry != null) {
                val (afterCounters, counterEvents) = EntersWithCountersHelper.applyEntersWithCounters(
                    newState, tokenId, controllerId, cardRegistry
                )
                newState = afterCounters
                events.addAll(counterEvents)
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        }

        return EffectResult.success(newState, events)
    }
}
