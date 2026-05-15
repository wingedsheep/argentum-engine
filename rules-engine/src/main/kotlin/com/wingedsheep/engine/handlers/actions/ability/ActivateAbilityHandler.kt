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
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext
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
import com.wingedsheep.engine.state.components.stack.capturePermanentSnapshots
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
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorLandsCouldProduceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
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

        // Tokens (and other entities without a registered CardDefinition) only have abilities
        // via static grants (e.g., Brightcap Badger granting "{T}: Add {G}" to Saproling tokens),
        // intrinsic mana abilities (basic-land subtypes), or temporarily granted abilities. Don't
        // bail out when the lookup fails — fall through to those sources instead.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val staticGrants = getStaticGrantedAbilitiesWithGranter(action.sourceId, state)
        val ability = cardDef?.script?.effectiveActivatedAbilities(classLevel)?.find { it.id == action.abilityId }
            ?: cardDef?.let { findClassLevelUpAbility(it, container, action.abilityId) }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: staticGrants.firstOrNull { it.first.id == action.abilityId }?.first
            ?: resolveIntrinsicManaAbility(state, action.sourceId, action.abilityId)
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

            // Face-down creatures have no abilities (Rule 708.2)
            if (container.has<FaceDownComponent>()) {
                return "Face-down creatures have no abilities"
            }

            // Creatures that have lost all abilities cannot activate them (e.g., Deep Freeze)
            if (state.projectedState.hasLostAllAbilities(action.sourceId)) {
                // Only block the creature's own abilities, not granted ones
                val isOwnAbility = (cardDef?.script?.effectiveActivatedAbilities(classLevel)?.any { it.id == action.abilityId } == true)
                    || action.abilityId.value.startsWith("class_level_up_")
                if (isOwnAbility) {
                    return "This permanent has lost all abilities"
                }
            }
        }

        // Apply text-changing effects to cost and target filters
        val textReplacement = container.get<TextReplacementComponent>()
        val rawCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        // Apply ability-specific generic cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Per Scryfall ruling, the reduced
        // cost is locked in here, before costs are paid.
        val effectiveCost = applyGenericCostReduction(rawCost, ability, state, action.sourceId, action.playerId)
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

        val abilityPaymentContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId)

        if (action.paymentStrategy !is PaymentStrategy.Explicit && !canPayAbilityCostWithSources(state, costAfterConvokeReduction, action.sourceId, action.playerId, abilityPaymentContext)) {
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

        // Tokens (no registered CardDefinition) reach this path when activating granted abilities;
        // fall through with a null cardDef and let the granted-ability lookup succeed.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val staticGrants = getStaticGrantedAbilitiesWithGranter(action.sourceId, state)
        val staticGrantMatch = staticGrants.firstOrNull { it.first.id == action.abilityId }
        val ability = cardDef?.script?.effectiveActivatedAbilities(classLevel)?.find { it.id == action.abilityId }
            ?: cardDef?.let { findClassLevelUpAbility(it, container, action.abilityId) }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: staticGrantMatch?.first
            ?: resolveIntrinsicManaAbility(state, action.sourceId, action.abilityId)
            ?: return ExecutionResult.error(state, "Ability not found")
        val staticGranterId = staticGrantMatch?.second

        // Apply text-changing effects to cost
        val textReplacement = container.get<TextReplacementComponent>()
        val rawCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        // Apply ability-specific generic cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Locked in before payment.
        val effectiveCost = applyGenericCostReduction(rawCost, ability, state, action.sourceId, action.playerId)

        val executeAbilityContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId)

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
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana
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
        // If the outer ability's cost includes Tap, the source itself cannot also be used
        // as a mana source — the single "tap" it has is already consumed by the outer cost.
        val selfExcludedSources = if (hasTapCost(effectiveCost)) setOf(action.sourceId) else emptySet()
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
                        .toSet() + selfExcludedSources
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
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, manaPool, manaCost, cardComponent.name, manaXValue, selfExcludedSources, executeAbilityContext)
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
            counterRemovalChoices = action.costPayment?.counterRemovals ?: emptyMap(),
            blightChoices = action.costPayment?.blightTargets ?: emptyList(),
            granterId = staticGranterId
        )

        // Snapshot projected subtypes and P/T of sacrifice targets before zone change
        // (Rule 112.7a / 608.2h — "as it last existed on the battlefield")
        val sacrificeTargetIds = action.costPayment?.sacrificedPermanents ?: emptyList()
        val sacrificedSnapshots = capturePermanentSnapshots(sacrificeTargetIds, currentState.projectedState)

        // Mirror sacrifice snapshots for tapped-as-cost permanents — they may leave the
        // battlefield in response while the ability is on the stack.
        val tappedTargetIds = action.costPayment?.tappedPermanents ?: emptyList()
        val tappedSnapshots = capturePermanentSnapshots(tappedTargetIds, currentState.projectedState)

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
            costChoices,
            executeAbilityContext,
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
                colorless = manaPool.colorless,
                restrictedMana = manaPool.restrictedMana
            ))
        }

        // Emit events for cost types
        val abilityCost = ability.cost
        when (abilityCost) {
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
            is AbilityCost.Composite -> {
                for (subCost in abilityCost.costs) {
                    when (subCost) {
                        is AbilityCost.Tap -> events.add(TappedEvent(action.sourceId, cardComponent.name))
                        is AbilityCost.TapAttachedCreature -> {
                            val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                            if (attachedId != null) {
                                val attachedName = currentState.getEntity(attachedId)?.get<CardComponent>()?.name ?: "Unknown"
                                events.add(TappedEvent(attachedId, attachedName))
                            }
                        }
                        else -> {}
                    }
                }
            }
            is AbilityCost.Loyalty -> {
                events.add(LoyaltyChangedEvent(action.sourceId, cardComponent.name, abilityCost.change))
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
        var finalEffect = if (textReplacement != null) {
            ability.effect.applyTextReplacement(textReplacement)
        } else {
            ability.effect
        }

        // Mana abilities don't use the stack
        if (ability.isManaAbility) {
            // Check for an attached aura that overrides the produced mana color
            // (e.g., Shimmerwilds Growth: "Enchanted land is the chosen color").
            val overrideColor = findEnchantedLandManaColorOverride(currentState, action.sourceId)
            if (overrideColor != null && finalEffect is AddManaEffect) {
                finalEffect = finalEffect.copy(color = overrideColor)
            }
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
                    // Replace with 1 colorless mana: revert to old pool + 1 colorless.
                    // Restricted mana the player had floating before this activation
                    // is preserved — Damping Sphere only replaces what the land just
                    // produced, not what was already in the pool.
                    val dampenedPool = ManaPoolComponent(
                        white = oldPool.white,
                        blue = oldPool.blue,
                        black = oldPool.black,
                        red = oldPool.red,
                        green = oldPool.green,
                        colorless = oldPool.colorless + 1,
                        restrictedMana = oldPool.restrictedMana
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
                is AddAnyColorManaSpendOnChosenTypeEffect -> {
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
                is AddManaOfColorAmongEffect,
                is AddManaOfColorLandsCouldProduceEffect -> {
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
                    when (val manaEffect = effect.effects.firstOrNull {
                        it is AddManaEffect ||
                            it is AddColorlessManaEffect ||
                            it is AddAnyColorManaEffect ||
                            it is AddAnyColorManaSpendOnChosenTypeEffect
                    }) {
                        is AddManaEffect -> {
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
                            ManaAddedEvent(
                                playerId = action.playerId,
                                sourceId = action.sourceId,
                                sourceName = cardComponent.name,
                                white = if (manaEffect.color == Color.WHITE) amount else 0,
                                blue = if (manaEffect.color == Color.BLUE) amount else 0,
                                black = if (manaEffect.color == Color.BLACK) amount else 0,
                                red = if (manaEffect.color == Color.RED) amount else 0,
                                green = if (manaEffect.color == Color.GREEN) amount else 0,
                                colorless = 0
                            )
                        }
                        is AddColorlessManaEffect -> {
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
                            ManaAddedEvent(
                                playerId = action.playerId,
                                sourceId = action.sourceId,
                                sourceName = cardComponent.name,
                                colorless = amount
                            )
                        }
                        is AddAnyColorManaEffect -> {
                            val chosenColor = action.manaColorChoice ?: Color.GREEN
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
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
                        is AddAnyColorManaSpendOnChosenTypeEffect -> {
                            val chosenColor = action.manaColorChoice ?: Color.GREEN
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
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
                        else -> null
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

            // Check for global "additional mana whenever a matching source is tapped for mana"
            // (Lavaleaper: basic land mirror; Badgermole Cub: creature → +{G}).
            // Triggered mana ability — resolves immediately without the stack.
            val onSourceTapResult = resolveAdditionalManaOnSourceTap(
                currentState, action.sourceId, action.playerId, manaEvent, additionalManaResult.events
            )
            currentState = onSourceTapResult.state
            val allManaEvents = onSourceTapResult.events

            return ExecutionResult.success(currentState, allManaEvents)
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = finalEffect,
            sacrificedPermanents = sacrificedSnapshots,
            xValue = action.xValue,
            tappedPermanents = action.costPayment?.tappedPermanents ?: emptyList(),
            tappedPermanentSnapshots = tappedSnapshots,
            descriptionOverride = ability.descriptionOverride
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
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, repeatPool, manaCost, cardComponent.name, 0, abilityContext = executeAbilityContext)
                        ?: break // Can't afford — stop early
                    currentState = autoTapResult.newState
                    repeatPool = autoTapResult.newPool
                    events.addAll(autoTapResult.events)
                }

                // Pay the cost
                val repeatCostResult = costHandler.payAbilityCost(
                    currentState, effectiveCost, action.sourceId, action.playerId, repeatPool, CostPaymentChoices(), executeAbilityContext
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
                    tappedPermanents = emptyList(),
                    descriptionOverride = ability.descriptionOverride
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
        playerId: com.wingedsheep.sdk.model.EntityId,
        abilityContext: SpellPaymentContext? = null,
    ): Boolean {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana,
        )
        return when (cost) {
            is AbilityCost.Mana -> manaSolver.canPay(state, playerId, cost.cost, spellContext = abilityContext)
            is AbilityCost.Composite -> {
                // If composite cost includes Tap, the source itself can't also be used as a mana source
                val excludeSources = if (hasTapCost(cost)) setOf(sourceId) else emptySet()
                cost.costs.all { subCost ->
                    when (subCost) {
                        is AbilityCost.Mana -> manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeSources, spellContext = abilityContext)
                        else -> costHandler.canPayAbilityCost(state, subCost, sourceId, playerId, manaPool, abilityContext)
                    }
                }
            }
            else -> costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool, abilityContext)
        }
    }

    /**
     * Whether the given ability cost includes a Tap sub-cost.
     * The source of a Tap-cost ability cannot also serve as a mana source during payment.
     */
    private fun hasTapCost(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Tap -> true
        is AbilityCost.Composite -> cost.costs.any { it is AbilityCost.Tap }
        else -> false
    }

    /**
     * Apply [ActivatedAbility.genericCostReduction] to the mana portion of [cost].
     * The reduction is evaluated against the activating entity (e.g., the equipped creature
     * for The Dominion Bracelet, whose granted ability reduces by the creature's power).
     * Per Scryfall ruling, this is locked in before costs are paid.
     */
    private fun applyGenericCostReduction(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId
    ): AbilityCost {
        val reduction = ability.genericCostReduction ?: return cost
        val reductionContext = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            opponentId = null
        )
        val amount = DynamicAmountEvaluator().evaluate(state, reduction, reductionContext)
        if (amount <= 0) return cost
        return reduceGenericInCost(cost, amount)
    }

    private fun reduceGenericInCost(cost: AbilityCost, amount: Int): AbilityCost = when (cost) {
        is AbilityCost.Mana -> AbilityCost.Mana(cost.cost.reduceGeneric(amount))
        is AbilityCost.Composite -> {
            var applied = false
            AbilityCost.Composite(cost.costs.map { sub ->
                if (!applied && sub is AbilityCost.Mana) {
                    applied = true
                    AbilityCost.Mana(sub.cost.reduceGeneric(amount))
                } else sub
            })
        }
        else -> cost
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
        xValue: Int = 0,
        excludeSources: Set<com.wingedsheep.sdk.model.EntityId> = emptySet(),
        abilityContext: SpellPaymentContext? = null,
    ): AutoTapResult? {
        // Determine what the floating pool can cover (with the ability context so restricted
        // mana eligible for this activation counts toward coverage)
        val partialResult = pool.payPartial(cost, abilityContext)
        val remainingCost = partialResult.remainingCost

        // If floating pool covers everything (and no X to pay), no tapping needed.
        // Return the original pool unchanged — `payAbilityCost` performs the actual deduction.
        if (remainingCost.isEmpty() && xValue == 0) {
            return AutoTapResult(state, pool, emptyList())
        }

        // Tap sources for the remaining cost (xValue is treated as additional generic mana)
        val solution = manaSolver.solve(state, playerId, remainingCost, xValue, excludeSources = excludeSources, spellContext = abilityContext)
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

        // Add produced mana to floating pool so costHandler.payAbilityCost can consume it.
        // When the source's ability is restricted (e.g. Steelswarm Operator's
        // {T}: Add {U}{U} restricted to artifact-source ability activations), tag the
        // produced mana with that restriction. payAbilityCost will preferentially spend
        // the eligible restricted mana for the cost — and any unconsumed remainder stays
        // restricted in the pool instead of laundering into unrestricted mana.
        for (source in solution.sources) {
            // ManaSolver always emits a manaProduced entry per tapped source; a missing
            // entry would indicate a solver bug, not a runtime gap to swallow silently.
            val production = solution.manaProduced[source.entityId]
                ?: error("solution.sources contains ${source.name} without a matching manaProduced entry")
            val color = production.color
            val restriction = if (color != null) {
                source.colorRestrictions[color] ?: source.restriction
            } else source.restriction
            currentPool = when {
                color != null && restriction != null ->
                    currentPool.addRestricted(color, production.amount, restriction)
                color != null ->
                    currentPool.add(color, production.amount)
                else ->
                    currentPool.addColorless(production.colorless)
            }
        }

        // Add per-source bonus mana from AdditionalManaOnSourceTap auras/statics (e.g.,
        // Lavaleaper: tapping a basic land adds an extra mana of its produced color).
        // Unlike the cast flow — which uses solve's internal accounting as the payment —
        // the activate flow funnels all produced mana through the pool and then deducts
        // the cost via payAbilityCost, so the *total* bonus from tapping must land in the
        // pool. solution.remainingBonusMana would drop any bonus consumed during solve.
        // (Multi-mana excess is already included via manaProduced.amount above.)
        // Aura bonus mana is unrestricted — the source's restriction belongs to the
        // printed ability, not to the aura-granted extras.
        for (source in solution.sources) {
            if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                currentPool = currentPool.add(source.bonusManaColor, source.bonusManaPerTap)
            }
        }

        // Update state with enriched pool — carry restrictedMana through so the
        // ability-payment context can spend (and the leftover can stay) restricted.
        currentState = currentState.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = currentPool.white,
                blue = currentPool.blue,
                black = currentPool.black,
                red = currentPool.red,
                green = currentPool.green,
                colorless = currentPool.colorless,
                restrictedMana = currentPool.restrictedMana,
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
     * Count how many [AdditionalSourceTriggers] doublers on the battlefield apply to a
     * triggered ability with source [triggerSourceId] controlled by [triggerControllerId].
     *
     * Triggered mana abilities ([AdditionalManaOnTap], [AdditionalManaOnSourceTap]) bypass
     * the stack and are resolved synchronously, so they never flow through the normal
     * `TriggerDetector` doubling pass. This helper lets the inline mana resolution paths
     * apply the same doubling logic as the trigger pipeline.
     *
     * Returns N — N additional firings on top of the natural one (so total firings = N + 1).
     */
    private fun countAdditionalSourceTriggerDoublers(
        state: GameState,
        triggerSourceId: EntityId,
        triggerControllerId: EntityId
    ): Int {
        val projected = state.projectedState
        var count = 0
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            if (controllerId != triggerControllerId) continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is AdditionalSourceTriggers) continue
                if (ability.excludeSelf && permanentId == triggerSourceId) continue
                if (!predicateEvaluator.matchesWithProjection(
                        state, projected, triggerSourceId, ability.sourceFilter,
                        PredicateContext(controllerId = controllerId, sourceId = permanentId)
                    )
                ) continue
                count++
            }
        }
        return count
    }

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

    /**
     * If any aura attached to [sourceId] has an [OverrideEnchantedLandManaColor]
     * static ability, return the color the enchanted land's own mana abilities
     * should produce instead. `null` means no override (mana ability produces
     * normally). Multiple auras: last-wins (same aura only applies once).
     */
    private fun findEnchantedLandManaColorOverride(
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId
    ): Color? {
        var override: Color? = null
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val o = staticAbility as? com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor ?: continue
                override = o.color
                    ?: container.get<com.wingedsheep.engine.state.components.identity.ChosenColorComponent>()?.color
                    ?: continue
            }
        }
        return override
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

                // Resolve the color: if the ability specifies null, read the aura's chosen color.
                // If no color is chosen (e.g., somehow on battlefield without a choice), skip.
                val manaColor = additionalMana.color
                    ?: container.get<com.wingedsheep.engine.state.components.identity.ChosenColorComponent>()?.color
                    ?: continue

                // Triggered mana ability — apply AdditionalSourceTriggers doublers
                // (e.g., Twinflame Travelers) so the bonus fires N+1 times.
                val auraController = container.get<ControllerComponent>()?.playerId ?: landController
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, auraController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(landController) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        c.with(pool.add(manaColor, amount))
                    }

                    events.add(ManaAddedEvent(
                        playerId = landController,
                        sourceId = entityId,
                        sourceName = card.name,
                        white = if (manaColor == Color.WHITE) amount else 0,
                        blue = if (manaColor == Color.BLUE) amount else 0,
                        black = if (manaColor == Color.BLACK) amount else 0,
                        red = if (manaColor == Color.RED) amount else 0,
                        green = if (manaColor == Color.GREEN) amount else 0,
                        colorless = 0
                    ))
                }
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * After a permanent's mana ability resolves, check for [AdditionalManaOnSourceTap]
     * statics anywhere on the battlefield whose `sourceFilter` matches the tapped source.
     * Each match adds bonus mana to the tapping player's pool.
     *
     * Filter matching uses projected state so animated creature-lands and typeshifted
     * lands count under their projected types (Rule 613.1). The static-ability source's
     * controller is read from projected state so control-changing effects (Annex,
     * Ray of Command) correctly transfer the "you tap" condition along with the permanent.
     *
     * Triggered mana ability — resolves immediately without using the stack (Rule 605).
     */
    private fun resolveAdditionalManaOnSourceTap(
        state: GameState,
        sourceId: EntityId,
        tappingPlayerId: EntityId,
        manaEvent: ManaAddedEvent?,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        state.getEntity(sourceId) ?: return AdditionalManaResult(state, existingEvents)

        // The mirror-color form (color = null) needs the actual produced color from manaEvent.
        // The fixed-color form does not.
        val producedColor: Color? = manaEvent?.let {
            when {
                it.white > 0 -> Color.WHITE
                it.blue > 0 -> Color.BLUE
                it.black > 0 -> Color.BLACK
                it.red > 0 -> Color.RED
                it.green > 0 -> Color.GREEN
                else -> null
            }
        }
        val producedColorless = manaEvent != null && producedColor == null && manaEvent.colorless > 0

        var currentState = state
        val events = existingEvents.toMutableList()

        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val onSourceTap = staticAbility as? AdditionalManaOnSourceTap ?: continue

                val staticController = currentState.projectedState.getController(entityId) ?: continue

                // Filter is evaluated from the static-ability controller's perspective so
                // `youControl` on the source filter means "controlled by you, the static
                // controller" — see AdditionalManaOnSourceTap kdoc.
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (!predicateEvaluator.matchesWithProjection(
                        currentState, currentState.projectedState, sourceId, onSourceTap.sourceFilter, filterContext
                    )) continue

                val opponentId = currentState.turnOrder.firstOrNull { it != tappingPlayerId }
                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = tappingPlayerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )
                val bonusAmount = dynamicAmountEvaluator.evaluate(currentState, onSourceTap.amount, effectContext)
                if (bonusAmount <= 0) continue

                // Resolve the bonus color: explicit color wins; null means mirror the produced color.
                val bonusColor: Color? = onSourceTap.color ?: producedColor
                val bonusColorless = onSourceTap.color == null && bonusColor == null && producedColorless
                if (bonusColor == null && !bonusColorless) continue

                // Triggered mana abilities bypass the stack but are still triggered
                // abilities — so AdditionalSourceTriggers (Twinflame Travelers) doubles
                // them just like any other trigger. firings = 1 (natural) + N (doublers).
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, staticController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(tappingPlayerId) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        val newPool = if (bonusColor != null) pool.add(bonusColor, bonusAmount)
                                      else pool.addColorless(bonusAmount)
                        c.with(newPool)
                    }

                    events.add(ManaAddedEvent(
                        playerId = tappingPlayerId,
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
     * Resolve an intrinsic mana ability granted by a basic-land subtype (CR 305.7).
     * Returns the synthesized ability only if the entity currently projects the
     * matching basic-land subtype, so an `intrinsic_mana_R` request on a land that
     * isn't a Mountain in the projected state is rejected.
     */
    private fun resolveIntrinsicManaAbility(
        state: GameState,
        sourceId: EntityId,
        abilityId: AbilityId,
    ): ActivatedAbility? {
        val ability = IntrinsicManaAbilities.lookup(abilityId) ?: return null
        val color = (ability.effect as? AddManaEffect)?.color ?: return null
        val expectedSubtype = when (color) {
            Color.WHITE -> "Plains"
            Color.BLUE -> "Island"
            Color.BLACK -> "Swamp"
            Color.RED -> "Mountain"
            Color.GREEN -> "Forest"
        }
        val subtypes = state.projectedState.getSubtypes(sourceId)
        if (expectedSubtype !in subtypes) return null
        return ability
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
     * Get activated abilities granted to an entity by static abilities on battlefield permanents,
     * paired with the EntityId of the permanent that granted each ability.
     * E.g., Spectral Sliver grants a pump ability to all Sliver creatures via
     * GrantActivatedAbility. The Dominion Bracelet grants its activated
     * ability to the equipped creature via GrantActivatedAbility; the
     * granter ID is needed to resolve AbilityCost.ExileGrantingPermanent.
     */
    private fun getStaticGrantedAbilitiesWithGranter(
        entityId: EntityId,
        state: GameState
    ): List<Pair<ActivatedAbility, EntityId>> {
        if (state.getEntity(entityId) == null) return emptyList()

        val result = mutableListOf<Pair<ActivatedAbility, EntityId>>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is GrantActivatedAbility) continue
                when (ability.filter.scope) {
                    is Scope.Battlefield -> {
                        if (ability.filter.excludeSelf && permanentId == entityId) continue
                        val granterController = state.projectedState.getController(permanentId) ?: continue
                        val matches = predicateEvaluator.matchesWithProjection(
                            state,
                            state.projectedState,
                            entityId,
                            ability.filter.baseFilter,
                            PredicateContext(controllerId = granterController, sourceId = permanentId)
                        )
                        if (matches) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                    is Scope.AttachedTo -> {
                        val attachedTo = container.get<AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                    is Scope.Self -> {
                        if (permanentId == entityId) result.add(ability.ability to permanentId)
                    }
                    is Scope.Specific -> {
                        if ((ability.filter.scope as Scope.Specific).entityId == entityId) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<ActivatedAbility> = getStaticGrantedAbilitiesWithGranter(entityId, state).map { it.first }

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
