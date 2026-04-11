package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.core.Keyword

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import kotlin.reflect.KClass

/**
 * Handler for the CastSpell action.
 *
 * Orchestrates spell casting by delegating to focused components:
 * - [CastZoneResolver]: Determines where a card can be cast from
 * - [CastPaymentProcessor]: Handles mana payment via three strategies
 *
 * This handler owns the top-level validate/execute flow, cast restrictions,
 * additional cost processing, and trigger detection.
 */
class CastSpellHandler(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val costHandler: CostHandler,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
) : ActionHandler<CastSpell> {
    override val actionType: KClass<CastSpell> = CastSpell::class

    private val predicateEvaluator = PredicateEvaluator()
    private val zoneResolver = CastZoneResolver(cardRegistry, conditionEvaluator)
    private val paymentProcessor = CastPaymentProcessor(manaSolver, costHandler)

    override fun validate(state: GameState, action: CastSpell): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val inHand = action.cardId in state.getZone(handZone)
        val onTopOfLibrary = !inHand && zoneResolver.isOnTopOfLibraryWithPermission(state, action.playerId, action.cardId)
        val mayPlayFromExile = !inHand && !onTopOfLibrary && zoneResolver.isInExileWithPlayPermission(state, action.playerId, action.cardId)
        val mayCastFromZone = !inHand && !onTopOfLibrary && !mayPlayFromExile &&
            zoneResolver.hasMayCastSelfFromZonePermission(state, action.playerId, action.cardId)
        val mayCastFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone &&
            zoneResolver.hasMayPlayPermanentFromGraveyardPermission(state, action.playerId, action.cardId, cardComponent)
        val hasFlashback = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard &&
            zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)
        val hasWarpFromExile = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback &&
            zoneResolver.hasWarpFromExilePermission(state, action.playerId, action.cardId)
        val hasGraveyardLifeCost = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasWarpFromExile &&
            action.graveyardLifeCost > 0 && action.cardId in state.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasWarpFromExile && !hasGraveyardLifeCost) {
            return "Card is not in your hand"
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Handle face-down casting (morph)
        if (action.castFaceDown) {
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
                ?: return "This card cannot be cast face down (no morph ability)"

            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast face-down creatures at sorcery speed"
            }

            val morphCastCost = costCalculator.calculateFaceDownCost(state, action.playerId)
            return validatePayment(state, action, morphCastCost)
        }

        // Check timing
        if (!cardComponent.typeLine.isInstant) {
            val hasFlash = cardDef?.keywords?.contains(Keyword.FLASH) == true
            val grantedFlash = hasFlash || zoneResolver.hasGrantedFlash(state, action.cardId)
            if (!grantedFlash && !turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast sorcery-speed spells during your main phase with an empty stack"
            }
        }

        // Check cast restrictions
        if (cardDef != null && cardDef.script.castRestrictions.isNotEmpty()) {
            val restrictionError = validateCastRestrictions(state, cardDef.script.castRestrictions, action.playerId)
            if (restrictionError != null) {
                return restrictionError
            }
        }

        // Validate additional costs (use per-mode costs if the chosen mode overrides them)
        if (cardDef != null) {
            val modeAdditionalCosts = resolveAdditionalCostsForMode(cardDef, action)
            val additionalCostError = validateAdditionalCosts(state, modeAdditionalCosts, action)
            if (additionalCostError != null) {
                return additionalCostError
            }
        }

        // Validate runtime additional costs from PlayWithAdditionalCostComponent (e.g., The Infamous Cruelclaw)
        val runtimeAdditionalCostComponent = state.getEntity(action.cardId)
            ?.get<PlayWithAdditionalCostComponent>()
            ?.takeIf { it.controllerId == action.playerId }
        if (runtimeAdditionalCostComponent != null) {
            val runtimeCostError = validateAdditionalCosts(state, runtimeAdditionalCostComponent.additionalCosts, action)
            if (runtimeCostError != null) return runtimeCostError
        }

        // Validate kicker/offspring: card must have Kicker, KickerWithAdditionalCost, or Offspring keyword ability
        if (action.wasKicked && cardDef != null) {
            val hasKicker = cardDef.keywordAbilities.any { it is KeywordAbility.Kicker || it is KeywordAbility.KickerWithAdditionalCost || it is KeywordAbility.Offspring }
            if (!hasKicker) return "This card does not have kicker"

            // Validate kicker additional cost (sacrifice, etc.)
            val kickerAdditionalCost = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.KickerWithAdditionalCost>()
                .firstOrNull()
            if (kickerAdditionalCost != null) {
                val kickerCostError = validateAdditionalCosts(state, listOf(kickerAdditionalCost.cost), action)
                if (kickerCostError != null) return kickerCostError
            }
        }

        // Validate self-alternative cost's additional costs when using alternative cost
        if (action.useAlternativeCost && cardDef != null) {
            val selfAltCost = cardDef.script.selfAlternativeCost
            if (selfAltCost != null && selfAltCost.additionalCosts.isNotEmpty()) {
                val selfAltCostError = validateAdditionalCosts(state, selfAltCost.additionalCosts, action)
                if (selfAltCostError != null) return selfAltCostError
            }
        }

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present)
        val playForFree = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId)
        var effectiveCost = if (playForFree) {
            ManaCost.ZERO
        } else if (action.useAlternativeCost && cardDef != null) {
            // Check flashback cost first (card in graveyard with Flashback keyword)
            val flashbackAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Flashback>().firstOrNull()
            if (flashbackAbility != null && zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, flashbackAbility.cost, action.playerId)
            } else {
                // Check warp cost (card in hand or exile with warp permission)
                val warpAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Warp>().firstOrNull()
                if (warpAbility != null && (zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
                            || zoneResolver.hasWarpFromExilePermission(state, action.playerId, action.cardId))) {
                    costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, warpAbility.cost, action.playerId)
                } else {
                    // Check self-alternative cost (e.g., Zahid's {3}{U} + tap artifact)
                    val selfAltCost = cardDef.script.selfAlternativeCost
                    if (selfAltCost != null) {
                        val altMana = ManaCost.parse(selfAltCost.manaCost)
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altMana, action.playerId)
                    } else {
                        // Fall back to battlefield-granted alternative cost (e.g., Jodah's {W}{U}{B}{R}{G})
                        val altCosts = costCalculator.findAlternativeCastingCosts(state, action.playerId)
                        if (altCosts.isEmpty()) return "No alternative casting cost available"
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCosts.first())
                    }
                }
            }
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(
                state,
                cardDef,
                action.playerId,
                action.targets.map { it.toEntityId() }
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring mana cost if kicked (only for mana-based kicker/offspring)
        if (action.wasKicked && !playForFree && !action.useAlternativeCost && cardDef != null) {
            val kickerAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Kicker>().firstOrNull()
            val offspringAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Offspring>().firstOrNull()
            if (kickerAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerAbility.cost.symbols)
            } else if (offspringAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + offspringAbility.cost.symbols)
            }
        }

        // Apply sacrifice-for-cost-reduction before validating payment
        if (cardDef != null && action.additionalCostPayment != null) {
            for (cost in cardDef.script.additionalCosts) {
                if (cost is AdditionalCost.SacrificeCreaturesForCostReduction) {
                    val sacrificeCount = action.additionalCostPayment.sacrificedPermanents.size
                    val reduction = sacrificeCount * cost.costReductionPerCreature
                    if (reduction > 0) {
                        effectiveCost = effectiveCost.reduceGeneric(reduction)
                    }
                }
            }
        }

        // Account for Delve/Convoke reduction before validating payment
        val costAfterAltPayment = if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            alternativePaymentHandler.calculateReducedCost(effectiveCost, action.alternativePayment, cardDef)
        } else {
            effectiveCost
        }

        // Validate payment
        val paymentError = validatePayment(state, action, costAfterAltPayment)
        if (paymentError != null) {
            return paymentError
        }

        // Validate targets (include auraTarget as a target requirement for aura spells)
        // Use mode-specific targets for modal spells, kickerTargetRequirements when kicked
        if (cardDef != null) {
            val modalEffect = cardDef.script.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
            val baseTargetReqs = if (action.chosenMode != null && modalEffect != null) {
                // Modal spell with mode chosen at cast time — validate against mode-specific targets
                val mode = modalEffect.modes.getOrNull(action.chosenMode)
                mode?.targetRequirements ?: emptyList()
            } else if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            val targetRequirements = buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
            if (targetRequirements.isNotEmpty()) {
                // Reject casting if spell requires targets but none were provided
                if (action.targets.isEmpty()) {
                    val requiredCount = targetRequirements.sumOf { it.effectiveMinCount }
                    if (requiredCount > 0) {
                        return "No valid targets available"
                    }
                }
                val targetError = targetValidator.validateTargets(
                    state,
                    action.targets,
                    targetRequirements,
                    action.playerId,
                    sourceColors = cardDef.colors,
                    sourceSubtypes = cardDef.typeLine.subtypes.map { it.value }.toSet(),
                    sourceId = action.cardId
                )
                if (targetError != null) {
                    return targetError
                }
            }
        }

        // Validate damage distribution for DividedDamageEffect spells
        // Use kickerSpellEffect when kicked and available
        val spellEffect = if (action.wasKicked && cardDef?.script?.kickerSpellEffect != null) {
            cardDef.script.kickerSpellEffect
        } else {
            cardDef?.script?.spellEffect
        }
        if (spellEffect is DividedDamageEffect && action.targets.size > 1) {
            val distribution = action.damageDistribution
            if (distribution == null) {
                return "Damage distribution required for this spell when targeting multiple creatures"
            }

            // Check that distribution targets match chosen targets
            val targetIds = action.targets.map { it.toEntityId() }.toSet()
            val distributionTargets = distribution.keys
            if (distributionTargets != targetIds) {
                return "Damage distribution targets must match chosen targets"
            }

            // Check that total damage equals the spell's total damage
            val totalDistributed = distribution.values.sum()
            if (totalDistributed != spellEffect.totalDamage) {
                return "Total distributed damage ($totalDistributed) must equal ${spellEffect.totalDamage}"
            }

            // Check that each target gets at least 1 damage (per MTG rules)
            val minPerTarget = 1
            for ((targetId, damage) in distribution) {
                if (damage < minPerTarget) {
                    return "Each target must receive at least $minPerTarget damage"
                }
            }
        }

        return null
    }

    private fun validatePayment(state: GameState, action: CastSpell, cost: ManaCost): String? {
        val xValue = action.xValue ?: 0

        // Build spell context for conditional mana validation
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
        val spellCtx = if (cardComponent != null) {
            SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = action.wasKicked,
                isCreature = cardComponent.typeLine.isCreature,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX
            )
        } else null

        return when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, cost, xValue, spellContext = spellCtx)) {
                    "Not enough mana to cast this spell"
                } else null
            }
            is PaymentStrategy.FromPool -> {
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                if (!pool.canPay(cost, spellCtx)) {
                    "Insufficient mana in pool to cast this spell"
                } else null
            }
            is PaymentStrategy.Explicit -> {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                // Verify the chosen sources can actually pay the colored cost.
                // Ask the solver to pay using ONLY the chosen sources by excluding all others.
                val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                    .map { it.entityId }
                    .filter { it !in chosen }
                    .toSet()
                if (manaSolver.solve(state, action.playerId, cost, xValue, excludeSources = excluded, spellContext = spellCtx) == null) {
                    "Selected mana sources cannot pay this spell's cost"
                } else null
            }
        }
    }

    private fun validateCastRestrictions(
        state: GameState,
        restrictions: List<CastRestriction>,
        playerId: EntityId
    ): String? {
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
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
            is CastRestriction.TimingRequirement -> null
            is CastRestriction.All -> {
                for (subRestriction in restriction.restrictions) {
                    val error = validateSingleRestriction(state, subRestriction, context)
                    if (error != null) return error
                }
                null
            }
        }
    }

    /**
     * Resolves the additional costs for a spell, considering per-mode overrides.
     * If the chosen mode specifies its own additionalCosts, those are used instead of card-level costs.
     */
    private fun resolveAdditionalCostsForMode(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        action: CastSpell
    ): List<AdditionalCost> {
        if (action.chosenMode != null) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            val chosenMode = modalEffect?.modes?.getOrNull(action.chosenMode)
            val modeCosts = chosenMode?.additionalCosts
            if (modeCosts != null) {
                return modeCosts
            }
        }
        return cardDef.script.additionalCosts
    }

    private fun validateAdditionalCosts(
        state: GameState,
        additionalCosts: List<AdditionalCost>,
        action: CastSpell
    ): String? {
        val projected = state.projectedState
        val flattenedCosts = additionalCosts.flatMap {
            if (it is AdditionalCost.Composite) it.steps else listOf(it)
        }
        for (additionalCost in flattenedCosts) {
            when (additionalCost) {
                is AdditionalCost.SacrificePermanent -> {
                    val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                    val filterDesc = additionalCost.filter.description
                    if (sacrificed.size < additionalCost.count) {
                        return "You must sacrifice ${additionalCost.count} $filterDesc to cast this spell"
                    }
                    for (permId in sacrificed) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Sacrificed permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Sacrificed entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only sacrifice permanents you control"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Sacrificed permanent is not on the battlefield: $permId"
                        }
                        // Use unified filter with projected state
                        val context = PredicateContext(controllerId = action.playerId)
                        val matches = predicateEvaluator.matchesWithProjection(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: $filterDesc"
                        }
                    }
                }
                is AdditionalCost.ExileVariableCards -> {
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.size < additionalCost.minCount) {
                        return "You must exile at least ${additionalCost.minCount} ${additionalCost.filter.description}(s) from your ${additionalCost.fromZone.description}"
                    }
                    val zone = when (additionalCost.fromZone) {
                        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                    }
                    val zoneKey = ZoneKey(action.playerId, zone)
                    val zoneCards = state.getZone(zoneKey)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in exiled) {
                        if (cardId !in zoneCards) {
                            return "Card to exile is not in your ${additionalCost.fromZone.description}"
                        }
                        if (!predicateEvaluator.matchesWithProjection(state, projected, cardId, additionalCost.filter, context)) {
                            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                            return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.ExileCards -> {
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.size < additionalCost.count) {
                        return "You must exile ${additionalCost.count} ${additionalCost.filter.description}(s) from your ${additionalCost.fromZone.description}"
                    }
                    val zone = when (additionalCost.fromZone) {
                        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                    }
                    val zoneKey = ZoneKey(action.playerId, zone)
                    val zoneCards = state.getZone(zoneKey)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in exiled) {
                        if (cardId !in zoneCards) {
                            return "Card to exile is not in your ${additionalCost.fromZone.description}"
                        }
                        if (!predicateEvaluator.matchesWithProjection(state, projected, cardId, additionalCost.filter, context)) {
                            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                            return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.DiscardCards -> {
                    val discarded = action.additionalCostPayment?.discardedCards ?: emptyList()
                    if (discarded.size < additionalCost.count) {
                        return "You must discard ${additionalCost.count} card(s) to cast this spell"
                    }
                    val handZone = ZoneKey(action.playerId, Zone.HAND)
                    val handCards = state.getZone(handZone)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in discarded) {
                        if (cardId !in handCards) {
                            return "Card to discard is not in your hand"
                        }
                        if (cardId == action.cardId) {
                            return "Cannot discard the spell being cast"
                        }
                        if (additionalCost.filter != com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                            if (!predicateEvaluator.matches(state, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                }
                is AdditionalCost.TapPermanents -> {
                    val tapped = action.additionalCostPayment?.tappedPermanents ?: emptyList()
                    if (tapped.size < additionalCost.count) {
                        return "You must tap ${additionalCost.count} ${additionalCost.filter.description}(s) to cast this spell"
                    }
                    val context = PredicateContext(controllerId = action.playerId)
                    for (permId in tapped) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Tapped permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Tapped entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only tap permanents you control"
                        }
                        if (permContainer.has<TappedComponent>()) {
                            return "${permCard.name} is already tapped"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Tapped permanent is not on the battlefield: $permId"
                        }
                        val matches = predicateEvaluator.matchesWithProjection(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                    // Sacrificing 0 creatures is valid (optional sacrifice)
                    val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                    for (permId in sacrificed) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Sacrificed permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Sacrificed entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only sacrifice permanents you control"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Sacrificed permanent is not on the battlefield: $permId"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        val matches = predicateEvaluator.matchesWithProjection(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.Behold -> {
                    val chosen = action.additionalCostPayment?.beheldCards ?: emptyList()
                    if (chosen.size < additionalCost.count) {
                        return "You must behold ${additionalCost.count} ${additionalCost.filter.description}(s)"
                    }
                    val handZone = ZoneKey(action.playerId, Zone.HAND)
                    val handCards = state.getZone(handZone)
                    val battlefieldCards = state.getBattlefield()
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in chosen) {
                        val inHand = cardId in handCards && cardId != action.cardId
                        val onBattlefield = cardId in battlefieldCards &&
                            projected.getController(cardId) == action.playerId
                        if (!inHand && !onBattlefield) {
                            return "Beheld card must be a card in your hand or a permanent you control"
                        }
                        if (onBattlefield) {
                            if (!predicateEvaluator.matchesWithProjection(state, projected, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        } else {
                            if (!predicateEvaluator.matches(state, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                }
                is AdditionalCost.ExileFromStorage -> {
                    // Validated by the preceding Behold cost — nothing extra needed
                }
                else -> {}
            }
        }
        return null
    }

    override fun execute(state: GameState, action: CastSpell): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")

        val xValue = action.xValue ?: 0
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present)
        val playForFreeInExecute = zoneResolver.hasPlayWithoutPayingCost(currentState, action.playerId, action.cardId)
        var effectiveCost = if (playForFreeInExecute) {
            ManaCost.ZERO
        } else if (action.useAlternativeCost && cardDef != null) {
            // Check flashback cost first
            val flashbackAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Flashback>().firstOrNull()
            if (flashbackAbility != null && zoneResolver.hasFlashbackPermission(currentState, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, flashbackAbility.cost, action.playerId)
            } else {
                // Check warp cost
                val warpAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Warp>().firstOrNull()
                if (warpAbility != null && (zoneResolver.hasWarpPermission(currentState, action.playerId, action.cardId)
                            || zoneResolver.hasWarpFromExilePermission(currentState, action.playerId, action.cardId))) {
                    costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, warpAbility.cost, action.playerId)
                } else {
                    val selfAltCost = cardDef.script.selfAlternativeCost
                    if (selfAltCost != null) {
                        val altMana = ManaCost.parse(selfAltCost.manaCost)
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altMana, action.playerId)
                    } else {
                        val altCosts = costCalculator.findAlternativeCastingCosts(currentState, action.playerId)
                        if (altCosts.isNotEmpty()) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altCosts.first())
                        } else {
                            cardComponent.manaCost
                        }
                    }
                }
            }
        } else if (action.castFaceDown) {
            costCalculator.calculateFaceDownCost(currentState, action.playerId)
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(
                currentState,
                cardDef,
                action.playerId,
                action.targets.map { it.toEntityId() }
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring cost if kicked (not applicable with alternative costs)
        if (action.wasKicked && !playForFreeInExecute && !action.useAlternativeCost && cardDef != null) {
            val kickerAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Kicker>().firstOrNull()
            val offspringAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Offspring>().firstOrNull()
            if (kickerAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerAbility.cost.symbols)
            } else if (offspringAbility != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + offspringAbility.cost.symbols)
            }
        }

        // Apply per-mode additional mana cost (e.g., Feed the Cycle "pay {B}" mode)
        if (cardDef != null && action.chosenMode != null) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            val chosenMode = modalEffect?.modes?.getOrNull(action.chosenMode)
            val modeManaCost = chosenMode?.additionalManaCost
            if (modeManaCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(modeManaCost)
            }
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedPermanentIds = mutableListOf<EntityId>()
        val sacrificedPermanentSubtypes = mutableMapOf<EntityId, Set<String>>()
        var exiledCardCount = 0
        val beheldCards = mutableListOf<EntityId>()
        /** Pipeline storage populated by Behold, consumed by ExileFromStorage */
        val costPipelineCollections = mutableMapOf<String, List<EntityId>>()

        // Collect all additional costs: script costs + kicker additional cost (if kicked)
        // + self-alternative cost's additional costs (if using alternative cost)
        // + runtime additional costs from PlayWithAdditionalCostComponent
        // Per-mode additional costs override card-level costs when present
        val allAdditionalCosts = buildList {
            if (cardDef != null) addAll(resolveAdditionalCostsForMode(cardDef, action))
            if (action.wasKicked && cardDef != null) {
                val kickerAdditionalCost = cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.KickerWithAdditionalCost>()
                    .firstOrNull()
                if (kickerAdditionalCost != null) add(kickerAdditionalCost.cost)
            }
            if (action.useAlternativeCost && cardDef != null) {
                val selfAltCost = cardDef.script.selfAlternativeCost
                if (selfAltCost != null) addAll(selfAltCost.additionalCosts)
            }
            // Runtime additional costs from entity component (e.g., The Infamous Cruelclaw)
            val runtimeCostComp = currentState.getEntity(action.cardId)
                ?.get<PlayWithAdditionalCostComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostComp != null) addAll(runtimeCostComp.additionalCosts)
        }

        val flattenedAllCosts = allAdditionalCosts.flatMap {
            if (it is AdditionalCost.Composite) it.steps else listOf(it)
        }
        if (flattenedAllCosts.isNotEmpty() && action.additionalCostPayment != null) {
            for (additionalCost in flattenedAllCosts) {
                when (additionalCost) {
                    is AdditionalCost.SacrificePermanent -> {
                        // Project state to capture text-changed subtypes before sacrifice
                        val projectedBeforeSacrifice = currentState.projectedState
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            // Snapshot projected subtypes before zone change
                            val projectedSubtypes = projectedBeforeSacrifice.getSubtypes(permId)
                            if (projectedSubtypes.isNotEmpty()) {
                                sacrificedPermanentSubtypes[permId] = projectedSubtypes
                            }

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            sacrificedPermanentIds.add(permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = Zone.BATTLEFIELD,
                                toZone = Zone.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                    }
                    is AdditionalCost.DiscardCards -> {
                        val discardedCards = action.additionalCostPayment.discardedCards
                        for (cardId in discardedCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val handZone = ZoneKey(action.playerId, Zone.HAND)
                            val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)

                            currentState = currentState.removeFromZone(handZone, cardId)
                            currentState = currentState.addToZone(graveyardZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = Zone.HAND,
                                toZone = Zone.GRAVEYARD,
                                ownerId = action.playerId
                            ))
                        }
                        val discardNames = discardedCards.map { currentState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                        events.add(CardsDiscardedEvent(action.playerId, discardedCards, discardNames))
                    }
                    is AdditionalCost.ExileVariableCards, is AdditionalCost.ExileCards -> {
                        val exiledCards = action.additionalCostPayment.exiledCards
                        val fromZone = when (additionalCost) {
                            is AdditionalCost.ExileVariableCards -> additionalCost.fromZone
                            is AdditionalCost.ExileCards -> additionalCost.fromZone
                        }
                        val zone = when (fromZone) {
                            com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                            com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                            com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                            com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                        }
                        for (cardId in exiledCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val sourceZone = ZoneKey(action.playerId, zone)
                            val exileZone = ZoneKey(action.playerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = zone,
                                toZone = Zone.EXILE,
                                ownerId = action.playerId
                            ))
                        }
                        exiledCardCount = exiledCards.size
                    }
                    is AdditionalCost.TapPermanents -> {
                        // Tap permanents as additional cost (e.g., Zahid's tap an artifact)
                        val tappedPerms = action.additionalCostPayment.tappedPermanents
                        for (permId in tappedPerms) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            if (!permContainer.has<TappedComponent>()) {
                                currentState = currentState.updateEntity(permId) { c ->
                                    c.with(TappedComponent)
                                }
                                val permCard = permContainer.get<CardComponent>()
                                events.add(TappedEvent(permId, permCard?.name ?: "Permanent"))
                            }
                        }
                    }
                    is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                        // Process sacrifices for cost reduction (e.g., Torgaar)
                        val projectedBeforeSacrifice = currentState.projectedState
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            val projectedSubtypes = projectedBeforeSacrifice.getSubtypes(permId)
                            if (projectedSubtypes.isNotEmpty()) {
                                sacrificedPermanentSubtypes[permId] = projectedSubtypes
                            }

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            sacrificedPermanentIds.add(permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = Zone.BATTLEFIELD,
                                toZone = Zone.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                        // Apply cost reduction based on number of creatures sacrificed
                        val reduction = action.additionalCostPayment.sacrificedPermanents.size * additionalCost.costReductionPerCreature
                        if (reduction > 0) {
                            effectiveCost = effectiveCost.reduceGeneric(reduction)
                        }
                    }
                    is AdditionalCost.Behold -> {
                        // Store beheld card IDs in pipeline for downstream costs/effects
                        val chosen = action.additionalCostPayment.beheldCards
                        beheldCards.addAll(chosen)
                        costPipelineCollections[additionalCost.storeAs] = chosen

                        // Behold reveals the chosen card(s) to all players
                        if (chosen.isNotEmpty()) {
                            val cardNames = chosen.mapNotNull { currentState.getEntity(it)?.get<CardComponent>()?.name }
                            val imageUris = chosen.map { id ->
                                val defId = currentState.getEntity(id)?.get<CardComponent>()?.cardDefinitionId
                                defId?.let { cardRegistry.getCard(it)?.metadata?.imageUri }
                            }
                            events.add(CardsRevealedEvent(
                                revealingPlayerId = action.playerId,
                                cardIds = chosen,
                                cardNames = cardNames,
                                imageUris = imageUris,
                                source = cardComponent.name
                            ))
                        }
                    }
                    is AdditionalCost.ExileFromStorage -> {
                        // Exile cards from pipeline collection (e.g., beheld cards)
                        val cardsToExile = costPipelineCollections[additionalCost.from] ?: emptyList()
                        for (cardId in cardsToExile) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue

                            // Determine source zone (could be battlefield or hand)
                            val controllerId = cardContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = card.ownerId ?: action.playerId
                            val sourceZone = if (cardId in currentState.getBattlefield()) {
                                ZoneKey(controllerId, Zone.BATTLEFIELD)
                            } else {
                                ZoneKey(action.playerId, Zone.HAND)
                            }
                            val exileZone = ZoneKey(ownerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = sourceZone.zoneType,
                                toZone = Zone.EXILE,
                                ownerId = ownerId
                            ))
                        }
                        // Link exiled cards to spell entity for LTB triggers
                        if (additionalCost.linkToSource && cardsToExile.isNotEmpty()) {
                            currentState = currentState.updateEntity(action.cardId) { c ->
                                c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(
                                    exiledIds = cardsToExile
                                ))
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Apply alternative payment (Delve/Convoke)
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

        // Build spell context for conditional mana restrictions
        val spellContext = SpellPaymentContext(
            isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
            isKicked = action.wasKicked,
            isCreature = cardComponent.typeLine.isCreature,
            manaValue = cardComponent.manaCost.cmc,
            hasXInCost = cardComponent.manaCost.hasX
        )

        // Handle mana payment via dedicated processor
        val paymentResult = paymentProcessor.processPayment(currentState, action, effectiveCost, cardComponent.name, xValue, spellContext)
        if (paymentResult.error != null) {
            return ExecutionResult.error(currentState, paymentResult.error)
        }
        currentState = paymentResult.state
        events.addAll(paymentResult.events)

        // Track total mana spent on spells this turn (for Expend triggers)
        val manaSpentThisCast = paymentResult.events
            .filterIsInstance<ManaSpentEvent>()
            .sumOf { it.total }
        if (manaSpentThisCast > 0) {
            currentState = currentState.updateEntity(action.playerId) { container ->
                val existing = container.get<ManaSpentOnSpellsThisTurnComponent>()
                    ?: ManaSpentOnSpellsThisTurnComponent()
                container.with(existing.copy(totalSpent = existing.totalSpent + manaSpentThisCast))
            }
        }

        // Pay additional life cost (e.g., Festival of Embers graveyard casting)
        if (action.graveyardLifeCost > 0) {
            val currentLife = currentState.getEntity(action.playerId)
                ?.get<LifeTotalComponent>()?.life ?: 0
            val newLife = currentLife - action.graveyardLifeCost
            currentState = currentState.updateEntity(action.playerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
            currentState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(currentState, action.playerId)
        }

        // Compute target requirements for resolution-time re-validation (Rule 608.2b)
        // Use mode-specific target requirements when a modal mode was chosen at cast time,
        // or kickerTargetRequirements when spell is kicked and alternate targets are defined
        val spellTargetRequirements = if (cardDef != null) {
            val modalEffect = cardDef.script.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
            val baseTargetReqs = if (action.chosenMode != null && modalEffect != null) {
                // Modal spell with mode chosen at cast time — use mode-specific targets
                val mode = modalEffect.modes.getOrNull(action.chosenMode)
                mode?.targetRequirements ?: emptyList()
            } else if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
        } else {
            emptyList()
        }

        // Check if spell requires a creature type choice during casting (e.g., Aphetto Dredging)
        val castTimeChoice = cardDef?.script?.castTimeCreatureTypeChoice
        if (castTimeChoice != null) {
            val pauseResult = pauseForCreatureTypeChoice(
                currentState, action, castTimeChoice, sacrificedPermanentIds, spellTargetRequirements, events
            )
            if (pauseResult != null) return pauseResult
        }

        // Determine if this spell is being cast using warp
        val wasWarped = action.useAlternativeCost && cardDef != null &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Warp }

        // Capture storm count before incrementing (spells cast before this one)
        val stormCount = currentState.spellsCastThisTurn

        // Increment spell count for this turn (global and per-player)
        val playerCount = currentState.playerSpellsCastThisTurn[action.playerId] ?: 0
        currentState = currentState.copy(
            spellsCastThisTurn = stormCount + 1,
            playerSpellsCastThisTurn = currentState.playerSpellsCastThisTurn +
                (action.playerId to playerCount + 1),
            spellWarpedThisTurn = currentState.spellWarpedThisTurn || wasWarped
        )

        // Track spell records cast this turn (for conditional evasion like Relic Runner, and "first of type" triggers)
        run {
            val record = com.wingedsheep.engine.state.CastSpellRecord(
                typeLine = cardComponent.typeLine,
                manaValue = cardComponent.manaValue,
                colors = cardComponent.colors,
                isFaceDown = action.castFaceDown
            )
            val existing = currentState.spellsCastThisTurnByPlayer[action.playerId] ?: emptyList()
            currentState = currentState.copy(
                spellsCastThisTurnByPlayer = currentState.spellsCastThisTurnByPlayer +
                    (action.playerId to existing + record)
            )
        }

        // Check if casting from graveyard via MayPlayPermanentsFromGraveyard (Muldrotha)
        val castingFromGraveyardViaMuldrotha = action.cardId in currentState.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD)) &&
            zoneResolver.hasMayPlayPermanentFromGraveyardPermission(currentState, action.playerId, action.cardId, cardComponent)

        // Cast the spell
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedPermanentIds,
            sacrificedPermanentSubtypes,
            action.castFaceDown,
            action.damageDistribution,
            spellTargetRequirements,
            exiledCardCount = exiledCardCount,
            wasKicked = action.wasKicked,
            wasWarped = wasWarped,
            chosenModes = if (action.chosenMode != null) listOf(action.chosenMode) else emptyList(),
            totalManaSpent = manaSpentThisCast,
            beheldCards = beheldCards
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var currentCastState = castResult.newState
        var allEvents = events + castResult.events

        // Record Muldrotha graveyard cast permission usage
        if (castingFromGraveyardViaMuldrotha) {
            val typeName = zoneResolver.choosePermanentTypeForGraveyardPermission(currentCastState, action.playerId, cardComponent)
            if (typeName != null) {
                currentCastState = zoneResolver.recordGraveyardPlayPermissionUsage(currentCastState, action.playerId, typeName)
            }
        }

        // Handle Storm keyword: create a Storm triggered ability on the stack
        // Check both the card's own keywords and any granted spell keywords (e.g., Ral's storm emblem)
        val hasStormFromGrant = run {
            val playerContainer = currentCastState.getEntity(action.playerId)
            val grants = playerContainer?.get<GrantedSpellKeywordsComponent>()?.grants ?: emptyList()
            val evalContext = PredicateContext(controllerId = action.playerId)
            grants.any { grant ->
                grant.keyword == Keyword.STORM &&
                    predicateEvaluator.matches(currentCastState, action.cardId, grant.spellFilter, evalContext)
            }
        }
        if (!action.castFaceDown && stormCount > 0 && cardDef != null &&
            (cardDef.hasKeyword(Keyword.STORM) || hasStormFromGrant)) {
            val spellEffect = cardDef.script.spellEffect
            if (spellEffect != null) {
                val stormEffect = StormCopyEffect(
                    copyCount = stormCount,
                    spellEffect = spellEffect,
                    spellTargetRequirements = spellTargetRequirements,
                    spellName = cardComponent.name
                )
                val stormAbility = TriggeredAbilityOnStackComponent(
                    sourceId = action.cardId,
                    sourceName = cardComponent.name,
                    controllerId = action.playerId,
                    effect = stormEffect,
                    description = "Storm — copy ${cardComponent.name} $stormCount time(s)"
                )
                val stormResult = stackResolver.putTriggeredAbility(currentCastState, stormAbility)
                if (!stormResult.isSuccess) return stormResult
                currentCastState = stormResult.newState
                allEvents = allEvents + stormResult.events
            }
        }

        // Handle pending spell copies (e.g., Howl of the Horde) — copy next instant/sorcery
        if (!action.castFaceDown && cardComponent.typeLine.let { it.isInstant || it.isSorcery }) {
            val matchingCopies = currentCastState.pendingSpellCopies.filter { it.controllerId == action.playerId }
            if (matchingCopies.isNotEmpty()) {
                val totalCopies = matchingCopies.sumOf { it.copies }
                // Remove consumed pending copies (keep persistent ones like The Mirari Conjecture Ch. III)
                val remainingPending = currentCastState.pendingSpellCopies.filter {
                    it.controllerId != action.playerId || it.persistent
                }
                currentCastState = currentCastState.copy(pendingSpellCopies = remainingPending)

                // Create copies using Storm copy infrastructure
                val spellEffect = cardDef?.script?.spellEffect
                if (spellEffect != null && totalCopies > 0) {
                    val copyEffect = StormCopyEffect(
                        copyCount = totalCopies,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    val copyAbility = TriggeredAbilityOnStackComponent(
                        sourceId = matchingCopies.first().sourceId,
                        sourceName = matchingCopies.first().sourceName,
                        controllerId = action.playerId,
                        effect = copyEffect,
                        description = "Copy ${cardComponent.name} $totalCopies time(s)"
                    )
                    val copyResult = stackResolver.putTriggeredAbility(currentCastState, copyAbility)
                    if (!copyResult.isSuccess) return copyResult
                    currentCastState = copyResult.newState
                    allEvents = allEvents + copyResult.events
                }
            }
        }

        // Detect and process triggers from casting (including additional cost events like sacrifice)
        val triggers = triggerDetector.detectTriggers(currentCastState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentCastState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            allEvents = allEvents + triggerResult.events
            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents
            )
        }

        return ExecutionResult.success(
            currentCastState.withPriority(action.playerId),
            allEvents
        )
    }

    /**
     * Check if the spell needs a creature type choice during casting (e.g., Aphetto Dredging).
     * If so, scan the appropriate zone for creature types and pause for the choice.
     * Returns null if no pause is needed (e.g., no creature types found).
     */
    private fun pauseForCreatureTypeChoice(
        currentState: GameState,
        action: CastSpell,
        source: com.wingedsheep.sdk.model.CastTimeCreatureTypeSource,
        sacrificedPermanentIds: List<EntityId>,
        spellTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        priorEvents: List<GameEvent>
    ): ExecutionResult? {
        // Determine which zone to scan based on source
        val zone = when (source) {
            com.wingedsheep.sdk.model.CastTimeCreatureTypeSource.GRAVEYARD ->
                ZoneKey(action.playerId, Zone.GRAVEYARD)
        }
        val zoneCards = currentState.getZone(zone)

        // Collect creature subtypes and which cards have each type
        val typeToCardIds = mutableMapOf<String, MutableList<EntityId>>()
        for (cardId in zoneCards) {
            val cc = currentState.getEntity(cardId)?.get<CardComponent>() ?: continue
            val typeLine = cc.typeLine
            if (typeLine.isCreature) {
                for (subtype in typeLine.subtypes) {
                    typeToCardIds.getOrPut(subtype.value) { mutableListOf() }.add(cardId)
                }
            }
        }

        // If no creature types found, skip the decision — casting proceeds normally
        if (typeToCardIds.isEmpty()) return null

        val sortedTypes = typeToCardIds.keys.sorted()
        val cardComponent = currentState.getEntity(action.cardId)?.get<CardComponent>()
        val sourceName = cardComponent?.name

        // Build option index → card IDs mapping for client preview
        val optionCardIds = sortedTypes.mapIndexed { index, type ->
            index to typeToCardIds[type]!!.toList()
        }.toMap()

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = action.playerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = action.cardId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            options = sortedTypes,
            optionCardIds = optionCardIds
        )

        val continuation = CastWithCreatureTypeContinuation(
            decisionId = decisionId,
            cardId = action.cardId,
            casterId = action.playerId,
            targets = action.targets,
            xValue = action.xValue,
            sacrificedPermanents = sacrificedPermanentIds,
            targetRequirements = spellTargetRequirements,
            count = 0,
            creatureTypes = sortedTypes
        )

        val pausedState = currentState
            .pushContinuation(continuation)
            .withPendingDecision(decision)

        return ExecutionResult.paused(
            pausedState.withPriority(action.playerId),
            decision,
            priorEvents + DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_OPTION",
                prompt = decision.prompt
            )
        )
    }

    companion object {
        fun create(services: EngineServices): CastSpellHandler {
            return CastSpellHandler(
                services.cardRegistry,
                services.turnManager,
                services.manaSolver,
                services.costCalculator,
                services.alternativePaymentHandler,
                services.costHandler,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
