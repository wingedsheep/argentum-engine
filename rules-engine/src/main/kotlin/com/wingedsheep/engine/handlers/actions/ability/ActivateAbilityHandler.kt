package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.AddManaEffect
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
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator
) : ActionHandler<ActivateAbility> {
    override val actionType: KClass<ActivateAbility> = ActivateAbility::class

    override fun validate(state: GameState, action: ActivateAbility): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != action.playerId) {
            return "You don't control this permanent"
        }

        val cardComponent = container.get<CardComponent>()
            ?: return "Source is not a card"

        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: return "Ability not found on this card"

        // Check timing for planeswalker abilities
        if (ability.isPlaneswalkerAbility) {
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "Loyalty abilities can only be activated at sorcery speed"
            }
        }

        // Get player's mana pool for cost validation
        val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        // Check cost requirements
        if (!costHandler.canPayAbilityCost(state, ability.cost, action.sourceId, action.playerId, manaPool)) {
            return when (ability.cost) {
                is AbilityCost.Tap -> "This permanent is already tapped"
                is AbilityCost.Loyalty -> {
                    val cost = ability.cost as AbilityCost.Loyalty
                    if (cost.change < 0) {
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
        if (ability.cost is AbilityCost.Tap ||
            (ability.cost is AbilityCost.Composite && (ability.cost as AbilityCost.Composite).costs.any { it is AbilityCost.Tap })) {
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
        if (ability.targetRequirement != null && action.targets.isNotEmpty()) {
            val targetError = targetValidator.validateTargets(
                state,
                action.targets,
                listOf(ability.targetRequirement!!),
                action.playerId
            )
            if (targetError != null) {
                return targetError
            }
        } else if (ability.targetRequirement != null && action.targets.isEmpty()) {
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

        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
            ?: return ExecutionResult.error(state, "Ability not found")

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

        // Pay the cost
        val costResult = costHandler.payAbilityCost(
            currentState,
            ability.cost,
            action.sourceId,
            action.playerId,
            manaPool
        )

        if (!costResult.success) {
            return ExecutionResult.error(state, costResult.error ?: "Failed to pay ability cost")
        }

        currentState = costResult.newState!!
        manaPool = costResult.newManaPool!!

        // Update mana pool if changed
        if (manaPool != ManaPool(poolComponent.white, poolComponent.blue, poolComponent.black, poolComponent.red, poolComponent.green, poolComponent.colorless)) {
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
        }

        // Emit events for cost types
        when (ability.cost) {
            is AbilityCost.Tap -> {
                events.add(TappedEvent(action.sourceId, cardComponent.name))
            }
            is AbilityCost.Loyalty -> {
                val loyaltyCost = ability.cost as AbilityCost.Loyalty
                events.add(LoyaltyChangedEvent(action.sourceId, cardComponent.name, loyaltyCost.change))
            }
            else -> {}
        }

        // Mana abilities don't use the stack
        if (ability.isManaAbility) {
            val opponentId = state.turnOrder.firstOrNull { it != action.playerId }
            val context = EffectContext(
                sourceId = action.sourceId,
                controllerId = action.playerId,
                opponentId = opponentId,
                targets = action.targets,
                xValue = null
            )

            val effectResult = effectExecutorRegistry.execute(currentState, ability.effect, context)
            if (!effectResult.isSuccess) {
                return effectResult
            }

            currentState = effectResult.newState

            // Emit ManaAddedEvent
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
            effect = ability.effect
        )
        val stackResult = stackResolver.putActivatedAbility(currentState, abilityOnStack, action.targets)
        return ExecutionResult.success(stackResult.newState, events + stackResult.events)
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
                context.effectExecutorRegistry,
                context.stackResolver,
                context.targetValidator,
                context.conditionEvaluator
            )
        }
    }
}
