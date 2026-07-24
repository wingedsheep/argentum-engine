package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PayOrSufferEffect.
 *
 * Generic "unless" effect handler for punisher mechanics:
 * "Do [suffer], unless you [cost]."
 *
 * Examples:
 * - Thundering Wurm: "When ~ enters the battlefield, sacrifice it unless you discard a land card."
 * - Primeval Force: "When ~ enters the battlefield, sacrifice it unless you sacrifice three Forests."
 *
 * The player is presented with a selection of valid options to pay the cost.
 * If they select exactly the required count, the cost is paid and the suffer effect is avoided.
 * If they select 0 (or don't have enough), the suffer effect is executed.
 */
class PayOrSufferExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val executeEffect: ((GameState, Effect, EffectContext) -> EffectResult)? = null
) : EffectExecutor<PayOrSufferEffect> {

    override val effectType: KClass<PayOrSufferEffect> = PayOrSufferEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val costPaymentService by lazy { CostPaymentService(EngineServices(cardRegistry)) }

    override fun execute(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for pay or suffer effect")

        // Resolve who must pay — defaults to controller but can be the opponent (e.g., "target opponent loses 3 life unless they sacrifice")
        val payingPlayerId = context.resolvePlayerTarget(effect.player)
            ?: context.controllerId

        // Find source card info
        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Source has no card component")

        return when (val cost = effect.cost) {
            is PayCost.Choice -> handleChoiceCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            // "...pay its mana cost": pay the affected permanent's own mana cost. A permanent
            // with no mana cost (a land, a token) has an empty ManaCost, which resolves to {0}
            // and is trivially payable, so such a permanent is always kept.
            is PayCost.OwnManaCost ->
                handleManaCost(state, effect, context, CostAtom.Mana(sourceCard.manaCost), sourceId, sourceCard.name, payingPlayerId)
            is PayCost.Atom -> when (val atom = cost.atom) {
                is CostAtom.Discard -> handleDiscardCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.Sacrifice -> handleSacrificeCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.PayLife -> handlePayLifeCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.Mana -> handleManaCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.ExileFrom -> handleExileCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.TapPermanents -> handleTapCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
                is CostAtom.ReturnToHand -> EffectResult.error(state, "ReturnToHand payment for PayOrSuffer not yet implemented")
                is CostAtom.RevealFromHand -> EffectResult.error(state, "RevealCard payment for PayOrSuffer not yet implemented")
                is CostAtom.PutCountersOnSelf -> EffectResult.error(state, "PutCountersOnSelf is an activated-ability cost, not a PayOrSuffer cost")
                is CostAtom.ExilePermanents -> EffectResult.error(state, "ExilePermanents payment for PayOrSuffer not supported")
                is CostAtom.RemoveCounters -> handleRemoveCountersCost(state, effect, context, atom, sourceId, sourceCard.name, payingPlayerId)
            }
        }
    }

    /**
     * Handle a discard cost - player must discard cards matching filter to avoid suffer effect.
     */
    private fun handleDiscardCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.Discard,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        // Handle random discard separately
        if (cost.random) {
            return handleRandomDiscard(state, effect, context, cost, sourceId, sourceName, controllerId)
        }

        // Find all valid cards in hand that match the filter
        val validCards = findValidCardsInHand(state, controllerId, cost.filter)

        // If the player doesn't have enough matching cards, automatically execute suffer effect
        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Player has at least enough valid cards - present the decision
        val prompt = buildDiscardPrompt(cost, sourceName, effect.suffer)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validCards,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.DISCARD,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle random discard variant.
     * Prompts the player with a yes/no choice.
     */
    private fun handleRandomDiscard(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.Discard,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        val validCards = findValidCardsInHand(state, controllerId, cost.filter)

        // If no valid cards, execute suffer effect automatically
        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val prompt = "Discard ${if (cost.count == 1) "a card" else "${cost.count} cards"} at random to keep $sourceName?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Discard",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.DISCARD,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = true,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
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
    }

    /**
     * Handle a sacrifice cost - player must sacrifice permanents matching filter to avoid suffer effect.
     */
    private fun handleSacrificeCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.Sacrifice,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        // Find all valid permanents on the battlefield that the player controls
        val validPermanents = findValidPermanentsOnBattlefield(state, controllerId, cost.filter, sourceId)

        // If the player doesn't have enough permanents, automatically execute suffer effect
        if (validPermanents.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Player has enough - present the decision
        val prompt = buildSacrificePrompt(cost, sourceName, effect.suffer)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true  // Use battlefield targeting UI instead of modal
        )

        // Push continuation to handle the response
        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.SACRIFICE,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle a tap cost - player must tap one or more of their untapped permanents
     * to avoid the suffer effect.
     *
     * Filters candidates to permanents the paying player controls that are untapped,
     * excluding the source itself (the source is typically tapped or off-battlefield
     * but we exclude defensively, matching the sacrifice-cost convention).
     */
    private fun handleTapCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.TapPermanents,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        // Untapped permanents the player controls that match the filter, excluding the source.
        val validPermanents = findValidUntappedPermanentsOnBattlefield(
            state, controllerId, cost.filter, sourceId
        )

        // If the player doesn't have enough untapped permanents, automatically suffer.
        if (validPermanents.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        val prompt = buildTapPrompt(cost, sourceName, effect.suffer)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true  // Click an untapped permanent in play to tap it
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.TAP,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle a pay life cost - player must pay life to avoid suffer effect.
     */
    private fun handlePayLifeCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.PayLife,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        // Check if player has enough life to pay (must have more than the cost).
        // CR 810.9a — affordability uses the team's shared total in Two-Headed Giant.
        val playerLife = state.lifeTotal(controllerId)

        // If player doesn't have enough life to pay and survive, execute suffer effect
        if (playerLife <= cost.amount) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val prompt = "Pay ${cost.amount} life to avoid ${effect.suffer.description}?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay life",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.PAY_LIFE,
            sufferEffect = effect.suffer,
            requiredCount = cost.amount,
            filter = GameObjectFilter.Any, // Not used for life payment
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
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
    }

    /**
     * Handle an exile cost - player must exile cards from a zone to avoid suffer effect.
     */
    private fun handleExileCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.ExileFrom,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        val validCards = findValidCardsInZone(state, controllerId, cost.filter, cost.zone)

        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        val prompt = buildExilePrompt(cost, sourceName, effect.suffer)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validCards,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.EXILE,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId,
            zone = cost.zone
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle a mana cost - player must pay mana to avoid suffer effect.
     */
    private fun handleManaCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.Mana,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        // Check if the player can pay the mana cost
        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, controllerId, cost.cost)) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val consequence = describeConsequence(effect.suffer, sourceName)
        val prompt = "Pay ${cost.cost} or $consequence?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay ${cost.cost}",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.MANA,
            sufferEffect = effect.suffer,
            requiredCount = 0,
            filter = GameObjectFilter.Any,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId,
            manaCost = cost.cost
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
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
    }

    /**
     * Handle a choice cost - player picks which cost to pay from multiple options.
     */
    private fun handleChoiceCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Choice,
        sourceId: EntityId,
        sourceName: String,
        payingPlayerId: EntityId
    ): EffectResult {
        // Build available options: only include costs the player can actually pay
        val availableOptions = mutableListOf<Pair<Int, String>>()
        for ((index, option) in cost.options.withIndex()) {
            if (canPayCost(state, payingPlayerId, option, sourceId)) {
                availableOptions.add(index to option.description.replaceFirstChar { it.uppercase() })
            }
        }

        // Always add the suffer option
        val sufferDescription = effect.suffer.description.replaceFirstChar { it.uppercase() }

        val optionLabels = availableOptions.map { it.second } + sufferDescription

        // If no avoidance options are available, automatically suffer
        if (availableOptions.isEmpty()) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        val decisionId = UUID.randomUUID().toString()
        val prompt = "Choose one:"

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = payingPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = optionLabels
        )

        val continuation = PayOrSufferChoiceContinuation(
            decisionId = decisionId,
            playerId = payingPlayerId,
            sourceId = sourceId,
            sourceName = sourceName,
            options = availableOptions.map { cost.options[it.first] },
            sufferEffect = effect.suffer,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            triggeringEntityId = context.triggeringEntityId,
            triggeringPlayerId = context.triggeringPlayerId,
            abilityControllerId = context.controllerId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = payingPlayerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = prompt
                )
            )
        )
    }


    /**
     * Handles a remove counters cost - player must remove the specified number of counters from the specified entities.
     */
    private fun handleRemoveCountersCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: CostAtom.RemoveCounters,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): EffectResult {
        val payment = costPaymentService.pay(
            state = state,
            payerId = controllerId,
            cost = PayCost.Atom(cost),
            sourceId = sourceId,
            ctx = CostPaymentContext(
                onDeclined = effect.suffer,
                targets = context.targets,
                namedTargets = context.pipeline.namedTargets,
                storedCollections = context.pipeline.storedCollections
            ),
            // "Remove counters from among creatures you control" does not say "another".
            excludeSource = false
        )
        return when (payment) {
            is PaymentResult.Pending ->
                EffectResult.paused(payment.state, payment.pendingDecision, payment.events)
            is PaymentResult.Unaffordable ->
                executeSufferEffect(state, effect.suffer, context)
            is PaymentResult.Paid ->
                EffectResult.success(payment.state, payment.events)
            is PaymentResult.Declined ->
                executeSufferEffect(state, effect.suffer, context)
        }
    }

    /**
     * Check if a player can pay a specific cost.
     */
    private fun canPayCost(
        state: GameState,
        playerId: EntityId,
        cost: PayCost,
        sourceId: EntityId
    ): Boolean {
        return when (cost) {
            is PayCost.OwnManaCost -> {
                // Null only when the source entity/CardComponent is missing; an empty mana cost
                // (lands, tokens) is {0} and always payable — see the execute branch above.
                val ownCost = state.getEntity(sourceId)?.get<CardComponent>()?.manaCost
                ownCost != null && ManaSolver(cardRegistry).canPay(state, playerId, ownCost)
            }
            is PayCost.Choice -> cost.options.any { canPayCost(state, playerId, it, sourceId) }
            is PayCost.Atom -> when (val atom = cost.atom) {
                is CostAtom.Discard -> findValidCardsInHand(state, playerId, atom.filter).size >= atom.count
                is CostAtom.Sacrifice -> findValidPermanentsOnBattlefield(state, playerId, atom.filter, sourceId).size >= atom.count
                is CostAtom.PayLife -> {
                    val life = state.lifeTotal(playerId) // CR 810.9a — team's shared total
                    life > atom.amount
                }
                is CostAtom.Mana -> ManaSolver(cardRegistry).canPay(state, playerId, atom.cost)
                is CostAtom.ExileFrom -> findValidCardsInZone(state, playerId, atom.filter, atom.zone).size >= atom.count
                is CostAtom.TapPermanents -> findValidUntappedPermanentsOnBattlefield(state, playerId, atom.filter, sourceId).size >= atom.count
                is CostAtom.ReturnToHand -> false
                is CostAtom.RevealFromHand -> false
                is CostAtom.PutCountersOnSelf -> false
                is CostAtom.ExilePermanents -> false
                is CostAtom.RemoveCounters -> {
                    // Can pay if there are permanents matching the filter with enough counters.
                    // Don't exclude the source — removing counters from the source itself is a
                    // legitimate payment, matching the logic in handleRemoveCountersCost.
                    val candidates = if (atom.self) listOf(sourceId)
                    else BattlefieldFilterUtils.findMatchingOnBattlefield(
                        state, atom.filter.youControl(), PredicateContext(controllerId = playerId)
                    )
                    val counterType = atom.counterType?.let {
                        com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(it)
                    }
                    val required = (atom.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 0
                    val total = candidates.sumOf { permId ->
                        val counters = state.getEntity(permId)?.get<CountersComponent>() ?: return@sumOf 0
                        if (counterType != null) counters.getCount(counterType)
                        else counters.counters.values.sum()
                    }
                    total >= required
                }
            }
        }
    }

    /**
     * Find all cards in hand that match the specified filter.
     */
    private fun findValidCardsInHand(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val context = PredicateContext(controllerId = playerId)

        return hand.filter { cardId ->
            predicateEvaluator.matches(state, state.projectedState, cardId, filter, context)
        }
    }

    /**
     * Find all cards in a given zone that match the specified filter.
     */
    private fun findValidCardsInZone(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        zone: Zone
    ): List<EntityId> {
        if (zone == Zone.BATTLEFIELD) {
            return BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = playerId)
            )
        }
        val zoneKey = ZoneKey(playerId, zone)
        val cards = state.getZone(zoneKey)
        val context = PredicateContext(controllerId = playerId)
        return cards.filter { cardId ->
            predicateEvaluator.matches(state, state.projectedState, cardId, filter, context)
        }
    }

    /**
     * Find all permanents on the battlefield that match the filter.
     * Excludes the source permanent itself.
     */
    private fun findValidPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = playerId), excludeSelfId = sourceId
        )
    }

    /**
     * Find all untapped permanents the player controls that match the filter.
     * Excludes the source permanent itself.
     */
    private fun findValidUntappedPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl().untapped(),
            PredicateContext(controllerId = playerId),
            excludeSelfId = sourceId
        )
    }

    /**
     * Execute the suffer effect.
     */
    private fun executeSufferEffect(
        state: GameState,
        sufferEffect: Effect,
        context: EffectContext
    ): EffectResult {
        // Use injected executor if available, otherwise handle common cases
        if (executeEffect != null) {
            return executeEffect.invoke(state, sufferEffect, context)
        }

        // Fallback: Handle the most common suffer effects directly
        return when (sufferEffect) {
            is SacrificeSelfEffect -> {
                // Handle "sacrifice this" - the most common suffer effect
                val sourceId = context.sourceId ?: return EffectResult.success(state)
                val controllerId = context.controllerId
                sacrificePermanent(state, controllerId, sourceId)
            }
            is SacrificeEffect -> {
                // Handle "sacrifice this" when using SacrificeEffect
                // For PayOrSufferEffect, we assume it means sacrifice self
                val sourceId = context.sourceId ?: return EffectResult.success(state)
                val controllerId = context.controllerId
                sacrificePermanent(state, controllerId, sourceId)
            }
            else -> {
                // For other effects, we'd need the full executor registry
                // This should be handled by the registry in practice
                EffectResult.error(state, "Cannot execute suffer effect: ${sufferEffect::class.simpleName}")
            }
        }
    }

    /**
     * Sacrifice a permanent.
     */
    private fun sacrificePermanent(
        state: GameState,
        playerId: EntityId,
        permanentId: EntityId
    ): EffectResult {
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Check if the permanent is still on the battlefield
        if (permanentId !in state.getZone(battlefieldZone)) {
            return EffectResult.success(state)
        }

        val permanentName = state.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"

        var newState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .trackPermanentSacrifice(state, listOf(permanentId), playerId)
        newState = newState.removeFromZone(battlefieldZone, permanentId)
        newState = newState.addToZone(graveyardZone, permanentId)

        val events = listOf(
            PermanentsSacrificedEvent(playerId, listOf(permanentId), listOf(permanentName)),
            ZoneChangeEvent(
                entityId = permanentId,
                entityName = permanentName,
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.GRAVEYARD,
                ownerId = playerId
            )
        )

        return EffectResult.success(newState, events)
    }

    /**
     * Build prompt for discard cost.
     */
    private fun buildDiscardPrompt(cost: CostAtom.Discard, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            val article = if (desc == "card") "a" else if (desc.first().lowercaseChar() in "aeiou") "an" else "a"
            "$article $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Discard $typeText or $consequence"
    }

    /**
     * Build prompt for sacrifice cost.
     */
    private fun buildSacrificePrompt(cost: CostAtom.Sacrifice, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            "${if (desc.first().lowercaseChar() in "aeiou") "an" else "a"} $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Sacrifice $typeText or $consequence"
    }

    /**
     * Build prompt for tap cost.
     */
    private fun buildTapPrompt(cost: CostAtom.TapPermanents, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            // The article always precedes "untapped", so it is always "an".
            "an untapped $desc you control"
        } else {
            "${cost.count} untapped ${desc}s you control"
        }
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Tap $typeText or $consequence"
    }

    /**
     * Build prompt for exile cost.
     */
    private fun buildExilePrompt(cost: CostAtom.ExileFrom, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            "${if (desc.first().lowercaseChar() in "aeiou") "an" else "a"} $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val zoneName = cost.zone.name.lowercase()
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Exile $typeText from your $zoneName or $consequence"
    }

    /**
     * Describe the consequence of not paying the cost.
     */
    private fun describeConsequence(sufferEffect: Effect, sourceName: String): String {
        return when (sufferEffect) {
            is SacrificeSelfEffect,
            is SacrificeEffect -> "sacrifice $sourceName"
            else -> sufferEffect.description
        }
    }

    companion object {
        private val predicateEvaluatorStatic = PredicateEvaluator()

        /**
         * Execute the random discard after player confirmed.
         */
        fun executeRandomDiscard(
            state: GameState,
            playerId: EntityId,
            filter: GameObjectFilter,
            count: Int
        ): EffectResult {
            val handZone = ZoneKey(playerId, Zone.HAND)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            val hand = state.getZone(handZone)
            val context = PredicateContext(controllerId = playerId)

            // Filter valid cards
            val validCards = hand.filter { cardId ->
                predicateEvaluatorStatic.matches(state, state.projectedState, cardId, filter, context)
            }

            if (validCards.isEmpty()) {
                return EffectResult.success(state)
            }

            // Randomly select cards to discard
            val (shuffledValid, stateAfterShuffle) = state.nextRandom { shuffle(validCards) }
            val cardsToDiscard = shuffledValid.take(count)
            var newState = stateAfterShuffle
            val events = mutableListOf<GameEvent>()

            for (cardId in cardsToDiscard) {
                val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
                newState = newState.removeFromZone(handZone, cardId)
                newState = newState.addToZone(graveyardZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = Zone.HAND,
                        toZone = Zone.GRAVEYARD,
                        ownerId = playerId
                    )
                )
            }

            val discardNames = cardsToDiscard.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(0, CardsDiscardedEvent(playerId, cardsToDiscard, discardNames))

            return EffectResult.success(newState, events)
        }
    }
}
