package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.DrawReplacementActivationContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for DrawCardsEffect.
 * "Draw X cards" or "Target player draws X cards"
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry? = null,
    private val effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<DrawCardsEffect> {

    override val effectType: KClass<DrawCardsEffect> = DrawCardsEffect::class

    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid player for draw")

        val count = amountEvaluator.evaluate(state, effect.count, context)
        return executeDraws(state, playerId, count)
    }

    /**
     * Execute a sequence of draws, checking for replacement shields on each draw.
     * This is also called from ContinuationHandler when resuming remaining draws
     * after a replacement effect completes.
     */
    fun executeDraws(
        state: GameState,
        playerId: EntityId,
        count: Int
    ): ExecutionResult {
        var newState = state
        val drawnCards = mutableListOf<EntityId>()
        val events = mutableListOf<GameEvent>()

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)

        val shieldConsumer = effectExecutor?.let { DrawReplacementShieldConsumer(it) }

        for (i in 0 until count) {
            // Check for unified draw replacement shields (Words of Worship/Wind/War/Waste/Wilding)
            if (shieldConsumer != null) {
                val shieldResult = shieldConsumer.consumeShield(
                    state = newState,
                    playerId = playerId,
                    remainingDraws = count - i - 1,
                    drawnCardsSoFar = drawnCards.toList(),
                    eventsSoFar = events.toList(),
                    isDrawStep = false
                )
                if (shieldResult != null) {
                    when (shieldResult) {
                        is DrawReplacementShieldConsumer.ConsumeResult.Paused -> {
                            // Emit CardsDrawnEvent for cards drawn before this shield was hit
                            val allEvents = events.toMutableList()
                            if (drawnCards.isNotEmpty()) {
                                val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                                allEvents.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames))
                            }
                            return ExecutionResult.paused(
                                shieldResult.result.state,
                                shieldResult.result.pendingDecision!!,
                                allEvents + shieldResult.result.events
                            )
                        }
                        is DrawReplacementShieldConsumer.ConsumeResult.Synchronous -> {
                            newState = shieldResult.state
                            events.addAll(shieldResult.events)
                            // Remaining draws are handled: if there were remaining draws and the pipeline
                            // completed synchronously, the consumer already popped the continuation and
                            // we continue the loop to process remaining draws normally
                            continue
                        }
                    }
                }
            }

            // No shield exists - check if player wants to activate a promptOnDraw ability
            val promptResult = checkPromptOnDraw(
                newState, playerId, count - i, drawnCards.toList()
            )
            if (promptResult != null) {
                // Emit CardsDrawnEvent for cards drawn before this prompt
                if (drawnCards.isNotEmpty()) {
                    val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                    val allEvents = mutableListOf<GameEvent>(
                        CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames)
                    )
                    allEvents.addAll(events)
                    allEvents.addAll(promptResult.events)
                    return ExecutionResult.paused(
                        promptResult.state,
                        promptResult.pendingDecision!!,
                        allEvents
                    )
                }
                return promptResult
            }

            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition (Rule 704.5c)
                newState = newState.updateEntity(playerId) { container ->
                    container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                }
                events.add(DrawFailedEvent(playerId, "Empty library"))
                if (drawnCards.isNotEmpty()) {
                    val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                    events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames))
                }
                return ExecutionResult.success(newState, events)
            }

            // Draw from top of library (first card)
            val cardId = library.first()
            drawnCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)

            // Track draw count and check for reveal-first-draw effects
            val drawCountBefore = newState.getEntity(playerId)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
            newState = newState.updateEntity(playerId) { container ->
                container.with(CardsDrawnThisTurnComponent(count = drawCountBefore + 1))
            }
            if (drawCountBefore == 0) {
                val revealEvent = checkRevealFirstDraw(newState, playerId, cardId)
                if (revealEvent != null) {
                    events.add(revealEvent)
                }
            }
        }

        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(0, CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Check if a drawn card should be revealed due to RevealFirstDrawEachTurn static abilities.
     * Only called when this is the first draw of the turn.
     */
    private fun checkRevealFirstDraw(
        state: GameState,
        playerId: EntityId,
        drawnCardId: EntityId
    ): CardRevealedFromDrawEvent? {
        if (cardRegistry == null) return null

        val projected = stateProjector.project(state)
        val hasRevealAbility = projected.getBattlefieldControlledBy(playerId).any { permanentId ->
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: return@any false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@any false
            cardDef.script.staticAbilities.any { it is com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn }
        }

        if (!hasRevealAbility) return null

        val drawnCard = state.getEntity(drawnCardId)?.get<CardComponent>() ?: return null
        return CardRevealedFromDrawEvent(
            playerId = playerId,
            cardEntityId = drawnCardId,
            cardName = drawnCard.name,
            isCreature = drawnCard.typeLine.isCreature
        )
    }

    /**
     * Check if a player has a "prompt on draw" activated ability that they can afford.
     * If so, present a mana source selection decision and pause.
     * Returns null if no prompt is needed (no cardRegistry, no ability, can't afford).
     *
     * This is the same logic as TurnManager.checkPromptOnDraw() but for spell/ability draws.
     */
    internal fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        remainingDrawCount: Int,
        drawnCardsSoFar: List<EntityId>,
        declinedSourceIds: List<EntityId> = emptyList()
    ): ExecutionResult? {
        if (cardRegistry == null) return null

        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(playerId)

        for (permanentId in controlledPermanents) {
            if (permanentId in declinedSourceIds) continue
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.promptOnDraw) continue

                val manaCost = when (val cost = ability.cost) {
                    is com.wingedsheep.sdk.scripting.AbilityCost.Mana -> cost.cost
                    is com.wingedsheep.sdk.scripting.AbilityCost.Composite -> {
                        cost.costs.filterIsInstance<com.wingedsheep.sdk.scripting.AbilityCost.Mana>()
                            .firstOrNull()?.cost
                    }
                    else -> null
                } ?: continue

                val manaSolver = ManaSolver(cardRegistry, stateProjector)
                if (!manaSolver.canPay(state, playerId, manaCost)) continue

                val sources = manaSolver.findAvailableManaSources(state, playerId)
                val sourceOptions = sources.map { source ->
                    ManaSourceOption(
                        entityId = source.entityId,
                        name = source.name,
                        producesColors = source.producesColors,
                        producesColorless = source.producesColorless
                    )
                }

                val solution = manaSolver.solve(state, playerId, manaCost)
                val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

                val decisionId = java.util.UUID.randomUUID().toString()
                val decision = SelectManaSourcesDecision(
                    id = decisionId,
                    playerId = playerId,
                    prompt = "Pay ${manaCost} to activate ${card.name}?",
                    context = DecisionContext(
                        sourceId = permanentId,
                        sourceName = card.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    availableSources = sourceOptions,
                    requiredCost = manaCost.toString(),
                    autoPaySuggestion = autoPaySuggestion,
                    canDecline = true
                )

                val continuation = DrawReplacementActivationContinuation(
                    decisionId = decisionId,
                    drawingPlayerId = playerId,
                    sourceId = permanentId,
                    sourceName = card.name,
                    abilityEffect = ability.effect,
                    manaCost = manaCost.toString(),
                    drawCount = remainingDrawCount,
                    isDrawStep = false,
                    drawnCardsSoFar = drawnCardsSoFar,
                    targetRequirements = ability.targetRequirements,
                    declinedSourceIds = declinedSourceIds
                )

                val stateWithDecision = state.withPendingDecision(decision)
                val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

                return ExecutionResult.paused(
                    stateWithContinuation,
                    decision,
                    listOf(
                        DecisionRequestedEvent(
                            decisionId = decisionId,
                            playerId = playerId,
                            decisionType = "SELECT_MANA_SOURCES",
                            prompt = decision.prompt
                        )
                    )
                )
            }
        }

        return null
    }
}
