package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.CycleDrawContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PreventCycling
import kotlin.reflect.KClass

/**
 * Handler for the CycleCard action.
 *
 * Cycling allows a player to pay a cost, discard the card,
 * and draw a new card. It's an activated ability from hand.
 */
class CycleCardHandler(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)?,
    private val replacementProcessor: ReplacementEffectProcessor = ReplacementEffectProcessor()
) : ActionHandler<CycleCard> {
    override val actionType: KClass<CycleCard> = CycleCard::class

    override fun validate(state: GameState, action: CycleCard): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check if cycling is prevented by any permanent on the battlefield (e.g., Stabilizer)
        if (isCyclingPrevented(state)) {
            return "Cycling is prevented"
        }

        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        val handZone = ZoneKey(action.playerId, Zone.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Card is not in your hand"
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        val cyclingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cycling>()
            .firstOrNull { it.searchFilter == null }
            ?: return "This card doesn't have cycling"

        // The action is client-supplied: a negative X would underflow the cost substitution.
        if (action.xValue != null && action.xValue < 0) {
            return "X can't be negative"
        }

        // Affordability is judged against X=0 when the player hasn't announced X yet — the
        // handler raises the choice, and X=0 is always a legal announcement (CR 107.3a).
        val effectiveCost = cyclingAbility.cost.withXAs(action.xValue ?: 0)

        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
            }
        } else if (!manaSolver.canPay(state, action.playerId, effectiveCost)) {
            return "Not enough mana to cycle this card"
        }

        return null
    }

    override fun execute(state: GameState, action: CycleCard): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        val cyclingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cycling>()
            .firstOrNull { it.searchFilter == null }
            ?: return ExecutionResult.error(state, "This card doesn't have cycling")

        // ---------------------------------------------------------------------
        // {X} cycling cost — announce X (CR 107.3a).
        //
        // Cycling is an activated ability (CR 702.29a), so its `{X}` is chosen as the ability is
        // activated. The legal-actions submission path sends a bare CycleCard with no xValue; pause
        // for the choice and re-enter with it bound. The engine-direct path (xValue pre-filled,
        // as scenario tests and the AI use) skips this. Mirrors the mana-X pause in
        // ActivateAbilityHandler.
        // ---------------------------------------------------------------------
        if (cyclingAbility.cost.hasX && action.xValue == null) {
            val fixedMana = cyclingAbility.cost.withXAs(0).cmc
            val maxX = ((manaSolver.getAvailableManaCount(state, action.playerId) - fixedMana) /
                cyclingAbility.cost.xCount.coerceAtLeast(1)).coerceAtLeast(0)
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = com.wingedsheep.engine.core.ChooseNumberDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = "Choose X for cycling ${cardComponent.name} (0-$maxX)",
                context = com.wingedsheep.engine.core.DecisionContext(
                    sourceId = action.cardId,
                    sourceName = cardComponent.name,
                    phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                ),
                minValue = 0,
                maxValue = maxX
            )
            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(
                    com.wingedsheep.engine.core.CycleCardChooseXContinuation(
                        decisionId = decisionId,
                        action = action
                    )
                )
            val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_NUMBER",
                prompt = decision.prompt
            )
            return ExecutionResult.paused(pausedState, decision, listOf(event))
        }

        // X is settled from here on. Substituting it into the cost leaves an ordinary X-free cost,
        // so every payment path below (pool, auto-tap, explicit taps) works unchanged.
        val announcedX = action.xValue?.takeIf { cyclingAbility.cost.hasX }
        val cyclingCost = cyclingAbility.cost.withXAs(announcedX ?: 0)

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val ownerId = cardComponent.ownerId ?: action.playerId

        // Pay the cycling cost - use floating mana first, then tap lands
        val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        val partialResult = pool.payPartial(cyclingCost)
        val poolAfterPayment = partialResult.newPool
        val remainingCost = partialResult.remainingCost
        val manaSpentFromPool = partialResult.manaSpent

        var whiteSpent = manaSpentFromPool.white
        var blueSpent = manaSpentFromPool.blue
        var blackSpent = manaSpentFromPool.black
        var redSpent = manaSpentFromPool.red
        var greenSpent = manaSpentFromPool.green
        var colorlessSpent = manaSpentFromPool.colorless

        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(
                ManaPoolComponent(
                    white = poolAfterPayment.white,
                    blue = poolAfterPayment.blue,
                    black = poolAfterPayment.black,
                    red = poolAfterPayment.red,
                    green = poolAfterPayment.green,
                    colorless = poolAfterPayment.colorless
                )
            )
        }

        // Tap lands for remaining cost
        if (!remainingCost.isEmpty()) {
            if (action.paymentStrategy is PaymentStrategy.Explicit) {
                // Tap specified sources explicitly
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val (tappedState, tapEvent) = tap(currentState, sourceId)
                    currentState = tappedState
                    tapEvent?.let(events::add)
                }
            } else {
                val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0)
                    ?: return ExecutionResult.error(state, "Not enough mana to cycle")

                val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
                    .tapSourcesWithSideEffects(currentState, solution, action.playerId)
                currentState = stateAfterTaps
                events.addAll(tapEvents)

                for ((_, production) in solution.manaProduced) {
                    when (production.color) {
                        Color.WHITE -> whiteSpent++
                        Color.BLUE -> blueSpent++
                        Color.BLACK -> blackSpent++
                        Color.RED -> redSpent++
                        Color.GREEN -> greenSpent++
                        null -> colorlessSpent += production.colorless
                    }
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = action.playerId,
                reason = "Cycle ${cardComponent.name}",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        // Discard the card to pay the cycling cost (CR 702.29a: "Cycling [cost]" means
        // "[Cost], Discard this card: Draw a card."), through the shared discard path so that
        // "whenever you discard" payoffs see it (Magmakin Artillerist) *and* a card-intrinsic
        // discard replacement applies — cycling a madness card exiles it and offers the madness
        // cast (CR 702.35a), which is the classic Fiery Temper line. The discard event and the
        // zone change land before CardCycledEvent, so a card that triggers on both (CR 702.29d)
        // sees them in the order they happened.
        val discardResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .discardCards(currentState, action.playerId, listOf(action.cardId), asCyclingCost = true)
        currentState = discardResult.state
        events.addAll(discardResult.events)

        // Emit cycling event (for cycling triggers like Astral Slide)
        events.add(CardCycledEvent(action.playerId, action.cardId, cardComponent.name, announcedX))

        currentState = currentState.tick()

        // Detect and process triggers from discard + cycling events before drawing,
        // since the draw may pause for replacement effects (e.g., Words cycle)
        val preTriggers = triggerDetector.detectTriggers(currentState, events)
        if (preTriggers.isNotEmpty()) {
            // Push draw continuation BEFORE processing triggers, so it ends up below
            // any trigger continuations on the stack. After all triggers resolve,
            // checkForMoreContinuations() will find this and execute the draw.
            val stateWithDrawContinuation = currentState.pushContinuation(
                CycleDrawContinuation(playerId = action.playerId)
            )
            val triggerResult = triggerProcessor.processTriggers(stateWithDrawContinuation, preTriggers)

            if (triggerResult.isPaused) {
                // triggersAlreadyProcessed: the cycling events above have been through
                // detectTriggers here. Without the flag, SubmitDecisionHandler re-scans this
                // result's events when the cycle was resumed from a decision (an {X} cycling
                // cost's ChooseNumber) and queues the cycling trigger a second time.
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                ).copy(triggersAlreadyProcessed = true)
            }

            // Triggers resolved synchronously — pop the draw continuation and draw inline
            val (_, stateAfterPop) = triggerResult.newState.popContinuation()
            currentState = stateAfterPop
            events.addAll(triggerResult.events)
        }

        // Draw a card using DrawCardsExecutor (checks replacement shields).
        // Cycling is "Discard this card: Draw a card" (CR 702.29a). The announcement-site
        // modifier (CR 121.2a) fires via executeDraws → checkDrawAmount before the per-card loop.
        val drawExecutor = DrawCardsExecutor(
            cardRegistry = cardRegistry,
            effectExecutor = effectExecutor,
            replacementProcessor = replacementProcessor
        )
        val drawResult = drawExecutor.executeDraws(currentState, action.playerId, 1)
        if (drawResult.isPaused) {
            return ExecutionResult.paused(
                drawResult.state,
                drawResult.pendingDecision!!,
                events + drawResult.events
            ).copy(triggersAlreadyProcessed = true)
        }
        currentState = drawResult.newState
        events.addAll(drawResult.events)

        // Cycling doesn't change priority
        return ExecutionResult.success(currentState, events)
            .copy(triggersAlreadyProcessed = true)
    }

    private fun isCyclingPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventCycling }) {
                return true
            }
        }
        return false
    }

    companion object {
        fun create(services: EngineServices): CycleCardHandler {
            return CycleCardHandler(
                services.cardRegistry,
                services.manaSolver,
                services.triggerDetector,
                services.triggerProcessor,
                services.manaAbilitySideEffectExecutor,
                services.effectExecutorRegistry::execute,
                services.replacementEffectProcessor
            )
        }
    }
}
