package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.mechanics.text.SubtypeReplacer
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.AddManaEffect
import com.wingedsheep.engine.handlers.CostPaymentChoices
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import kotlin.reflect.KClass

/**
 * Handler for the ActivateAbility action.
 *
 * Handles activating abilities on permanents, including:
 * - Mana abilities (immediate resolution)
 * - Non-mana abilities (go on stack)
 * - Planeswalker loyalty abilities
 */
class ActivateAbilityHandler(
    private val cardRegistry: CardRegistry?,
    private val turnManager: TurnManager,
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val stateProjector: StateProjector = StateProjector()
) : ActionHandler<ActivateAbility> {
    override val actionType: KClass<ActivateAbility> = ActivateAbility::class

    override fun validate(state: GameState, action: ActivateAbility): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Source is not a card"

        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        // Look up ability from card definition or granted activated abilities
        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: return "Ability not found on this card"

        // Check that the card is in the correct zone for this ability
        if (ability.activateFromZone != Zone.BATTLEFIELD) {
            val ownerId = container.get<OwnerComponent>()?.playerId ?: return "Card has no owner"
            val inZone = state.getZone(ownerId, ability.activateFromZone).contains(action.sourceId)
            if (!inZone) return "This ability can only be activated from the ${ability.activateFromZone.name.lowercase()}"
            if (ownerId != action.playerId) return "You don't own this card"
        } else {
            // Use projected controller to account for control-changing effects (e.g., Annex)
            val projected = stateProjector.project(state)
            val controller = projected.getController(action.sourceId)
                ?: container.get<ControllerComponent>()?.playerId
            if (controller != action.playerId) {
                return "You don't control this permanent"
            }

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) {
                return "Face-down creatures have no abilities"
            }
        }

        // Apply text-changing effects to cost and target filters
        val textReplacement = container.get<TextReplacementComponent>()
        val effectiveCost = if (textReplacement != null) {
            SubtypeReplacer.replaceAbilityCost(ability.cost, textReplacement)
        } else {
            ability.cost
        }
        val effectiveTargetReq = if (textReplacement != null && ability.targetRequirement != null) {
            SubtypeReplacer.replaceTargetRequirement(ability.targetRequirement!!, textReplacement)
        } else {
            ability.targetRequirement
        }

        // Check timing for planeswalker abilities
        if (ability.isPlaneswalkerAbility) {
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "Loyalty abilities can only be activated at sorcery speed"
            }
        }

        // Check timing for sorcery-speed abilities ("Activate only as a sorcery")
        if (ability.timing == TimingRule.SorcerySpeed && !ability.isPlaneswalkerAbility) {
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "This ability can only be activated as a sorcery"
            }
        }

        // Check summoning sickness for TapAttachedCreature cost (before general cost check
        // to give a specific error message)
        if (effectiveCost is AbilityCost.TapAttachedCreature ||
            (effectiveCost is AbilityCost.Composite && effectiveCost.costs.any { it is AbilityCost.TapAttachedCreature })) {
            val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
            if (attachedId != null) {
                val attachedContainer = state.getEntity(attachedId)
                val attachedCard = attachedContainer?.get<CardComponent>()
                if (attachedCard != null && attachedCard.typeLine.isCreature) {
                    val hasSummoningSickness = attachedContainer.has<SummoningSicknessComponent>()
                    val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) {
                        return "Enchanted creature has summoning sickness"
                    }
                }
            }
        }

        // Check cost requirements (using ManaSolver for mana costs to consider untapped sources)
        if (!canPayAbilityCostWithSources(state, effectiveCost, action.sourceId, action.playerId)) {
            return when (effectiveCost) {
                is AbilityCost.Tap -> "This permanent is already tapped"
                is AbilityCost.TapAttachedCreature -> "Enchanted creature is tapped"
                is AbilityCost.Loyalty -> {
                    if (effectiveCost.change < 0) {
                        "Not enough loyalty to activate this ability"
                    } else {
                        "Cannot pay loyalty cost"
                    }
                }
                is AbilityCost.Mana -> "Not enough mana to activate this ability"
                is AbilityCost.PayLife -> "Not enough life to activate this ability"
                else -> "Cannot pay ability cost"
            }
        }

        // Check summoning sickness for tap abilities
        if (effectiveCost is AbilityCost.Tap ||
            (effectiveCost is AbilityCost.Composite && effectiveCost.costs.any { it is AbilityCost.Tap })) {
            if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                if (hasSummoningSickness && !hasHaste) {
                    return "This creature has summoning sickness"
                }
            }
        }

        // Check activation restrictions
        for (restriction in ability.restrictions) {
            val error = checkActivationRestriction(state, action.playerId, restriction)
            if (error != null) return error
        }

        // Validate targets
        if (effectiveTargetReq != null && action.targets.isNotEmpty()) {
            val targetError = targetValidator.validateTargets(
                state,
                action.targets,
                listOf(effectiveTargetReq),
                action.playerId,
                sourceColors = cardComponent.colors,
                sourceSubtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            )
            if (targetError != null) {
                return targetError
            }
        } else if (effectiveTargetReq != null && action.targets.isEmpty()) {
            return "This ability requires a target"
        }

        return null
    }

    override fun execute(state: GameState, action: ActivateAbility): ExecutionResult {
        val container = state.getEntity(action.sourceId)
            ?: return ExecutionResult.error(state, "Source not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card")

        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        // Look up ability from card definition or granted activated abilities
        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: return ExecutionResult.error(state, "Ability not found")

        // Apply text-changing effects to cost
        val textReplacement = container.get<TextReplacementComponent>()
        val effectiveCost = if (textReplacement != null) {
            SubtypeReplacer.replaceAbilityCost(ability.cost, textReplacement)
        } else {
            ability.cost
        }

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Get player's mana pool
        val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        var manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        // Auto-tap lands for mana costs before paying
        val manaCost = extractManaCost(effectiveCost)
        val xValue = action.xValue ?: 0
        if (manaCost != null) {
            val autoTapResult = autoTapForManaCost(currentState, action.playerId, manaPool, manaCost, cardComponent.name, xValue)
                ?: return ExecutionResult.error(state, "Not enough mana to activate this ability")
            currentState = autoTapResult.newState
            manaPool = autoTapResult.newPool
            events.addAll(autoTapResult.events)
        }

        // Build cost payment choices from the action
        val costChoices = CostPaymentChoices(
            sacrificeChoices = action.costPayment?.sacrificedPermanents ?: emptyList(),
            discardChoices = action.costPayment?.discardedCards ?: emptyList(),
            exileChoices = action.costPayment?.exiledCards ?: emptyList(),
            tapChoices = action.costPayment?.tappedPermanents ?: emptyList()
        )

        // Pay the cost (using effective cost with text replacements applied)
        val costResult = costHandler.payAbilityCost(
            currentState,
            effectiveCost,
            action.sourceId,
            action.playerId,
            manaPool,
            costChoices
        )

        if (!costResult.success) {
            return ExecutionResult.error(state, costResult.error ?: "Failed to pay ability cost")
        }

        currentState = costResult.newState!!
        manaPool = costResult.newManaPool!!

        // Collect events from cost payment (e.g., sacrifice events)
        events.addAll(costResult.events)

        // Always update mana pool on state after cost payment.
        // autoTapForManaCost writes the enriched (pre-payment) pool to state,
        // so we must unconditionally write the post-payment pool.
        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(ManaPoolComponent(
                white = manaPool.white,
                blue = manaPool.blue,
                black = manaPool.black,
                red = manaPool.red,
                green = manaPool.green,
                colorless = manaPool.colorless
            ))
        }

        // Emit events for cost types
        when (ability.cost) {
            is AbilityCost.Tap -> {
                events.add(TappedEvent(action.sourceId, cardComponent.name))
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                if (attachedId != null) {
                    val attachedName = currentState.getEntity(attachedId)?.get<CardComponent>()?.name ?: "Unknown"
                    events.add(TappedEvent(attachedId, attachedName))
                }
            }
            is AbilityCost.Loyalty -> {
                val loyaltyCost = ability.cost as AbilityCost.Loyalty
                events.add(LoyaltyChangedEvent(action.sourceId, cardComponent.name, loyaltyCost.change))
            }
            else -> {}
        }

        // Apply text replacement if the source has a TextReplacementComponent
        val finalEffect = if (textReplacement != null) {
            SubtypeReplacer.replaceEffect(ability.effect, textReplacement)
        } else {
            ability.effect
        }

        // Mana abilities don't use the stack
        if (ability.isManaAbility) {
            val opponentId = state.turnOrder.firstOrNull { it != action.playerId }
            val context = EffectContext(
                sourceId = action.sourceId,
                controllerId = action.playerId,
                opponentId = opponentId,
                targets = action.targets,
                xValue = null,
                manaColorChoice = action.manaColorChoice
            )

            val effectResult = effectExecutorRegistry.execute(currentState, finalEffect, context)
            if (!effectResult.isSuccess) {
                return effectResult
            }

            currentState = effectResult.newState

            // Emit ManaAddedEvent
            val manaEvent = when (val effect = finalEffect) {
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
                is AddAnyColorManaEffect -> {
                    val chosenColor = action.manaColorChoice ?: Color.GREEN
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        white = if (chosenColor == Color.WHITE) effect.amount else 0,
                        blue = if (chosenColor == Color.BLUE) effect.amount else 0,
                        black = if (chosenColor == Color.BLACK) effect.amount else 0,
                        red = if (chosenColor == Color.RED) effect.amount else 0,
                        green = if (chosenColor == Color.GREEN) effect.amount else 0,
                        colorless = 0
                    )
                }
                else -> null
            }

            if (manaEvent != null) {
                events.add(manaEvent)
            }

            return ExecutionResult.success(currentState, events + effectResult.events)
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = finalEffect,
            sacrificedPermanents = action.costPayment?.sacrificedPermanents ?: emptyList(),
            xValue = action.xValue
        )

        // Apply text-changing effects to the target requirement for resolution-time re-validation
        val effectiveTargetReq = if (textReplacement != null && ability.targetRequirement != null) {
            SubtypeReplacer.replaceTargetRequirement(ability.targetRequirement!!, textReplacement)
        } else {
            ability.targetRequirement
        }

        val stackResult = stackResolver.putActivatedAbility(
            currentState, abilityOnStack, action.targets,
            targetRequirements = listOfNotNull(effectiveTargetReq)
        )
        val allEvents = events + stackResult.events

        // Detect and process triggers from cost payment (e.g., sacrifice death triggers)
        val triggers = triggerDetector.detectTriggers(stackResult.newState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(stackResult.newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents + triggerResult.events
            )
        }

        return ExecutionResult.success(stackResult.newState, allEvents)
    }

    /**
     * Check if an ability cost can be paid, using ManaSolver for mana costs
     * to consider both floating mana and untapped mana sources.
     */
    private fun canPayAbilityCostWithSources(
        state: GameState,
        cost: AbilityCost,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        playerId: com.wingedsheep.sdk.model.EntityId
    ): Boolean {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )
        return when (cost) {
            is AbilityCost.Mana -> manaSolver.canPay(state, playerId, cost.cost)
            is AbilityCost.Composite -> {
                cost.costs.all { subCost ->
                    when (subCost) {
                        is AbilityCost.Mana -> manaSolver.canPay(state, playerId, subCost.cost)
                        else -> costHandler.canPayAbilityCost(state, subCost, sourceId, playerId, manaPool)
                    }
                }
            }
            else -> costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool)
        }
    }

    /**
     * Extract the ManaCost from an ability cost, if present.
     */
    private fun extractManaCost(cost: AbilityCost): ManaCost? = when (cost) {
        is AbilityCost.Mana -> cost.cost
        is AbilityCost.Composite -> cost.costs.filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
        else -> null
    }

    private data class AutoTapResult(
        val newState: GameState,
        val newPool: ManaPool,
        val events: List<GameEvent>
    )

    /**
     * Auto-tap mana sources to cover a mana cost that can't be fully paid from the floating pool.
     * Taps sources for the shortfall and adds their mana to the pool so costHandler can consume it.
     * Returns null if the cost cannot be paid.
     */
    private fun autoTapForManaCost(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        pool: ManaPool,
        cost: ManaCost,
        sourceName: String,
        xValue: Int = 0
    ): AutoTapResult? {
        // Determine what the floating pool can cover
        val partialResult = pool.payPartial(cost)
        val remainingCost = partialResult.remainingCost

        // If floating pool covers everything (and no X to pay), no tapping needed
        if (remainingCost.isEmpty() && xValue == 0) {
            return AutoTapResult(state, pool, emptyList())
        }

        // Tap sources for the remaining cost (xValue is treated as additional generic mana)
        val solution = manaSolver.solve(state, playerId, remainingCost, xValue)
            ?: return null

        var currentState = state
        var currentPool = pool
        val events = mutableListOf<GameEvent>()

        for (source in solution.sources) {
            currentState = currentState.updateEntity(source.entityId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
            }
            events.add(TappedEvent(source.entityId, source.name))
        }

        // Add produced mana to floating pool so costHandler.payAbilityCost can consume it
        for ((_, production) in solution.manaProduced) {
            currentPool = if (production.color != null) {
                currentPool.add(production.color)
            } else {
                currentPool.addColorless(production.colorless)
            }
        }

        // Update state with enriched pool
        currentState = currentState.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = currentPool.white,
                blue = currentPool.blue,
                black = currentPool.black,
                red = currentPool.red,
                green = currentPool.green,
                colorless = currentPool.colorless
            ))
        }

        return AutoTapResult(currentState, currentPool, events)
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
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

    companion object {
        fun create(context: ActionContext): ActivateAbilityHandler {
            return ActivateAbilityHandler(
                context.cardRegistry,
                context.turnManager,
                context.costHandler,
                context.manaSolver,
                context.effectExecutorRegistry,
                context.stackResolver,
                context.targetValidator,
                context.conditionEvaluator,
                context.triggerDetector,
                context.triggerProcessor,
                context.stateProjector
            )
        }
    }
}
