package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToAttachedCreature
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.AdditionalManaOnLandTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
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
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
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

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val ability = cardDef.script.effectiveActivatedAbilities(classLevel).find { it.id == action.abilityId }
            ?: findClassLevelUpAbility(cardDef, container, action.abilityId)
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: getStaticGrantedActivatedAbilities(action.sourceId, state)
                .find { it.id == action.abilityId }
            ?: return "Ability not found on this card"

        // Check that the card is in the correct zone for this ability
        if (ability.activateFromZone != Zone.BATTLEFIELD) {
            val ownerId = container.get<OwnerComponent>()?.playerId ?: return "Card has no owner"
            val inZone = state.getZone(ownerId, ability.activateFromZone).contains(action.sourceId)
            if (!inZone) return "This ability can only be activated from the ${ability.activateFromZone.name.lowercase()}"
            if (ownerId != action.playerId) return "You don't own this card"
        } else {
            // Check if any player may activate this ability (e.g., Lethal Vapors)
            val anyPlayerMay = ability.restrictions.any { it is ActivationRestriction.AnyPlayerMay }

            if (!anyPlayerMay) {
                // Use projected controller to account for control-changing effects (e.g., Annex)
                val projected = state.projectedState
                val controller = projected.getController(action.sourceId)
                    ?: container.get<ControllerComponent>()?.playerId
                if (controller != action.playerId) {
                    return "You don't control this permanent"
                }
            }

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) {
                return "Face-down creatures have no abilities"
            }

            // Creatures that have lost all abilities cannot activate them (e.g., Deep Freeze)
            if (state.projectedState.hasLostAllAbilities(action.sourceId)) {
                // Only block the creature's own abilities, not granted ones
                val isOwnAbility = cardDef.script.effectiveActivatedAbilities(classLevel).any { it.id == action.abilityId }
                    || action.abilityId.value.startsWith("class_level_up_")
                if (isOwnAbility) {
                    return "This permanent has lost all abilities"
                }
            }
        }

        // Apply text-changing effects to cost and target filters
        val textReplacement = container.get<TextReplacementComponent>()
        val effectiveCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        val effectiveTargetReqs = if (textReplacement != null) {
            ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements
        }

        // Check timing for planeswalker abilities
        if (ability.isPlaneswalkerAbility) {
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "Loyalty abilities can only be activated at sorcery speed"
            }
            // Rule 606.3: Only one loyalty ability per planeswalker per turn
            // (Oath of Teferi allows two activations per turn)
            val tracker = container.get<AbilityActivatedThisTurnComponent>()
            if (tracker != null && tracker.loyaltyActivationCount > 0) {
                val maxActivations = getMaxLoyaltyActivations(state, action.playerId)
                if (tracker.hasReachedLoyaltyLimit(maxActivations)) {
                    return if (maxActivations > 1) {
                        "Loyalty abilities can only be activated $maxActivations times per planeswalker each turn"
                    } else {
                        "Only one loyalty ability can be activated per planeswalker each turn"
                    }
                }
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

        // Validate explicit payment sources
        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
            }
        }

        // Check cost requirements (using ManaSolver for mana costs to consider untapped sources)
        // If the ability has convoke and the player provided alternative payment, account for the reduced cost
        val costAfterConvokeReduction = if (ability.hasConvoke && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val mc = extractManaCost(effectiveCost) ?: effectiveCost
            if (mc is ManaCost || effectiveCost is AbilityCost.Mana || effectiveCost is AbilityCost.Composite) {
                val reducedManaCost = extractManaCost(effectiveCost)?.let {
                    alternativePaymentHandler.calculateReducedCostForAbility(it, action.alternativePayment)
                }
                if (reducedManaCost != null) {
                    when (effectiveCost) {
                        is AbilityCost.Mana -> AbilityCost.Mana(reducedManaCost)
                        is AbilityCost.Composite -> AbilityCost.Composite(effectiveCost.costs.map { subCost ->
                            if (subCost is AbilityCost.Mana) AbilityCost.Mana(reducedManaCost) else subCost
                        })
                        else -> effectiveCost
                    }
                } else effectiveCost
            } else effectiveCost
        } else effectiveCost

        if (action.paymentStrategy !is PaymentStrategy.Explicit && !canPayAbilityCostWithSources(state, costAfterConvokeReduction, action.sourceId, action.playerId)) {
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
            val error = checkActivationRestriction(state, action.playerId, action.sourceId, action.abilityId, restriction)
            if (error != null) return error
        }

        // Validate targets
        if (effectiveTargetReqs.isNotEmpty() && action.targets.isNotEmpty()) {
            val targetError = targetValidator.validateTargets(
                state,
                action.targets,
                effectiveTargetReqs,
                action.playerId,
                sourceColors = cardComponent.colors,
                sourceSubtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                sourceId = action.sourceId
            )
            if (targetError != null) {
                return targetError
            }
        } else if (effectiveTargetReqs.isNotEmpty() && action.targets.isEmpty()) {
            return "This ability requires a target"
        }

        return null
    }

    override fun execute(state: GameState, action: ActivateAbility): ExecutionResult {
        val container = state.getEntity(action.sourceId)
            ?: return ExecutionResult.error(state, "Source not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card")

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val ability = cardDef.script.effectiveActivatedAbilities(classLevel).find { it.id == action.abilityId }
            ?: findClassLevelUpAbility(cardDef, container, action.abilityId)
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: getStaticGrantedActivatedAbilities(action.sourceId, state)
                .find { it.id == action.abilityId }
            ?: return ExecutionResult.error(state, "Ability not found")

        // Apply text-changing effects to cost
        val textReplacement = container.get<TextReplacementComponent>()
        val effectiveCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
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

        // Pay mana costs before paying other costs
        var effectiveManaCost = extractManaCost(effectiveCost)
        val xValue = action.xValue ?: 0

        // Apply convoke payment for abilities with hasConvoke (e.g., Heirloom Epic)
        if (effectiveManaCost != null && ability.hasConvoke && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val convokeResult = alternativePaymentHandler.applyConvokeForAbility(
                currentState, effectiveManaCost, action.alternativePayment, action.playerId
            )
            effectiveManaCost = convokeResult.reducedCost
            currentState = convokeResult.newState
            events.addAll(convokeResult.events)
        }

        val manaCost = effectiveManaCost
        // Only pass xValue to auto-tap when X is in the mana cost itself (not in a non-mana cost like counter removal)
        val manaXValue = if (manaCost?.hasX == true) xValue else 0
        if (manaCost != null) {
            when (action.paymentStrategy) {
                is PaymentStrategy.Explicit -> {
                    // Tap only the minimum subset of chosen sources required to cover the
                    // (already convoke-reduced) mana cost — the client's auto-tap preview
                    // is computed against the full cost and may over-select. Solving with
                    // the non-chosen sources excluded matches the behavior in
                    // CastPaymentProcessor.explicitPay and keeps validation and execution
                    // in sync. Mana pool deduction is skipped by stripping the Mana cost
                    // below; tapping the solved subset is the payment.
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(currentState, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet()
                    val solution = manaSolver.solve(
                        currentState, action.playerId, manaCost, manaXValue, excludeSources = excluded
                    ) ?: return ExecutionResult.error(state, "Selected mana sources cannot pay this ability's cost")
                    for (source in solution.sources) {
                        val sourceName = currentState.getEntity(source.entityId)
                            ?.get<CardComponent>()?.name ?: source.name
                        currentState = currentState.updateEntity(source.entityId) { c ->
                            c.with(TappedComponent)
                        }
                        events.add(TappedEvent(source.entityId, sourceName))
                    }
                }
                else -> {
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, manaPool, manaCost, cardComponent.name, manaXValue)
                        ?: return ExecutionResult.error(state, "Not enough mana to activate this ability")
                    currentState = autoTapResult.newState
                    manaPool = autoTapResult.newPool
                    events.addAll(autoTapResult.events)
                }
            }
        }

        // Build cost payment choices from the action
        val costChoices = CostPaymentChoices(
            sacrificeChoices = action.costPayment?.sacrificedPermanents ?: emptyList(),
            discardChoices = action.costPayment?.discardedCards ?: emptyList(),
            exileChoices = action.costPayment?.exiledCards ?: emptyList(),
            tapChoices = action.costPayment?.tappedPermanents ?: emptyList(),
            bounceChoices = action.costPayment?.bouncedPermanents ?: emptyList(),
            xValue = xValue,
            counterRemovalChoices = action.costPayment?.counterRemovals ?: emptyMap()
        )

        // Snapshot projected subtypes of sacrifice targets before zone change
        val sacrificedPermanentSubtypes = mutableMapOf<EntityId, Set<String>>()
        val sacrificeTargetIds = action.costPayment?.sacrificedPermanents ?: emptyList()
        if (sacrificeTargetIds.isNotEmpty()) {
            val projectedBeforeSacrifice = currentState.projectedState
            for (permId in sacrificeTargetIds) {
                val projectedSubtypes = projectedBeforeSacrifice.getSubtypes(permId)
                if (projectedSubtypes.isNotEmpty()) {
                    sacrificedPermanentSubtypes[permId] = projectedSubtypes
                }
            }
        }

        // When using Explicit payment, mana sources were already tapped above —
        // strip the Mana portion so payAbilityCost doesn't try to deduct from the pool.
        // When convoke was applied, replace the mana portion with the reduced cost.
        val costForPayment = if (action.paymentStrategy is PaymentStrategy.Explicit) {
            stripManaCost(effectiveCost)
        } else if (ability.hasConvoke && action.alternativePayment != null && !action.alternativePayment.isEmpty && manaCost != null) {
            // Convoke reduced the mana cost — update the cost structure so payAbilityCost
            // deducts the reduced amount from the pool instead of the original full amount
            when (effectiveCost) {
                is AbilityCost.Mana -> AbilityCost.Mana(manaCost)
                is AbilityCost.Composite -> AbilityCost.Composite(effectiveCost.costs.map { subCost ->
                    if (subCost is AbilityCost.Mana) AbilityCost.Mana(manaCost) else subCost
                })
                else -> effectiveCost
            }
        } else {
            effectiveCost
        }

        // Pay the cost (using effective cost with text replacements applied)
        val costResult = costHandler.payAbilityCost(
            currentState,
            costForPayment,
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

        // Deduct X mana from the pool. ManaPool.pay() skips X symbols ("handled by caller"),
        // so we must explicitly spend the X portion here (same pattern as CastSpellHandler.autoPay).
        // Skip for Explicit payment — sources were already tapped to cover the full cost including X.
        if (action.paymentStrategy !is PaymentStrategy.Explicit && manaCost != null && manaCost.hasX && xValue > 0) {
            val xSymbolCount = manaCost.xCount.coerceAtLeast(1)
            var xRemainingToPay = xValue * xSymbolCount

            // Spend colorless first for X
            while (xRemainingToPay > 0 && manaPool.colorless > 0) {
                manaPool = manaPool.spendColorless()!!
                xRemainingToPay--
            }

            // Spend colored mana for remaining X
            for (color in Color.entries) {
                while (xRemainingToPay > 0 && manaPool.get(color) > 0) {
                    manaPool = manaPool.spend(color)!!
                    xRemainingToPay--
                }
            }
        }

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

        // Track once-per-turn activation if the ability has an OncePerTurn restriction
        if (ability.restrictions.any { it is ActivationRestriction.OncePerTurn || (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.OncePerTurn }) }) {
            // Only track if source is still on the battlefield (it might have been bounced as cost)
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track once-ever activation if the ability has an Once restriction
        if (ability.restrictions.any { it is ActivationRestriction.Once || (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.Once }) }) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedEverComponent>() ?: AbilityActivatedEverComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track planeswalker loyalty ability activation (Rule 606.3: once per planeswalker per turn)
        if (ability.isPlaneswalkerAbility) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withLoyaltyActivated())
                }
            }
        }

        // Apply text replacement if the source has a TextReplacementComponent
        val finalEffect = if (textReplacement != null) {
            ability.effect.applyTextReplacement(textReplacement)
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

            val effectResult = effectExecutorRegistry.execute(currentState, finalEffect, context).toExecutionResult()
            if (!effectResult.isSuccess) {
                return effectResult
            }

            currentState = effectResult.newState

            // Check for Damping Sphere-style mana dampening on lands
            var manaDampened = false
            if (cardComponent.typeLine.isLand && hasDampLandManaProduction(currentState)) {
                val oldPool = state.getEntity(action.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                val newPool = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                val totalManaProduced = (newPool.white - oldPool.white) +
                    (newPool.blue - oldPool.blue) +
                    (newPool.black - oldPool.black) +
                    (newPool.red - oldPool.red) +
                    (newPool.green - oldPool.green) +
                    (newPool.colorless - oldPool.colorless)

                if (totalManaProduced >= 2) {
                    // Replace with 1 colorless mana: revert to old pool + 1 colorless
                    val dampenedPool = ManaPoolComponent(
                        white = oldPool.white,
                        blue = oldPool.blue,
                        black = oldPool.black,
                        red = oldPool.red,
                        green = oldPool.green,
                        colorless = oldPool.colorless + 1
                    )
                    currentState = currentState.updateEntity(action.playerId) { container ->
                        container.with(dampenedPool)
                    }
                    manaDampened = true
                }
            }

            // Emit ManaAddedEvent — if dampened, always emit 1 colorless
            val manaEvent: ManaAddedEvent? = if (manaDampened) {
                ManaAddedEvent(
                    playerId = action.playerId,
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    colorless = 1
                )
            } else when (val effect = finalEffect) {
                is AddManaEffect -> {
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        white = if (effect.color == Color.WHITE) amount else 0,
                        blue = if (effect.color == Color.BLUE) amount else 0,
                        black = if (effect.color == Color.BLACK) amount else 0,
                        red = if (effect.color == Color.RED) amount else 0,
                        green = if (effect.color == Color.GREEN) amount else 0,
                        colorless = 0
                    )
                }
                is AddColorlessManaEffect -> {
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        colorless = amount
                    )
                }
                is AddAnyColorManaEffect -> {
                    val chosenColor = action.manaColorChoice ?: Color.GREEN
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        white = if (chosenColor == Color.WHITE) amount else 0,
                        blue = if (chosenColor == Color.BLUE) amount else 0,
                        black = if (chosenColor == Color.BLACK) amount else 0,
                        red = if (chosenColor == Color.RED) amount else 0,
                        green = if (chosenColor == Color.GREEN) amount else 0,
                        colorless = 0
                    )
                }
                is AddManaOfChosenColorEffect -> {
                    val chosenColor = state.getEntity(action.sourceId)
                        ?.get<com.wingedsheep.engine.state.components.identity.ChosenColorComponent>()?.color
                    if (chosenColor != null) {
                        val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                        ManaAddedEvent(
                            playerId = action.playerId,
                            sourceId = action.sourceId,
                            sourceName = cardComponent.name,
                            white = if (chosenColor == Color.WHITE) amount else 0,
                            blue = if (chosenColor == Color.BLUE) amount else 0,
                            black = if (chosenColor == Color.BLACK) amount else 0,
                            red = if (chosenColor == Color.RED) amount else 0,
                            green = if (chosenColor == Color.GREEN) amount else 0,
                            colorless = 0
                        )
                    } else {
                        null
                    }
                }
                is AddManaOfColorAmongEffect -> {
                    // Determine what color was actually added by comparing mana pools
                    val oldPool = state.getEntity(action.playerId)?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                    val newPool = currentState.getEntity(action.playerId)?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                    if (oldPool != null && newPool != null && oldPool != newPool) {
                        ManaAddedEvent(
                            playerId = action.playerId,
                            sourceId = action.sourceId,
                            sourceName = cardComponent.name,
                            white = newPool.white - oldPool.white,
                            blue = newPool.blue - oldPool.blue,
                            black = newPool.black - oldPool.black,
                            red = newPool.red - oldPool.red,
                            green = newPool.green - oldPool.green,
                            colorless = newPool.colorless - oldPool.colorless
                        )
                    } else if (oldPool == null && newPool != null) {
                        ManaAddedEvent(
                            playerId = action.playerId,
                            sourceId = action.sourceId,
                            sourceName = cardComponent.name,
                            white = newPool.white,
                            blue = newPool.blue,
                            black = newPool.black,
                            red = newPool.red,
                            green = newPool.green,
                            colorless = newPool.colorless
                        )
                    } else {
                        null
                    }
                }
                is CompositeEffect -> {
                    val anyColorEffect = effect.effects.filterIsInstance<AddAnyColorManaEffect>().firstOrNull()
                    if (anyColorEffect != null) {
                        val chosenColor = action.manaColorChoice ?: Color.GREEN
                        val amount = dynamicAmountEvaluator.evaluate(state, anyColorEffect.amount, context)
                        ManaAddedEvent(
                            playerId = action.playerId,
                            sourceId = action.sourceId,
                            sourceName = cardComponent.name,
                            white = if (chosenColor == Color.WHITE) amount else 0,
                            blue = if (chosenColor == Color.BLUE) amount else 0,
                            black = if (chosenColor == Color.BLACK) amount else 0,
                            red = if (chosenColor == Color.RED) amount else 0,
                            green = if (chosenColor == Color.GREEN) amount else 0,
                            colorless = 0
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (manaEvent != null) {
                events.add(manaEvent)
            }

            // Check for "additional mana on tap" auras (e.g., Elvish Guidance)
            // These are triggered mana abilities that resolve immediately
            val additionalManaResult = resolveAdditionalManaOnTap(
                currentState, action.sourceId, action.playerId, events + effectResult.events
            )
            currentState = additionalManaResult.state

            // Check for global "additional mana on land tap" abilities (e.g., Lavaleaper)
            // Triggered mana ability — resolves immediately, mirrors the color produced
            val onLandTapResult = resolveAdditionalManaOnLandTap(
                currentState, action.sourceId, action.playerId, manaEvent, additionalManaResult.events
            )
            currentState = onLandTapResult.state
            val allManaEvents = onLandTapResult.events

            return ExecutionResult.success(currentState, allManaEvents)
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = finalEffect,
            sacrificedPermanents = action.costPayment?.sacrificedPermanents ?: emptyList(),
            sacrificedPermanentSubtypes = sacrificedPermanentSubtypes,
            xValue = action.xValue,
            tappedPermanents = action.costPayment?.tappedPermanents ?: emptyList()
        )

        // Apply text-changing effects to the target requirements for resolution-time re-validation
        val effectiveTargetReqs = if (textReplacement != null) {
            ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements
        }

        var stackResult = stackResolver.putActivatedAbility(
            currentState, abilityOnStack, action.targets,
            targetRequirements = effectiveTargetReqs
        )
        currentState = stackResult.newState
        events.addAll(stackResult.events)

        // Handle repeated activations (repeatCount > 1)
        if (action.repeatCount > 1) {
            for (i in 2..action.repeatCount) {
                // Re-read mana pool from current state
                val repeatPoolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                var repeatPool = ManaPool(
                    white = repeatPoolComponent.white,
                    blue = repeatPoolComponent.blue,
                    black = repeatPoolComponent.black,
                    red = repeatPoolComponent.red,
                    green = repeatPoolComponent.green,
                    colorless = repeatPoolComponent.colorless
                )

                // Auto-tap for mana cost
                if (manaCost != null) {
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, repeatPool, manaCost, cardComponent.name, 0)
                        ?: break // Can't afford — stop early
                    currentState = autoTapResult.newState
                    repeatPool = autoTapResult.newPool
                    events.addAll(autoTapResult.events)
                }

                // Pay the cost
                val repeatCostResult = costHandler.payAbilityCost(
                    currentState, effectiveCost, action.sourceId, action.playerId, repeatPool, CostPaymentChoices()
                )
                if (!repeatCostResult.success) break // Can't pay — stop early

                currentState = repeatCostResult.newState!!
                repeatPool = repeatCostResult.newManaPool!!
                events.addAll(repeatCostResult.events)

                // Update mana pool on state
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(ManaPoolComponent(
                        white = repeatPool.white,
                        blue = repeatPool.blue,
                        black = repeatPool.black,
                        red = repeatPool.red,
                        green = repeatPool.green,
                        colorless = repeatPool.colorless
                    ))
                }

                // Put another ability on the stack
                val repeatAbilityOnStack = ActivatedAbilityOnStackComponent(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    controllerId = action.playerId,
                    effect = finalEffect,
                    sacrificedPermanents = emptyList(),
                    xValue = action.xValue,
                    tappedPermanents = emptyList()
                )
                val repeatStackResult = stackResolver.putActivatedAbility(
                    currentState, repeatAbilityOnStack, action.targets,
                    targetRequirements = effectiveTargetReqs
                )
                currentState = repeatStackResult.newState
                events.addAll(repeatStackResult.events)
            }
        }

        val allEvents = events.toList()

        // Detect and process triggers from cost payment (e.g., sacrifice death triggers)
        val triggers = triggerDetector.detectTriggers(currentState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)

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

        return ExecutionResult.success(currentState, allEvents)
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
                // If composite cost includes Tap, the source itself can't also be used as a mana source
                val hasTapCost = cost.costs.any { it is AbilityCost.Tap }
                val excludeSources = if (hasTapCost) setOf(sourceId) else emptySet()
                cost.costs.all { subCost ->
                    when (subCost) {
                        is AbilityCost.Mana -> manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeSources)
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
                currentPool.add(production.color, production.amount)
            } else {
                currentPool.addColorless(production.colorless)
            }
        }

        // Add only the bonus mana that wasn't consumed by the solver to the floating pool
        for ((color, amount) in solution.remainingBonusMana) {
            currentPool = currentPool.add(color, amount)
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

    /**
     * Strip the Mana portion from an ability cost — used when Explicit payment already
     * tapped the required sources, so the mana pool deduction should be skipped.
     */
    private fun stripManaCost(cost: AbilityCost): AbilityCost = when (cost) {
        is AbilityCost.Mana -> AbilityCost.Free
        is AbilityCost.Composite -> {
            val nonManaCosts = cost.costs.filter { it !is AbilityCost.Mana }
            when (nonManaCosts.size) {
                0 -> AbilityCost.Free
                1 -> nonManaCosts.single()
                else -> AbilityCost.Composite(nonManaCosts)
            }
        }
        else -> cost
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId,
        restriction: ActivationRestriction
    ): String? {
        return when (restriction) {
            is ActivationRestriction.AnyPlayerMay -> null // Not a restriction; handled in validate()
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
                    sourceId = sourceId,
                    controllerId = playerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = 0
                )
                if (!conditionEvaluator.evaluate(state, restriction.condition, context))
                    "Activation condition not met"
                else null
            }
            is ActivationRestriction.OncePerTurn -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                if (tracker != null && tracker.hasActivated(abilityId)) {
                    "This ability can only be activated once each turn"
                } else null
            }
            is ActivationRestriction.Once -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedEverComponent>()
                if (tracker != null && tracker.hasActivated(abilityId)) {
                    "This ability can only be activated once"
                } else null
            }
            is ActivationRestriction.All -> {
                restriction.restrictions.firstNotNullOfOrNull {
                    checkActivationRestriction(state, playerId, sourceId, abilityId, it)
                }
            }
        }
    }

    /**
     * After a mana ability resolves on a permanent, check for auras attached to it
     * that have AdditionalManaOnTap (e.g., Elvish Guidance). These are triggered mana
     * abilities that resolve immediately without using the stack.
     */
    private data class AdditionalManaResult(
        val state: GameState,
        val events: List<GameEvent>
    )

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Check if any permanent on the battlefield has DampLandManaProduction static ability.
     */
    private fun hasDampLandManaProduction(state: GameState): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                if (cardDef.script.staticAbilities.any { it is DampLandManaProduction }) {
                    return true
                }
            }
        }
        return false
    }

    private fun resolveAdditionalManaOnTap(
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        controllerId: com.wingedsheep.sdk.model.EntityId,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        var currentState = state
        val events = existingEvents.toMutableList()

        // Find all auras attached to the source permanent
        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue

            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Check each static ability for AdditionalManaOnTap
            for (staticAbility in cardDef.script.staticAbilities) {
                val additionalMana = staticAbility as? AdditionalManaOnTap ?: continue

                // The controller of the enchanted land gets the mana
                val landController = currentState.getEntity(sourceId)
                    ?.get<ControllerComponent>()?.playerId ?: controllerId
                val opponentId = currentState.turnOrder.firstOrNull { it != landController }

                val context = EffectContext(
                    sourceId = entityId,
                    controllerId = landController,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )

                val amount = dynamicAmountEvaluator.evaluate(currentState, additionalMana.amount, context)
                if (amount <= 0) continue

                // Add the mana to the land controller's pool
                currentState = currentState.updateEntity(landController) { c ->
                    val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    c.with(pool.add(additionalMana.color, amount))
                }

                events.add(ManaAddedEvent(
                    playerId = landController,
                    sourceId = entityId,
                    sourceName = card.name,
                    white = if (additionalMana.color == Color.WHITE) amount else 0,
                    blue = if (additionalMana.color == Color.BLUE) amount else 0,
                    black = if (additionalMana.color == Color.BLACK) amount else 0,
                    red = if (additionalMana.color == Color.RED) amount else 0,
                    green = if (additionalMana.color == Color.GREEN) amount else 0,
                    colorless = 0
                ))
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * After a land's mana ability resolves, check for permanents on the battlefield
     * with [AdditionalManaOnLandTap] whose filter matches the tapped land (e.g.,
     * Lavaleaper's "basic land" filter). Each matching ability adds one mana of the
     * same color(s) produced by the tap to the tapping player's pool.
     *
     * Like [AdditionalManaOnTap], this is a triggered mana ability that resolves
     * immediately without using the stack.
     */
    private fun resolveAdditionalManaOnLandTap(
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        manaEvent: ManaAddedEvent?,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        if (manaEvent == null) return AdditionalManaResult(state, existingEvents)

        val sourceContainer = state.getEntity(sourceId)
            ?: return AdditionalManaResult(state, existingEvents)
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return AdditionalManaResult(state, existingEvents)
        if (!sourceCard.typeLine.isLand) return AdditionalManaResult(state, existingEvents)

        var currentState = state
        val events = existingEvents.toMutableList()

        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val onLandTap = staticAbility as? AdditionalManaOnLandTap ?: continue

                val filterContext = PredicateContext(controllerId = controllerId, sourceId = entityId)
                if (!predicateEvaluator.matches(currentState, sourceId, onLandTap.filter, filterContext)) continue

                val opponentId = currentState.turnOrder.firstOrNull { it != controllerId }
                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = controllerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )
                val bonusAmount = dynamicAmountEvaluator.evaluate(currentState, onLandTap.amount, effectContext)
                if (bonusAmount <= 0) continue

                // "One mana of any type that land produced" — pick the single color
                // produced by the tap. Basic lands produce exactly one color, so this
                // is unambiguous. For multi-color producers, fall back to the first
                // non-zero color in the event.
                val bonusColor: Color? = when {
                    manaEvent.white > 0 -> Color.WHITE
                    manaEvent.blue > 0 -> Color.BLUE
                    manaEvent.black > 0 -> Color.BLACK
                    manaEvent.red > 0 -> Color.RED
                    manaEvent.green > 0 -> Color.GREEN
                    else -> null
                }
                val bonusColorless = bonusColor == null && manaEvent.colorless > 0
                if (bonusColor == null && !bonusColorless) continue

                currentState = currentState.updateEntity(controllerId) { c ->
                    val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    val newPool = if (bonusColor != null) pool.add(bonusColor, bonusAmount)
                                  else pool.addColorless(bonusAmount)
                    c.with(newPool)
                }

                events.add(ManaAddedEvent(
                    playerId = controllerId,
                    sourceId = entityId,
                    sourceName = card.name,
                    white = if (bonusColor == Color.WHITE) bonusAmount else 0,
                    blue = if (bonusColor == Color.BLUE) bonusAmount else 0,
                    black = if (bonusColor == Color.BLACK) bonusAmount else 0,
                    red = if (bonusColor == Color.RED) bonusAmount else 0,
                    green = if (bonusColor == Color.GREEN) bonusAmount else 0,
                    colorless = if (bonusColorless) bonusAmount else 0
                ))
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * Returns the maximum number of loyalty ability activations per planeswalker per turn
     * for the given player. Normally 1, but ExtraLoyaltyActivation (Oath of Teferi) raises it to 2.
     * Multiple copies do NOT stack beyond 2.
     */
    private fun getMaxLoyaltyActivations(state: GameState, playerId: EntityId): Int {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is ExtraLoyaltyActivation }) {
                return 2
            }
        }
        return 1
    }

    /**
     * Find a class level-up ability by its deterministic ID.
     * Returns the generated ActivatedAbility if the ID matches a valid level-up,
     * or null if this isn't a class level-up ability.
     */
    private fun findClassLevelUpAbility(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        container: com.wingedsheep.engine.state.ComponentContainer,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId
    ): ActivatedAbility? {
        if (!abilityId.value.startsWith("class_level_up_")) return null
        val classLevelComponent = container.get<ClassLevelComponent>() ?: return null
        val targetLevel = abilityId.value.removePrefix("class_level_up_").toIntOrNull() ?: return null
        if (targetLevel != classLevelComponent.currentLevel + 1) return null
        val levelAbility = cardDef.classLevels.find { it.level == targetLevel } ?: return null
        return ActivatedAbility(
            id = AbilityId.classLevelUp(targetLevel),
            cost = AbilityCost.Mana(levelAbility.cost),
            effect = LevelUpClassEffect(targetLevel),
            timing = TimingRule.SorcerySpeed,
            descriptionOverride = "Level up to level $targetLevel"
        )
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents.
     * E.g., Spectral Sliver grants a pump ability to all Sliver creatures via
     * GrantActivatedAbilityToCreatureGroup.
     */
    private fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<ActivatedAbility> {
        val targetContainer = state.getEntity(entityId) ?: return emptyList()
        val targetCard = targetContainer.get<CardComponent>() ?: return emptyList()

        val result = mutableListOf<ActivatedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                when (ability) {
                    is GrantActivatedAbilityToCreatureGroup -> {
                        if (ability.filter.excludeSelf && permanentId == entityId) continue
                        val filter = ability.filter.baseFilter
                        val matchesAll = filter.cardPredicates.all { predicate ->
                            when (predicate) {
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                    targetCard.typeLine.isCreature
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                    targetCard.typeLine.hasSubtype(predicate.subtype)
                                else -> true
                            }
                        }
                        if (matchesAll) {
                            result.add(ability.ability)
                        }
                    }
                    is GrantActivatedAbilityToAttachedCreature -> {
                        val attachedTo = container.get<AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(ability.ability)
                        }
                    }
                    else -> {}
                }
            }
        }

        return result
    }

    companion object {
        fun create(services: EngineServices): ActivateAbilityHandler {
            return ActivateAbilityHandler(
                services.cardRegistry,
                services.turnManager,
                services.costHandler,
                services.manaSolver,
                services.alternativePaymentHandler,
                services.effectExecutorRegistry,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
