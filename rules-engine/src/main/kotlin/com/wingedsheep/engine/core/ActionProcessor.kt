package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AddManaEffect
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.CastRestriction

/**
 * The central action processor for the game engine.
 *
 * This is the main entry point for all game actions. It validates actions,
 * executes them against the game state, and returns the result.
 *
 * The processor is stateless - it's a pure function:
 * (GameState, GameAction) -> ExecutionResult(GameState, Events)
 */
class ActionProcessor(
    private val cardRegistry: CardRegistry? = null,
    private val combatManager: CombatManager = CombatManager(cardRegistry),
    private val turnManager: TurnManager = TurnManager(combatManager),
    private val stackResolver: StackResolver = StackResolver(),
    private val manaSolver: ManaSolver = ManaSolver(cardRegistry),
    private val costCalculator: CostCalculator = CostCalculator(cardRegistry),
    private val alternativePaymentHandler: AlternativePaymentHandler = AlternativePaymentHandler(),
    private val costHandler: CostHandler = CostHandler(),
    private val mulliganHandler: MulliganHandler = MulliganHandler(),
    private val effectExecutorRegistry: EffectExecutorRegistry = EffectExecutorRegistry(),
    private val continuationHandler: ContinuationHandler = ContinuationHandler(effectExecutorRegistry),
    private val sbaChecker: StateBasedActionChecker = StateBasedActionChecker(),
    private val triggerDetector: TriggerDetector = TriggerDetector(cardRegistry),
    private val triggerProcessor: TriggerProcessor = TriggerProcessor(),
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator(),
    private val targetValidator: TargetValidator = TargetValidator(cardRegistry)
) {

    /**
     * Process a game action and return the result.
     */
    fun process(state: GameState, action: GameAction): ExecutionResult {
        // Validate the action
        val validationError = validate(state, action)
        if (validationError != null) {
            return ExecutionResult.error(state, validationError)
        }

        // Execute the action
        return execute(state, action)
    }

    /**
     * Validate that an action is legal.
     * Returns an error message if invalid, null if valid.
     */
    private fun validate(state: GameState, action: GameAction): String? {
        // Check game is not over
        if (state.gameOver) {
            return "Game is already over"
        }

        // Check player exists
        if (!state.turnOrder.contains(action.playerId)) {
            return "Unknown player: ${action.playerId}"
        }

        return when (action) {
            is PassPriority -> validatePassPriority(state, action)
            is CastSpell -> validateCastSpell(state, action)
            is ActivateAbility -> validateActivateAbility(state, action)
            is PlayLand -> validatePlayLand(state, action)
            is DeclareAttackers -> validateDeclareAttackers(state, action)
            is DeclareBlockers -> validateDeclareBlockers(state, action)
            is OrderBlockers -> validateOrderBlockers(state, action)
            is MakeChoice -> validateMakeChoice(state, action)
            is SelectTargets -> validateSelectTargets(state, action)
            is ChooseManaColor -> validateChooseManaColor(state, action)
            is SubmitDecision -> validateSubmitDecision(state, action)
            is TakeMulligan -> validateTakeMulligan(state, action)
            is KeepHand -> validateKeepHand(state, action)
            is BottomCards -> validateBottomCards(state, action)
            is Concede -> null  // Always valid
        }
    }

    private fun validatePassPriority(state: GameState, action: PassPriority): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        // Cannot pass priority while there's a pending decision - must submit decision first
        val pendingDecision = state.pendingDecision
        if (pendingDecision != null) {
            return "Cannot pass priority while there's a pending decision - please respond to: ${pendingDecision.prompt}"
        }
        return null
    }

    private fun validateCastSpell(state: GameState, action: CastSpell): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check the card exists and is in hand
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        // Check card is in a zone it can be cast from (typically hand)
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Card is not in your hand"
        }

        // Check timing (sorcery speed vs instant speed)
        if (!cardComponent.typeLine.isInstant) {
            // Non-instants require sorcery timing
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast sorcery-speed spells during your main phase with an empty stack"
            }
        }

        // Get card definition for cast restrictions and cost calculation
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)

        // Check cast restrictions from card definition
        if (cardDef != null && cardDef.script.castRestrictions.isNotEmpty()) {
            val restrictionError = validateCastRestrictions(state, cardDef.script.castRestrictions, action.playerId)
            if (restrictionError != null) {
                return restrictionError
            }
        }

        // Validate additional costs
        if (cardDef != null) {
            for (additionalCost in cardDef.script.additionalCosts) {
                when (additionalCost) {
                    is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent -> {
                        val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                        if (sacrificed.size < additionalCost.count) {
                            return "You must sacrifice ${additionalCost.count} ${additionalCost.filter.description} to cast this spell"
                        }
                        for (permId in sacrificed) {
                            val permContainer = state.getEntity(permId)
                                ?: return "Sacrificed permanent not found: $permId"
                            val permCard = permContainer.get<CardComponent>()
                                ?: return "Sacrificed entity is not a card: $permId"
                            val permController = permContainer.get<ControllerComponent>()
                            if (permController?.playerId != action.playerId) {
                                return "You can only sacrifice permanents you control"
                            }
                            if (permId !in state.getBattlefield()) {
                                return "Sacrificed permanent is not on the battlefield: $permId"
                            }
                            if (!matchesCardFilter(permCard, additionalCost.filter)) {
                                return "${permCard.name} doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                    else -> {
                        // Other additional cost types not yet validated
                    }
                }
            }
        }

        // Check mana cost can be paid
        val xValue = action.xValue ?: 0

        // Calculate effective cost after applying cost reductions
        val effectiveCost = if (cardDef != null) {
            costCalculator.calculateEffectiveCost(state, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                // Check if auto-pay can find a solution
                if (!manaSolver.canPay(state, action.playerId, effectiveCost, xValue)) {
                    return "Not enough mana to cast this spell"
                }
            }
            is PaymentStrategy.FromPool -> {
                // Check if the mana pool has enough
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )
                if (!costHandler.canPayManaCost(pool, effectiveCost)) {
                    return "Insufficient mana in pool to cast this spell"
                }
            }
            is PaymentStrategy.Explicit -> {
                // Validate that the specified sources exist and can produce enough mana
                // For now, just check that all sources exist and are untapped
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                // TODO: Full validation that explicit sources produce enough mana
            }
        }

        // Validate targets against target requirements
        if (cardDef != null && action.targets.isNotEmpty()) {
            val targetRequirements = cardDef.script.targetRequirements
            if (targetRequirements.isNotEmpty()) {
                val targetError = targetValidator.validateTargets(
                    state,
                    action.targets,
                    targetRequirements,
                    action.playerId
                )
                if (targetError != null) {
                    return targetError
                }
            }
        }

        return null
    }

    /**
     * Validate cast restrictions from the card script.
     * Returns an error message if any restriction is violated, null if all pass.
     */
    private fun validateCastRestrictions(
        state: GameState,
        restrictions: List<CastRestriction>,
        playerId: EntityId
    ): String? {
        // Get opponent ID for condition evaluation
        val opponentId = state.turnOrder.firstOrNull { it != playerId }

        // Create context for condition evaluation
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val error = validateSingleRestriction(state, restriction, context)
            if (error != null) return error
        }
        return null
    }

    private fun validateSingleRestriction(
        state: GameState,
        restriction: CastRestriction,
        context: EffectContext
    ): String? {
        return when (restriction) {
            is CastRestriction.OnlyDuringStep -> {
                if (state.step != restriction.step) {
                    "Can only be cast during the ${restriction.step.name.lowercase().replace('_', ' ')} step"
                } else null
            }
            is CastRestriction.OnlyDuringPhase -> {
                if (state.phase != restriction.phase) {
                    "Can only be cast during the ${restriction.phase.name.lowercase().replace('_', ' ')} phase"
                } else null
            }
            is CastRestriction.OnlyIfCondition -> {
                if (!conditionEvaluator.evaluate(state, restriction.condition, context)) {
                    "Casting condition not met"
                } else null
            }
            is CastRestriction.TimingRequirement -> {
                // TODO: Handle timing requirements (sorcery/instant speed overrides)
                null
            }
            is CastRestriction.All -> {
                for (subRestriction in restriction.restrictions) {
                    val error = validateSingleRestriction(state, subRestriction, context)
                    if (error != null) return error
                }
                null
            }
        }
    }

    private fun matchesCardFilter(card: CardComponent, filter: CardFilter): Boolean {
        return when (filter) {
            is CardFilter.AnyCard -> true
            is CardFilter.CreatureCard -> card.typeLine.isCreature
            is CardFilter.LandCard -> card.typeLine.isLand
            is CardFilter.BasicLandCard -> card.typeLine.isBasicLand
            is CardFilter.SorceryCard -> card.typeLine.isSorcery
            is CardFilter.InstantCard -> card.typeLine.isInstant
            is CardFilter.HasSubtype -> card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
            is CardFilter.HasColor -> card.colors.contains(filter.color)
            is CardFilter.And -> filter.filters.all { matchesCardFilter(card, it) }
            is CardFilter.Or -> filter.filters.any { matchesCardFilter(card, it) }
            is CardFilter.PermanentCard -> card.typeLine.isPermanent
            is CardFilter.NonlandPermanentCard -> card.typeLine.isPermanent && !card.typeLine.isLand
            is CardFilter.ManaValueAtMost -> card.manaCost.cmc <= filter.maxManaValue
            is CardFilter.Not -> !matchesCardFilter(card, filter.filter)
        }
    }

    private fun validateActivateAbility(state: GameState, action: ActivateAbility): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check the source exists
        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        // Check player controls the source
        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != action.playerId) {
            return "You don't control this permanent"
        }

        val cardComponent = container.get<CardComponent>()
            ?: return "Source is not a card"

        // Look up the card definition to find the ability
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: return "Ability not found on this card"

        // Check cost requirements
        when (ability.cost) {
            is AbilityCost.Tap -> {
                // Must be untapped to pay tap cost
                if (container.has<TappedComponent>()) {
                    return "This permanent is already tapped"
                }

                // Check summoning sickness for creatures (non-lands)
                if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                    val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                    // Check for haste keyword (note: this doesn't check granted haste, future enhancement)
                    val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) {
                        return "This creature has summoning sickness"
                    }
                }
            }
            else -> {
                // Other cost types - TODO: validate when implemented
            }
        }

        // Check activation restrictions
        for (restriction in ability.restrictions) {
            val error = checkActivationRestriction(state, action.playerId, restriction)
            if (error != null) return error
        }

        return null
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        restriction: ActivationRestriction
    ): String? {
        return when (restriction) {
            is ActivationRestriction.OnlyDuringYourTurn -> {
                if (state.activePlayerId != playerId) "This ability can only be activated during your turn"
                else null
            }
            is ActivationRestriction.BeforeStep -> {
                if (state.step.ordinal >= restriction.step.ordinal)
                    "This ability can only be activated before ${restriction.step.displayName}"
                else null
            }
            is ActivationRestriction.DuringPhase -> {
                if (state.phase != restriction.phase)
                    "This ability can only be activated during ${restriction.phase.displayName}"
                else null
            }
            is ActivationRestriction.DuringStep -> {
                if (state.step != restriction.step)
                    "This ability can only be activated during ${restriction.step.displayName}"
                else null
            }
            is ActivationRestriction.OnlyIfCondition -> {
                val opponentId = state.turnOrder.firstOrNull { it != playerId }
                val context = EffectContext(
                    sourceId = null,
                    controllerId = playerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = 0
                )
                if (!conditionEvaluator.evaluate(state, restriction.condition, context))
                    "Activation condition not met"
                else null
            }
            is ActivationRestriction.All -> {
                restriction.restrictions.firstNotNullOfOrNull {
                    checkActivationRestriction(state, playerId, it)
                }
            }
        }
    }

    private fun validatePlayLand(state: GameState, action: PlayLand): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only play lands on your turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Check land drop availability
        val landDrops = state.getEntity(action.playerId)?.get<LandDropsComponent>()
            ?: LandDropsComponent()
        if (!landDrops.canPlayLand) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        if (!cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Check card is in hand
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Land is not in your hand"
        }

        return null
    }

    private fun validateDeclareAttackers(state: GameState, action: DeclareAttackers): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only declare attackers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS) {
            return "You can only declare attackers during the declare attackers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    private fun validateDeclareBlockers(state: GameState, action: DeclareBlockers): String? {
        if (state.activePlayerId == action.playerId) {
            return "You cannot declare blockers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS) {
            return "You can only declare blockers during the declare blockers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    private fun validateOrderBlockers(state: GameState, action: OrderBlockers): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only order blockers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS) {
            return "You can only order blockers during the declare blockers step"
        }
        return null
    }

    private fun validateMakeChoice(state: GameState, action: MakeChoice): String? {
        // TODO: Validate choice context
        return null
    }

    private fun validateSelectTargets(state: GameState, action: SelectTargets): String? {
        // TODO: Validate target selection
        return null
    }

    private fun validateChooseManaColor(state: GameState, action: ChooseManaColor): String? {
        // TODO: Validate mana color choice
        return null
    }

    private fun validateTakeMulligan(state: GameState, action: TakeMulligan): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        if (!mullState.canMulligan) {
            return "Cannot mulligan - hand size would be 0"
        }

        return null
    }

    private fun validateKeepHand(state: GameState, action: KeepHand): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        return null
    }

    private fun validateBottomCards(state: GameState, action: BottomCards): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (!mullState.hasKept) {
            return "You have not kept your hand yet"
        }

        if (action.cardIds.size != mullState.cardsToBottom) {
            return "Must put exactly ${mullState.cardsToBottom} cards on bottom, got ${action.cardIds.size}"
        }

        // Validate cards are in hand
        val hand = state.getHand(action.playerId).toSet()
        val invalidCards = action.cardIds.filter { it !in hand }
        if (invalidCards.isNotEmpty()) {
            return "Cards not in hand: $invalidCards"
        }

        return null
    }

    /**
     * Execute a validated action.
     */
    private fun execute(state: GameState, action: GameAction): ExecutionResult {
        return when (action) {
            is PassPriority -> executePassPriority(state, action)
            is CastSpell -> executeCastSpell(state, action)
            is ActivateAbility -> executeActivateAbility(state, action)
            is PlayLand -> executePlayLand(state, action)
            is DeclareAttackers -> executeDeclareAttackers(state, action)
            is DeclareBlockers -> executeDeclareBlockers(state, action)
            is OrderBlockers -> executeOrderBlockers(state, action)
            is MakeChoice -> executeMakeChoice(state, action)
            is SelectTargets -> executeSelectTargets(state, action)
            is ChooseManaColor -> executeChooseManaColor(state, action)
            is SubmitDecision -> executeSubmitDecision(state, action)
            is TakeMulligan -> executeTakeMulligan(state, action)
            is KeepHand -> executeKeepHand(state, action)
            is BottomCards -> executeBottomCards(state, action)
            is Concede -> executeConcede(state, action)
        }
    }

    private fun executePassPriority(state: GameState, action: PassPriority): ExecutionResult {
        val newState = state.withPriorityPassed(action.playerId)

        // Check if all players passed
        if (newState.allPlayersPassed()) {
            // Either resolve top of stack or advance game
            return if (newState.stack.isNotEmpty()) {
                resolveTopOfStack(newState)
            } else {
                val advanceResult = advanceGame(newState)
                if (!advanceResult.isSuccess || advanceResult.events.isEmpty()) {
                    return advanceResult
                }
                // Detect triggers from step transition events (e.g., death triggers from combat damage SBAs)
                val triggers = triggerDetector.detectTriggers(advanceResult.newState, advanceResult.events)
                if (triggers.isNotEmpty()) {
                    val triggerResult = triggerProcessor.processTriggers(advanceResult.newState, triggers)
                    if (triggerResult.isPaused) {
                        return ExecutionResult.paused(
                            triggerResult.state,
                            triggerResult.pendingDecision!!,
                            advanceResult.events + triggerResult.events
                        )
                    }
                    return ExecutionResult.success(
                        triggerResult.newState.withPriority(state.activePlayerId),
                        advanceResult.events + triggerResult.events
                    )
                }
                advanceResult
            }
        }

        // Pass to next player - use copy() to preserve priorityPassedBy
        // (withPriority would reset the passed flags)
        val nextPlayer = state.getNextPlayer(action.playerId)
        return ExecutionResult.success(
            newState.copy(priorityPlayerId = nextPlayer),
            listOf(PriorityChangedEvent(nextPlayer))
        )
    }

    private fun executeCastSpell(state: GameState, action: CastSpell): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Get the card being cast
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")

        val xValue = action.xValue ?: 0

        // Calculate effective cost after applying cost reductions
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
        var effectiveCost = if (cardDef != null) {
            costCalculator.calculateEffectiveCost(currentState, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Process additional costs (sacrifice, discard, etc.)
        val sacrificedPermanentIds = mutableListOf<EntityId>()
        if (cardDef != null && action.additionalCostPayment != null) {
            for (additionalCost in cardDef.script.additionalCosts) {
                when (additionalCost) {
                    is com.wingedsheep.sdk.scripting.AdditionalCost.SacrificePermanent -> {
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            sacrificedPermanentIds.add(permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = ZoneType.BATTLEFIELD,
                                toZone = ZoneType.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                    }
                    else -> {
                        // Other additional cost types not yet implemented
                    }
                }
            }
        }

        // Apply alternative payment (Delve/Convoke) if specified
        if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            val altPaymentResult = alternativePaymentHandler.apply(
                currentState,
                effectiveCost,
                action.alternativePayment,
                action.playerId,
                cardDef
            )
            effectiveCost = altPaymentResult.reducedCost
            currentState = altPaymentResult.newState
            events.addAll(altPaymentResult.events)
        }

        // Handle mana payment based on strategy
        when (action.paymentStrategy) {
            is PaymentStrategy.FromPool -> {
                // Player has already floated mana - deduct from pool
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

                val newPool = costHandler.payManaCost(pool, effectiveCost)
                    ?: return ExecutionResult.error(currentState, "Insufficient mana in pool")

                // Update the player's mana pool component
                currentState = currentState.updateEntity(action.playerId) { container ->
                    container.with(
                        ManaPoolComponent(
                            white = newPool.white,
                            blue = newPool.blue,
                            black = newPool.black,
                            red = newPool.red,
                            green = newPool.green,
                            colorless = newPool.colorless
                        )
                    )
                }

                // Emit mana spent event
                val spent = ManaSpentEvent(
                    playerId = action.playerId,
                    reason = "Cast ${cardComponent.name}",
                    white = poolComponent.white - newPool.white,
                    blue = poolComponent.blue - newPool.blue,
                    black = poolComponent.black - newPool.black,
                    red = poolComponent.red - newPool.red,
                    green = poolComponent.green - newPool.green,
                    colorless = poolComponent.colorless - newPool.colorless
                )
                events.add(spent)
            }

            is PaymentStrategy.AutoPay -> {
                // First try to pay from the mana pool
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

                val newPool = costHandler.payManaCost(pool, effectiveCost)

                if (newPool != null) {
                    // Successfully paid from pool
                    currentState = currentState.updateEntity(action.playerId) { container ->
                        container.with(
                            ManaPoolComponent(
                                white = newPool.white,
                                blue = newPool.blue,
                                black = newPool.black,
                                red = newPool.red,
                                green = newPool.green,
                                colorless = newPool.colorless
                            )
                        )
                    }

                    events.add(
                        ManaSpentEvent(
                            playerId = action.playerId,
                            reason = "Cast ${cardComponent.name}",
                            white = poolComponent.white - newPool.white,
                            blue = poolComponent.blue - newPool.blue,
                            black = poolComponent.black - newPool.black,
                            red = poolComponent.red - newPool.red,
                            green = poolComponent.green - newPool.green,
                            colorless = poolComponent.colorless - newPool.colorless
                        )
                    )
                } else {
                    // Pool didn't have enough - try tapping lands
                    val solution = manaSolver.solve(currentState, action.playerId, effectiveCost, xValue)
                        ?: return ExecutionResult.error(currentState, "Not enough mana to auto-pay")

                    // Tap each source and generate events
                    for (source in solution.sources) {
                        currentState = currentState.updateEntity(source.entityId) { container ->
                            container.with(TappedComponent)
                        }

                        // Emit tapped event for client animation
                        events.add(TappedEvent(source.entityId, source.name))
                    }

                    // Track mana spent for the event
                    var whiteSpent = 0
                    var blueSpent = 0
                    var blackSpent = 0
                    var redSpent = 0
                    var greenSpent = 0
                    var colorlessSpent = 0

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

                    events.add(
                        ManaSpentEvent(
                            playerId = action.playerId,
                            reason = "Cast ${cardComponent.name}",
                            white = whiteSpent,
                            blue = blueSpent,
                            black = blackSpent,
                            red = redSpent,
                            green = greenSpent,
                            colorless = colorlessSpent
                        )
                    )
                }
            }

            is PaymentStrategy.Explicit -> {
                // Tap the specified sources
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceName = currentState.getEntity(sourceId)
                        ?.get<CardComponent>()?.name ?: "Unknown"

                    currentState = currentState.updateEntity(sourceId) { container ->
                        container.with(TappedComponent)
                    }

                    events.add(TappedEvent(sourceId, sourceName))
                }

                // For explicit payment, we trust that the client sent valid sources
                // TODO: More sophisticated tracking of exactly which mana was produced
            }
        }

        // Cast the spell using StackResolver
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedPermanentIds
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        // Combine our events with cast events
        var allEvents = events + castResult.events

        // Detect and process triggers from casting (e.g., "when you cast a spell")
        val triggers = triggerDetector.detectTriggers(castResult.newState, castResult.events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(castResult.newState, triggers)

            if (triggerResult.isPaused) {
                // A trigger requires target selection
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(state.activePlayerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            allEvents = allEvents + triggerResult.events

            // After casting and triggers, active player gets priority
            return ExecutionResult.success(
                triggerResult.newState.withPriority(state.activePlayerId),
                allEvents
            )
        }

        // After casting, active player gets priority
        return ExecutionResult.success(
            castResult.newState.withPriority(state.activePlayerId),
            allEvents
        )
    }

    private fun executeActivateAbility(state: GameState, action: ActivateAbility): ExecutionResult {
        val container = state.getEntity(action.sourceId)
            ?: return ExecutionResult.error(state, "Source not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card")

        // Look up the card definition to find the ability
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: return ExecutionResult.error(state, "Ability not found")

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Pay the cost
        when (ability.cost) {
            is AbilityCost.Tap -> {
                // Tap the permanent
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    c.with(TappedComponent)
                }
                events.add(TappedEvent(action.sourceId, cardComponent.name))
            }
            else -> {
                // TODO: Handle other cost types (mana, sacrifice, etc.)
            }
        }

        // Handle mana abilities - they don't use the stack
        if (ability.isManaAbility) {
            // Create effect context
            val opponentId = state.turnOrder.firstOrNull { it != action.playerId }
            val context = EffectContext(
                sourceId = action.sourceId,
                controllerId = action.playerId,
                opponentId = opponentId,
                targets = action.targets,
                xValue = null
            )

            // Execute the effect
            val effectResult = effectExecutorRegistry.execute(currentState, ability.effect, context)
            if (!effectResult.isSuccess) {
                return effectResult
            }

            currentState = effectResult.newState

            // Emit ManaAddedEvent based on the effect type
            val manaEvent = when (val effect = ability.effect) {
                is AddManaEffect -> ManaAddedEvent(
                    playerId = action.playerId,
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    white = if (effect.color == Color.WHITE) effect.amount else 0,
                    blue = if (effect.color == Color.BLUE) effect.amount else 0,
                    black = if (effect.color == Color.BLACK) effect.amount else 0,
                    red = if (effect.color == Color.RED) effect.amount else 0,
                    green = if (effect.color == Color.GREEN) effect.amount else 0,
                    colorless = 0
                )
                is AddColorlessManaEffect -> ManaAddedEvent(
                    playerId = action.playerId,
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    colorless = effect.amount
                )
                else -> null // Other mana effects might need different handling
            }

            if (manaEvent != null) {
                events.add(manaEvent)
            }

            // Combine events and return (mana abilities don't change priority)
            return ExecutionResult.success(currentState, events + effectResult.events)
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = ability.effect
        )
        val stackResult = stackResolver.putActivatedAbility(currentState, abilityOnStack, action.targets)
        return ExecutionResult.success(stackResult.newState, events + stackResult.events)
    }

    private fun executePlayLand(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        newState = newState.removeFromZone(handZone, action.cardId)

        // Add to battlefield
        val battlefieldZone = ZoneKey(action.playerId, ZoneType.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, action.cardId)

        // Add controller component
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(ControllerComponent(action.playerId))
        }

        // Use up a land drop
        newState = newState.updateEntity(action.playerId) { c ->
            val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
            c.with(landDrops.use())
        }

        val zoneChangeEvent = ZoneChangeEvent(
            action.cardId,
            cardComponent.name,
            ZoneType.HAND,
            ZoneType.BATTLEFIELD,
            action.playerId
        )

        val events = listOf(zoneChangeEvent)
        newState = newState.tick()

        // Detect and process any triggers from the land entering (e.g., landfall)
        val triggers = triggerDetector.detectTriggers(newState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(newState, triggers)

            if (triggerResult.isPaused) {
                // A trigger requires target selection
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                events + triggerResult.events
            )
        }

        return ExecutionResult.success(newState, events)
    }

    private fun executeDeclareAttackers(state: GameState, action: DeclareAttackers): ExecutionResult {
        val result = combatManager.declareAttackers(state, action.playerId, action.attackers)

        if (!result.isSuccess) {
            return result
        }

        // Detect and process attack triggers (e.g., "when this creature attacks")
        val triggers = triggerDetector.detectTriggers(result.newState, result.events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(result.newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    result.events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                result.events + triggerResult.events
            )
        }

        return result
    }

    private fun executeDeclareBlockers(state: GameState, action: DeclareBlockers): ExecutionResult {
        val result = combatManager.declareBlockers(state, action.playerId, action.blockers)

        if (!result.isSuccess) {
            return result
        }

        // Detect and process block triggers (e.g., "when this creature blocks")
        val triggers = triggerDetector.detectTriggers(result.newState, result.events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(result.newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    result.events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                result.events + triggerResult.events
            )
        }

        return result
    }

    private fun executeOrderBlockers(state: GameState, action: OrderBlockers): ExecutionResult {
        val attackerId = action.attackerId

        // Validate the attacker exists and is actually blocked
        val attackerContainer = state.getEntity(attackerId)
            ?: return ExecutionResult.error(state, "Attacker not found: $attackerId")

        val blockedComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()
            ?: return ExecutionResult.error(state, "Creature is not blocked")

        // Validate all ordered blockers are actually blocking this attacker
        val actualBlockers = blockedComponent.blockerIds.toSet()
        val orderedBlockers = action.orderedBlockers.toSet()

        if (actualBlockers != orderedBlockers) {
            return ExecutionResult.error(
                state,
                "Ordered blockers must contain exactly the creatures blocking this attacker"
            )
        }

        // Store the damage assignment order on the attacker
        val newState = state.updateEntity(attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(
                    action.orderedBlockers
                )
            )
        }

        return ExecutionResult.success(
            newState,
            listOf(BlockerOrderDeclaredEvent(attackerId, action.orderedBlockers))
        )
    }

    private fun executeMakeChoice(state: GameState, action: MakeChoice): ExecutionResult {
        // TODO: Handle modal choices, card selections, etc.
        return ExecutionResult.success(state)
    }

    private fun executeSelectTargets(state: GameState, action: SelectTargets): ExecutionResult {
        // TODO: Handle target selection for triggered abilities
        return ExecutionResult.success(state)
    }

    private fun executeChooseManaColor(state: GameState, action: ChooseManaColor): ExecutionResult {
        // Add mana of the chosen color to the player's mana pool
        var newState = state.updateEntity(action.playerId) { c ->
            val manaPool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
            c.with(manaPool.add(action.color))
        }
        return ExecutionResult.success(newState)
    }

    private fun executeConcede(state: GameState, action: Concede): ExecutionResult {
        val opponent = state.getOpponent(action.playerId)
        return ExecutionResult.success(
            state.copy(gameOver = true, winnerId = opponent),
            listOf(
                PlayerLostEvent(action.playerId, GameEndReason.CONCESSION),
                GameEndedEvent(opponent, GameEndReason.CONCESSION)
            )
        )
    }

    private fun resolveTopOfStack(state: GameState): ExecutionResult {
        val result = stackResolver.resolveTop(state)

        if (!result.isSuccess) {
            return result
        }

        // Check state-based actions after resolution (Rule 704.3)
        // This handles lethal damage, life <= 0, etc.
        val sbaResult = sbaChecker.checkAndApply(result.newState)
        var combinedEvents = result.events + sbaResult.events

        // If game is over, don't give priority
        if (sbaResult.newState.gameOver) {
            return ExecutionResult.success(sbaResult.newState, combinedEvents)
        }

        // Detect and process triggers from the resolution events AND SBA events (Rule 603.2)
        // SBA events include death triggers when creatures die from lethal damage
        val triggers = triggerDetector.detectTriggers(sbaResult.newState, combinedEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(sbaResult.newState, triggers)

            if (triggerResult.isPaused) {
                // A trigger requires target selection - return paused result
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    combinedEvents + triggerResult.events
                )
            }

            combinedEvents = combinedEvents + triggerResult.events

            // After triggers are on stack, active player gets priority
            return ExecutionResult.success(
                triggerResult.newState.withPriority(state.activePlayerId),
                combinedEvents
            )
        }

        // After resolution, active player gets priority
        return ExecutionResult.success(
            sbaResult.newState.withPriority(state.activePlayerId),
            combinedEvents
        )
    }

    private fun advanceGame(state: GameState): ExecutionResult {
        return turnManager.advanceStep(state)
    }

    // =========================================================================
    // Decision Handling
    // =========================================================================

    private fun validateSubmitDecision(state: GameState, action: SubmitDecision): String? {
        val pending = state.pendingDecision
            ?: return "No pending decision to respond to"

        if (pending.playerId != action.playerId) {
            return "You are not the player who needs to make this decision"
        }

        if (pending.id != action.response.decisionId) {
            return "Decision ID mismatch: expected ${pending.id}, got ${action.response.decisionId}"
        }

        // Validate the response matches the decision type
        return validateDecisionResponse(pending, action.response)
    }

    private fun validateDecisionResponse(decision: PendingDecision, response: DecisionResponse): String? {
        return when (decision) {
            is ChooseTargetsDecision -> {
                if (response !is TargetsResponse) {
                    return "Expected target selection response"
                }
                // Validate all selected targets are in the legal targets list
                for ((reqIndex, selectedIds) in response.selectedTargets) {
                    val legalForReq = decision.legalTargets[reqIndex] ?: emptyList()
                    for (id in selectedIds) {
                        if (id !in legalForReq) {
                            return "Invalid target: $id is not a legal choice for requirement $reqIndex"
                        }
                    }
                    // Check min/max constraints
                    val req = decision.targetRequirements.find { it.index == reqIndex }
                    if (req != null) {
                        if (selectedIds.size < req.minTargets) {
                            return "Not enough targets for requirement $reqIndex: need at least ${req.minTargets}"
                        }
                        if (selectedIds.size > req.maxTargets) {
                            return "Too many targets for requirement $reqIndex: maximum is ${req.maxTargets}"
                        }
                    }
                }
                null
            }
            is SelectCardsDecision -> {
                if (response !is CardsSelectedResponse) {
                    return "Expected card selection response"
                }
                // Validate all selected cards are in the options
                for (cardId in response.selectedCards) {
                    if (cardId !in decision.options) {
                        return "Invalid selection: $cardId is not a valid option"
                    }
                }
                if (response.selectedCards.size < decision.minSelections) {
                    return "Not enough cards selected: need at least ${decision.minSelections}"
                }
                if (response.selectedCards.size > decision.maxSelections) {
                    return "Too many cards selected: maximum is ${decision.maxSelections}"
                }
                null
            }
            is YesNoDecision -> {
                if (response !is YesNoResponse) {
                    return "Expected yes/no response"
                }
                null
            }
            is ChooseModeDecision -> {
                if (response !is ModesChosenResponse) {
                    return "Expected mode selection response"
                }
                // Validate all selected modes exist and are available
                for (modeIndex in response.selectedModes) {
                    val mode = decision.modes.find { it.index == modeIndex }
                    if (mode == null) {
                        return "Invalid mode index: $modeIndex"
                    }
                    if (!mode.available) {
                        return "Mode $modeIndex is not available"
                    }
                }
                if (response.selectedModes.size < decision.minModes) {
                    return "Not enough modes selected: need at least ${decision.minModes}"
                }
                if (response.selectedModes.size > decision.maxModes) {
                    return "Too many modes selected: maximum is ${decision.maxModes}"
                }
                null
            }
            is ChooseColorDecision -> {
                if (response !is ColorChosenResponse) {
                    return "Expected color choice response"
                }
                if (response.color !in decision.availableColors) {
                    return "Invalid color: ${response.color} is not available"
                }
                null
            }
            is ChooseNumberDecision -> {
                if (response !is NumberChosenResponse) {
                    return "Expected number choice response"
                }
                if (response.number < decision.minValue || response.number > decision.maxValue) {
                    return "Invalid number: must be between ${decision.minValue} and ${decision.maxValue}"
                }
                null
            }
            is DistributeDecision -> {
                if (response !is DistributionResponse) {
                    return "Expected distribution response"
                }
                val total = response.distribution.values.sum()
                if (total != decision.totalAmount) {
                    return "Distribution must total ${decision.totalAmount}, got $total"
                }
                for ((targetId, amount) in response.distribution) {
                    if (targetId !in decision.targets) {
                        return "Invalid target for distribution: $targetId"
                    }
                    if (amount < decision.minPerTarget) {
                        return "Each target must receive at least ${decision.minPerTarget}"
                    }
                }
                null
            }
            is OrderObjectsDecision -> {
                if (response !is OrderedResponse) {
                    return "Expected ordering response"
                }
                if (response.orderedObjects.toSet() != decision.objects.toSet()) {
                    return "Ordered objects must contain exactly the same objects as the decision"
                }
                null
            }
            is SplitPilesDecision -> {
                if (response !is PilesSplitResponse) {
                    return "Expected pile split response"
                }
                val allCards = response.piles.flatten().toSet()
                if (allCards != decision.cards.toSet()) {
                    return "Piles must contain exactly the same cards as the decision"
                }
                if (response.piles.size != decision.numberOfPiles) {
                    return "Must split into exactly ${decision.numberOfPiles} piles"
                }
                null
            }
            is ChooseOptionDecision -> {
                if (response !is OptionChosenResponse) {
                    return "Expected option choice response"
                }
                if (response.optionIndex < 0 || response.optionIndex >= decision.options.size) {
                    return "Invalid option index: ${response.optionIndex}"
                }
                null
            }
            is AssignDamageDecision -> {
                if (response !is DamageAssignmentResponse) {
                    return "Expected damage assignment response"
                }
                // Validate total damage doesn't exceed available power
                val totalDamage = response.assignments.values.sum()
                if (totalDamage > decision.availablePower) {
                    return "Total damage ($totalDamage) exceeds available power (${decision.availablePower})"
                }
                // Validate all targets are valid
                val validTargets = decision.orderedTargets.toSet() + listOfNotNull(decision.defenderId)
                for (targetId in response.assignments.keys) {
                    if (targetId !in validTargets) {
                        return "Invalid damage target: $targetId"
                    }
                }
                // Validate damage assignment order (each blocker must have lethal before next)
                var allPreviousHaveLethal = true
                for (blockerId in decision.orderedTargets) {
                    val assignedDamage = response.assignments[blockerId] ?: 0
                    val lethalRequired = decision.minimumAssignments[blockerId] ?: 0
                    val hasLethal = assignedDamage >= lethalRequired

                    if (!hasLethal && !allPreviousHaveLethal) {
                        // This is fine - we can assign 0 to later blockers
                    } else if (!hasLethal) {
                        allPreviousHaveLethal = false
                    }

                    // Check that no subsequent blocker has damage if this one doesn't have lethal
                    if (!hasLethal) {
                        val laterBlockers = decision.orderedTargets.dropWhile { it != blockerId }.drop(1)
                        for (laterBlocker in laterBlockers) {
                            if ((response.assignments[laterBlocker] ?: 0) > 0) {
                                return "Cannot assign damage to later blocker until earlier blockers have lethal damage"
                            }
                        }
                    }
                }
                // Validate trample damage only if all blockers have lethal
                val damageToDefender = response.assignments[decision.defenderId] ?: 0
                if (damageToDefender > 0) {
                    if (!decision.hasTrample) {
                        return "Cannot assign damage to defending player without trample"
                    }
                    if (!allPreviousHaveLethal) {
                        return "Cannot assign trample damage until all blockers have lethal damage"
                    }
                }
                null
            }
            is SearchLibraryDecision -> {
                if (response !is CardsSelectedResponse) {
                    return "Expected card selection response for library search"
                }
                // Validate selected cards are in the options
                for (cardId in response.selectedCards) {
                    if (cardId !in decision.options) {
                        return "Invalid selection: $cardId is not a valid option"
                    }
                }
                if (response.selectedCards.size > decision.maxSelections) {
                    return "Too many cards selected: maximum is ${decision.maxSelections}"
                }
                null
            }
            is ReorderLibraryDecision -> {
                if (response !is OrderedResponse) {
                    return "Expected ordered response for library reorder"
                }
                // Validate the response contains exactly the same cards
                val expectedSet = decision.cards.toSet()
                val responseSet = response.orderedObjects.toSet()
                if (expectedSet != responseSet) {
                    return "Invalid reorder: response must contain the same cards"
                }
                if (response.orderedObjects.size != decision.cards.size) {
                    return "Invalid reorder: response must contain exactly ${decision.cards.size} cards"
                }
                null
            }
        }
    }

    private fun executeSubmitDecision(state: GameState, action: SubmitDecision): ExecutionResult {
        val pending = state.pendingDecision
            ?: return ExecutionResult.error(state, "No pending decision")

        // Clear the pending decision from state
        val clearedState = state.clearPendingDecision()

        // Emit event that decision was submitted
        val submittedEvent = DecisionSubmittedEvent(pending.id, action.playerId)

        // Check if there's a continuation frame to process
        val hasContinuation = clearedState.peekContinuation() != null

        if (hasContinuation) {
            // Resume execution using the continuation handler
            val result = continuationHandler.resume(clearedState, action.response)

            // If successful and we're in cleanup step with no more decisions,
            // advance to complete cleanup and end the turn
            if (result.isSuccess && !result.isPaused &&
                result.state.step == Step.CLEANUP &&
                result.state.pendingDecision == null
            ) {
                val cleanupAdvanceResult = advanceGame(result.state)
                return ExecutionResult(
                    state = cleanupAdvanceResult.state,
                    events = listOf(submittedEvent) + result.events + cleanupAdvanceResult.events,
                    error = cleanupAdvanceResult.error,
                    pendingDecision = cleanupAdvanceResult.pendingDecision
                )
            }

            // Prepend the decision submitted event
            return if (result.isSuccess || result.isPaused) {
                ExecutionResult(
                    state = result.state,
                    events = listOf(submittedEvent) + result.events,
                    error = result.error,
                    pendingDecision = result.pendingDecision
                )
            } else {
                result
            }
        }

        // No continuation - just return with cleared state
        // This handles legacy decisions that don't use the continuation system yet
        return ExecutionResult.success(clearedState, listOf(submittedEvent))
    }

    // =========================================================================
    // Mulligan Actions
    // =========================================================================

    private fun executeTakeMulligan(state: GameState, action: TakeMulligan): ExecutionResult {
        return when (val result = mulliganHandler.handleTakeMulligan(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    private fun executeKeepHand(state: GameState, action: KeepHand): ExecutionResult {
        return when (val result = mulliganHandler.handleKeepHand(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    private fun executeBottomCards(state: GameState, action: BottomCards): ExecutionResult {
        return when (val result = mulliganHandler.handleBottomCards(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    /**
     * Check if all mulligans are complete and advance the game to the first turn if so.
     */
    private fun checkMulliganCompletion(state: GameState, events: List<GameEvent>): ExecutionResult {
        // If still in mulligan phase or someone needs to bottom cards, just return success
        if (mulliganHandler.isInMulliganPhase(state) || mulliganHandler.needsBottomCards(state)) {
            return ExecutionResult.success(state, events)
        }

        // All mulligans complete - start the first turn
        // The game starts at UNTAP step, so advance to process the untap and get to upkeep
        val advanceResult = turnManager.advanceStep(state)
        return ExecutionResult.success(
            advanceResult.newState,
            events + advanceResult.events
        )
    }
}
