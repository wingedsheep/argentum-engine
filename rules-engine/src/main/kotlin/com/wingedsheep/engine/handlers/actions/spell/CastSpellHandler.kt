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
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
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
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
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
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.GameEvent as SdkGameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.PermanentSnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.capturePermanentSnapshots
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
    private val targetFinder: com.wingedsheep.engine.handlers.TargetFinder = com.wingedsheep.engine.handlers.TargetFinder(),
) : ActionHandler<CastSpell> {
    override val actionType: KClass<CastSpell> = CastSpell::class

    private val predicateEvaluator = PredicateEvaluator()
    private val zoneResolver = CastZoneResolver(cardRegistry, conditionEvaluator)
    private val paymentProcessor = CastPaymentProcessor(manaSolver, costHandler)
    private val grantedKeywordResolver = com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver(cardRegistry)

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
        val hasForageFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasWarpFromExile && !hasGraveyardLifeCost &&
            zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(state, action.playerId, action.cardId, cardComponent)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasWarpFromExile && !hasGraveyardLifeCost && !hasForageFromGraveyard) {
            return "Card is not in your hand"
        }

        if (hasForageFromGraveyard) {
            if (!costHandler.canPayAdditionalCost(state, AdditionalCost.Forage, action.playerId)) {
                return "Cannot forage: need 3 other cards in graveyard or a Food"
            }
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

        // Choose-N modal shape checks (rules 700.2a / 700.2d). Enforced only when the
        // action arrives with chosenModes populated — the cast-time continuation flow
        // starts with an empty list which falls through to the pause in execute().
        if (cardDef != null && action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                val modalError = validateChosenModeShape(modalEffect, action)
                if (modalError != null) return modalError
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

        // Validate linked-exile granter's additional cost (e.g. Dawnhand Dissident)
        val linkedExileGranter = zoneResolver.findLinkedExileGranter(state, action.playerId, action.cardId)
        val linkedExileAdditionalCost = linkedExileGranter?.additionalCost
        if (linkedExileAdditionalCost != null) {
            val linkedCostError = validateAdditionalCosts(state, listOf(linkedExileAdditionalCost), action)
            if (linkedCostError != null) return linkedCostError
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

        // Validate Conspire optional additional cost (CR 702.78). Two untapped creatures the
        // caster controls, each sharing a color with the spell. The spell must have Conspire
        // either printed or granted (e.g., Raiding Schemes: "Each noncreature spell you cast
        // has conspire").
        if (action.conspiredCreatures.isNotEmpty()) {
            if (cardDef == null) return "Conspire requires a card definition"
            val conspireError = validateConspire(state, action, cardDef)
            if (conspireError != null) return conspireError
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
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    if (evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, action.playerId)
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

        // Apply BlightOrPay "pay mana" adjustment in validation
        if (cardDef != null && !playForFree) {
            val blightOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BlightOrPay>()
                .firstOrNull()
            if (blightOrPay != null) {
                val choseBlight = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true
                if (!choseBlight) {
                    effectiveCost = effectiveCost + ManaCost.parse(blightOrPay.alternativeManaCost)
                }
            }
        }

        // Apply BeholdOrPay "pay mana" adjustment in validation
        if (cardDef != null && !playForFree) {
            val beholdOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BeholdOrPay>()
                .firstOrNull()
            if (beholdOrPay != null) {
                val choseBehold = action.additionalCostPayment?.beheldCards?.isNotEmpty() == true
                if (!choseBehold) {
                    effectiveCost = effectiveCost + ManaCost.parse(beholdOrPay.alternativeManaCost)
                }
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
            alternativePaymentHandler.calculateReducedCost(
                effectiveCost,
                action.alternativePayment,
                cardDef,
                state,
                action.playerId
            )
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
            val baseTargetReqs = if (action.chosenModes.isNotEmpty() && modalEffect != null) {
                // Modal spell with mode(s) chosen at cast time — validate against the union of per-mode requirements.
                action.chosenModes.flatMap { modeIndex ->
                    modalEffect.modes.getOrNull(modeIndex)?.targetRequirements ?: emptyList()
                }
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

    private fun validateConspire(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition
    ): String? {
        if (!grantedKeywordResolver.hasKeyword(state, action.playerId, cardDef, Keyword.CONSPIRE)) {
            return "This spell does not have conspire"
        }
        val chosen = action.conspiredCreatures
        if (chosen.size != 2) return "Conspire requires tapping exactly two creatures"
        if (chosen[0] == chosen[1]) return "Conspire requires two distinct creatures"
        val spellColors = cardDef.colors
        if (spellColors.isEmpty()) return "Cannot conspire: a colorless spell has no color to share"
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        for (creatureId in chosen) {
            if (creatureId !in battlefield) return "Conspire creature is not on the battlefield"
            val container = state.getEntity(creatureId)
                ?: return "Conspire creature not found: $creatureId"
            if (projected.getController(creatureId) != action.playerId) {
                return "Conspire creature is not controlled by you"
            }
            if (!projected.isCreature(creatureId)) return "Conspire requires creatures"
            if (container.has<TappedComponent>()) return "Conspire creature is already tapped"
            val sharesColor = spellColors.any { projected.hasColor(creatureId, it) }
            if (!sharesColor) return "Conspire creature shares no color with this spell"
        }
        return null
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
     * Validates the shape of a choose-N modal cast action (rules 700.2a / 700.2d).
     *
     * Checks: mode indices are in range, chosen count falls within
     * `[minChooseCount, chooseCount]`, duplicates only appear when `allowRepeat`, and
     * `modeTargetsOrdered` (if provided) is aligned 1:1 with `chosenModes`.
     */
    private fun validateChosenModeShape(modalEffect: ModalEffect, action: CastSpell): String? {
        val chosen = action.chosenModes
        for (idx in chosen) {
            if (idx < 0 || idx >= modalEffect.modes.size) {
                return "Invalid mode index: $idx"
            }
        }
        if (chosen.size < modalEffect.minChooseCount) {
            return "Too few modes chosen: ${chosen.size} (minimum ${modalEffect.minChooseCount})"
        }
        if (chosen.size > modalEffect.chooseCount) {
            return "Too many modes chosen: ${chosen.size} (maximum ${modalEffect.chooseCount})"
        }
        if (!modalEffect.allowRepeat && chosen.distinct().size != chosen.size) {
            return "Modes cannot be chosen more than once for this spell"
        }
        if (action.modeTargetsOrdered.isNotEmpty() && action.modeTargetsOrdered.size != chosen.size) {
            return "modeTargetsOrdered size (${action.modeTargetsOrdered.size}) must match chosenModes size (${chosen.size})"
        }
        return null
    }

    /**
     * Resolves the additional costs for a spell, considering per-mode overrides.
     *
     * If any chosen mode specifies its own additionalCosts, costs from every such mode are unioned
     * (rule 700.2h — per-mode additional costs stack). Modes with a null override fall through to
     * the card-level costs. If no chosen mode provides overrides, card-level costs are used.
     */
    private fun counterTypeToCountersString(type: CounterType): String = when (type) {
        CounterType.PLUS_ONE_PLUS_ONE -> Counters.PLUS_ONE_PLUS_ONE
        CounterType.MINUS_ONE_MINUS_ONE -> Counters.MINUS_ONE_MINUS_ONE
        else -> type.name.lowercase()
    }

    /**
     * Resolve the distributed counter removals to apply for a
     * [AdditionalCost.RemoveCountersFromYourCreatures] cost.
     *
     * Prefers the typed `distributedCounterRemovals` field. Falls back to the legacy
     * `counterRemovals: Map<EntityId, Int>` payload (produced by the current web client
     * for counter-distribution costs): for each (entity, amount) it picks counter types
     * greedily from whatever the creature has, so `{creature -> 3}` on a bears with 5
     * +1/+1 counters resolves to removing 3 +1/+1 counters.
     */
    private fun resolveDistributedCounterRemovalsForPayment(
        state: GameState,
        action: CastSpell
    ): List<com.wingedsheep.sdk.scripting.DistributedCounterRemoval> {
        val payment = action.additionalCostPayment ?: return emptyList()
        if (payment.distributedCounterRemovals.isNotEmpty()) return payment.distributedCounterRemovals
        if (payment.counterRemovals.isEmpty()) return emptyList()
        val result = mutableListOf<com.wingedsheep.sdk.scripting.DistributedCounterRemoval>()
        for ((entityId, amount) in payment.counterRemovals) {
            if (amount <= 0) continue
            val counters = state.getEntity(entityId)?.get<CountersComponent>()?.counters ?: continue
            // Greedy fill: prefer +1/+1 first (most common for this flow), then any
            // other type in deterministic order.
            val ordered = counters.entries.sortedWith(
                compareByDescending<Map.Entry<CounterType, Int>> { it.key == CounterType.PLUS_ONE_PLUS_ONE }
                    .thenBy { it.key.name }
            )
            var remaining = amount
            for ((type, available) in ordered) {
                if (remaining <= 0) break
                val take = minOf(remaining, available)
                if (take > 0) {
                    result.add(com.wingedsheep.sdk.scripting.DistributedCounterRemoval(entityId, type, take))
                    remaining -= take
                }
            }
        }
        return result
    }

    private fun resolveAdditionalCostsForMode(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        action: CastSpell
    ): List<AdditionalCost> {
        if (action.chosenModes.isEmpty()) return cardDef.script.additionalCosts
        val modalEffect = cardDef.script.spellEffect as? ModalEffect ?: return cardDef.script.additionalCosts

        val perModeOverrides = action.chosenModes.mapNotNull { modeIndex ->
            modalEffect.modes.getOrNull(modeIndex)?.additionalCosts
        }
        if (perModeOverrides.isEmpty()) return cardDef.script.additionalCosts
        return perModeOverrides.flatten()
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
                is AdditionalCost.BlightOrPay -> {
                    // BlightOrPay: player chose blight if blightTargets is non-empty,
                    // otherwise chose to pay extra mana (validated via mana payment)
                    val blightTargets = action.additionalCostPayment?.blightTargets ?: emptyList()
                    if (blightTargets.isNotEmpty()) {
                        // Validate the blight target
                        val targetId = blightTargets.first()
                        val container = state.getEntity(targetId)
                            ?: return "Blight target not found: $targetId"
                        container.get<CardComponent>()
                            ?: return "Blight target is not a card: $targetId"
                        val controller = projected.getController(targetId)
                        if (controller != action.playerId) {
                            return "You can only blight creatures you control"
                        }
                        if (targetId !in state.getBattlefield()) {
                            return "Blight target is not on the battlefield: $targetId"
                        }
                        if (!projected.isCreature(targetId)) {
                            return "Blight target must be a creature"
                        }
                    }
                    // If blightTargets is empty, the player is paying extra mana instead
                }
                is AdditionalCost.BeholdOrPay -> {
                    // BeholdOrPay: player chose behold if beheldCards is non-empty,
                    // otherwise chose to pay extra mana (validated via mana payment)
                    val beheld = action.additionalCostPayment?.beheldCards ?: emptyList()
                    if (beheld.isNotEmpty()) {
                        val handZone = ZoneKey(action.playerId, Zone.HAND)
                        val handCards = state.getZone(handZone)
                        val battlefieldCards = state.getBattlefield()
                        val context = PredicateContext(controllerId = action.playerId)
                        for (cardId in beheld) {
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
                    // If beheldCards is empty, the player is paying extra mana instead
                }
                is AdditionalCost.RemoveCountersFromYourCreatures -> {
                    // Accept either typed distributedCounterRemovals or the legacy
                    // counterRemovals: Map<EntityId, Int> payload the web client currently
                    // produces for counter-distribution costs.
                    val removals = resolveDistributedCounterRemovalsForPayment(state, action)
                    val total = removals.sumOf { it.count }
                    if (total < additionalCost.totalCount) {
                        return "You must remove ${additionalCost.totalCount} counters from among creatures you control to cast this spell"
                    }
                    // Tally demanded removals per (entity, counterType) so we can validate
                    // against actual counter counts.
                    val demanded = mutableMapOf<Pair<EntityId, CounterType>, Int>()
                    for (removal in removals) {
                        if (removal.count <= 0) {
                            return "Counter removal count must be positive"
                        }
                        val permContainer = state.getEntity(removal.entityId)
                            ?: return "Counter removal target not found: ${removal.entityId}"
                        permContainer.get<CardComponent>()
                            ?: return "Counter removal target is not a card: ${removal.entityId}"
                        if (projected.getController(removal.entityId) != action.playerId) {
                            return "You can only remove counters from creatures you control"
                        }
                        if (removal.entityId !in state.getBattlefield()) {
                            return "Counter removal target is not on the battlefield"
                        }
                        if (!projected.isCreature(removal.entityId)) {
                            return "Counter removal target must be a creature"
                        }
                        val key = removal.entityId to removal.counterType
                        demanded[key] = (demanded[key] ?: 0) + removal.count
                    }
                    for ((key, demandedCount) in demanded) {
                        val (entityId, counterType) = key
                        val actual = state.getEntity(entityId)
                            ?.get<CountersComponent>()
                            ?.getCount(counterType) ?: 0
                        if (actual < demandedCount) {
                            return "Creature does not have $demandedCount $counterType counters to remove"
                        }
                    }
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

        // Cast-time mode selection for choose-N modal spells (rules 601.2b, 700.2).
        // Must run before cost payment so cancellation leaves no side effects.
        val modalEffect = cardDef?.script?.spellEffect as? ModalEffect
        if (modalEffect != null && action.chosenModes.isEmpty() && modalEffect.chooseCount > 1) {
            return pauseForCastTimeModeSelection(currentState, action, cardComponent, modalEffect)
        }

        // Capture the linked-exile granter (if any) before the cast removes the card from
        // exile — once the spell moves to the stack the LinkedExileComponent lookup would
        // fail, but we still need the entry to enforce once-per-turn marking after a
        // successful cast.
        val linkedExileGranterEntry = zoneResolver.findLinkedExileGranterEntry(currentState, action.playerId, action.cardId)

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
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    if (evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, evokeAbility.cost, action.playerId)
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

        // Apply per-mode additional mana cost (e.g., Feed the Cycle "pay {B}" mode).
        // With choose-N (rule 700.2h), the additional mana cost of every chosen mode stacks.
        if (cardDef != null && action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                for (modeIndex in action.chosenModes) {
                    val modeManaCost = modalEffect.modes.getOrNull(modeIndex)?.additionalManaCost ?: continue
                    effectiveCost = effectiveCost + ManaCost.parse(modeManaCost)
                }
            }
        }

        // Apply BlightOrPay: if player chose "pay mana" path (no blight targets), add extra mana
        if (cardDef != null && !playForFreeInExecute) {
            val blightOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BlightOrPay>()
                .firstOrNull()
            if (blightOrPay != null) {
                val choseBlight = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true
                if (!choseBlight) {
                    effectiveCost = effectiveCost + ManaCost.parse(blightOrPay.alternativeManaCost)
                }
            }
        }

        // Apply BeholdOrPay: if player chose "pay mana" path (no beheld cards), add extra mana
        if (cardDef != null && !playForFreeInExecute) {
            val beholdOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BeholdOrPay>()
                .firstOrNull()
            if (beholdOrPay != null) {
                val choseBehold = action.additionalCostPayment?.beheldCards?.isNotEmpty() == true
                if (!choseBehold) {
                    effectiveCost = effectiveCost + ManaCost.parse(beholdOrPay.alternativeManaCost)
                }
            }
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedSnapshots = mutableListOf<PermanentSnapshot>()
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

            // Linked-exile granter additional cost (e.g., Dawnhand Dissident's
            // "remove three counters from among creatures you control")
            val linkedGranter = zoneResolver.findLinkedExileGranter(currentState, action.playerId, action.cardId)
            linkedGranter?.additionalCost?.let { add(it) }
        }

        val flattenedAllCosts = allAdditionalCosts.flatMap {
            if (it is AdditionalCost.Composite) it.steps else listOf(it)
        }
        if (flattenedAllCosts.isNotEmpty() && action.additionalCostPayment != null) {
            for (additionalCost in flattenedAllCosts) {
                when (additionalCost) {
                    is AdditionalCost.SacrificePermanent -> {
                        // Snapshot projected subtypes and P/T before zone change
                        // (Rule 112.7a / 608.2h — "as it last existed on the battlefield")
                        val projectedBeforeSacrifice = currentState.projectedState
                        sacrificedSnapshots.addAll(
                            capturePermanentSnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                        )
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

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
                        sacrificedSnapshots.addAll(
                            capturePermanentSnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                        )
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

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
                            val battlefield = currentState.getBattlefield()
                            val anyOnBattlefield = chosen.any { it in battlefield }
                            events.add(CardsRevealedEvent(
                                revealingPlayerId = action.playerId,
                                cardIds = chosen,
                                cardNames = cardNames,
                                imageUris = imageUris,
                                source = cardComponent.name,
                                // Deliver to the revealing player when the beheld card is on the
                                // battlefield (public info) so their client can show the behold
                                // pulse. Suppress when revealing from hand — the caster already
                                // knows and the reveal overlay would be redundant.
                                revealToSelf = anyOnBattlefield
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
                    is AdditionalCost.BlightOrPay -> {
                        // Apply -1/-1 counters if the player chose the blight path
                        val blightTargets = action.additionalCostPayment.blightTargets
                        if (blightTargets.isNotEmpty()) {
                            val targetId = blightTargets.first()
                            val targetContainer = currentState.getEntity(targetId)
                            if (targetContainer != null) {
                                val counters = targetContainer.get<CountersComponent>() ?: CountersComponent()
                                currentState = currentState.updateEntity(targetId) { c ->
                                    c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, additionalCost.blightAmount))
                                }
                                val targetName = targetContainer.get<CardComponent>()?.name ?: "Creature"
                                events.add(CountersAddedEvent(
                                    entityId = targetId,
                                    counterType = Counters.MINUS_ONE_MINUS_ONE,
                                    amount = additionalCost.blightAmount,
                                    entityName = targetName
                                ))
                            }
                        }
                        // If blightTargets is empty, "pay mana" path — extra mana already added to effectiveCost
                    }
                    is AdditionalCost.BeholdOrPay -> {
                        // Store beheld card IDs in pipeline and reveal them, if behold path chosen
                        val chosen = action.additionalCostPayment.beheldCards
                        if (chosen.isNotEmpty()) {
                            beheldCards.addAll(chosen)
                            costPipelineCollections[additionalCost.storeAs] = chosen

                            val cardNames = chosen.mapNotNull { currentState.getEntity(it)?.get<CardComponent>()?.name }
                            val imageUris = chosen.map { id ->
                                val defId = currentState.getEntity(id)?.get<CardComponent>()?.cardDefinitionId
                                defId?.let { cardRegistry.getCard(it)?.metadata?.imageUri }
                            }
                            val battlefield = currentState.getBattlefield()
                            val anyOnBattlefield = chosen.any { it in battlefield }
                            events.add(CardsRevealedEvent(
                                revealingPlayerId = action.playerId,
                                cardIds = chosen,
                                cardNames = cardNames,
                                imageUris = imageUris,
                                source = cardComponent.name,
                                revealToSelf = anyOnBattlefield
                            ))
                        }
                        // If beheldCards is empty, "pay mana" path — extra mana already added to effectiveCost
                    }
                    is AdditionalCost.RemoveCountersFromYourCreatures -> {
                        // Remove the chosen counters from the designated creatures.
                        // Accept either typed distributedCounterRemovals or the legacy
                        // counterRemovals: Map<EntityId, Int> (picks any counter type).
                        val resolvedRemovals = resolveDistributedCounterRemovalsForPayment(
                            currentState, action
                        )
                        for (removal in resolvedRemovals) {
                            val container = currentState.getEntity(removal.entityId) ?: continue
                            val existing = container.get<CountersComponent>() ?: continue
                            currentState = currentState.updateEntity(removal.entityId) { c ->
                                c.with(existing.withRemoved(removal.counterType, removal.count))
                            }
                            val entityName = container.get<CardComponent>()?.name ?: "Creature"
                            events.add(com.wingedsheep.engine.core.CountersRemovedEvent(
                                entityId = removal.entityId,
                                counterType = counterTypeToCountersString(removal.counterType),
                                amount = removal.count,
                                entityName = entityName
                            ))
                        }
                    }
                    else -> {}
                }
            }
        }

        // Pay Conspire's optional additional cost: tap the two chosen creatures (CR 702.78).
        // Validated in validate(); we just apply the tap and emit TappedEvent so "becomes
        // tapped" self-triggers fire (mirrors the attack-declare TappedEvent fix).
        if (action.conspiredCreatures.isNotEmpty()) {
            for (creatureId in action.conspiredCreatures) {
                val creatureContainer = currentState.getEntity(creatureId) ?: continue
                if (!creatureContainer.has<TappedComponent>()) {
                    currentState = currentState.updateEntity(creatureId) { c ->
                        c.with(TappedComponent)
                    }
                    val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Creature"
                    events.add(TappedEvent(creatureId, creatureName))
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

        // Pay forage additional cost when casting a creature from graveyard via
        // MayCastCreaturesFromGraveyardWithForageComponent (e.g., Osteomancer Adept).
        // Auto-pay: prefer sacrificing a Food; otherwise exile 3 other graveyard cards.
        val isForageCast = zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(
            currentState, action.playerId, action.cardId, cardComponent
        ) && action.cardId in currentState.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
        if (isForageCast) {
            val projected = currentState.projectedState
            val foods = currentState.getBattlefield().filter { permId ->
                currentState.getEntity(permId) != null &&
                    projected.getController(permId) == action.playerId &&
                    projected.hasSubtype(permId, Subtype.FOOD.value)
            }
            if (foods.isNotEmpty()) {
                val foodId = foods.first()
                val foodContainer = currentState.getEntity(foodId)
                val foodName = foodContainer?.get<CardComponent>()?.name ?: "Food"
                val foodController = foodContainer?.get<ControllerComponent>()?.playerId ?: action.playerId
                currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .trackFoodSacrifice(currentState, listOf(foodId), foodController)
                val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .moveToZone(currentState, foodId, Zone.GRAVEYARD)
                currentState = transition.state
                events.add(PermanentsSacrificedEvent(foodController, listOf(foodId), listOf(foodName)))
                events.addAll(transition.events)
            } else {
                val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
                val toExile = currentState.getZone(graveyardZone)
                    .filter { it != action.cardId }
                    .take(3)
                if (toExile.size < 3) {
                    return ExecutionResult.error(currentState, "Cannot forage: need 3 other cards in graveyard or a Food")
                }
                for (exileId in toExile) {
                    val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                        .moveToZone(currentState, exileId, Zone.EXILE)
                    currentState = transition.state
                    events.addAll(transition.events)
                }
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

        // Compute target requirements for resolution-time re-validation (Rule 608.2b).
        // For modal spells with cast-time mode picks, union the per-mode requirements so resolution can
        // re-check every targeted slot. Per-mode breakdown is persisted on SpellOnStackComponent.modeTargetRequirements.
        val modalEffectForTargets = cardDef?.script?.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
        val perModeTargetRequirements: Map<Int, List<TargetRequirement>> =
            if (modalEffectForTargets != null && action.chosenModes.isNotEmpty()) {
                action.chosenModes.distinct().associateWith { idx ->
                    modalEffectForTargets.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
            } else emptyMap()

        val spellTargetRequirements = if (cardDef != null) {
            val baseTargetReqs = if (action.chosenModes.isNotEmpty() && modalEffectForTargets != null) {
                // Modal spell with modes chosen at cast time — union per-mode requirements
                action.chosenModes.flatMap { idx ->
                    modalEffectForTargets.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
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
                currentState, action, castTimeChoice, sacrificedSnapshots, spellTargetRequirements, events
            )
            if (pauseResult != null) return pauseResult
        }

        // Determine if this spell is being cast using warp
        val wasWarped = action.useAlternativeCost && cardDef != null &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Warp }

        // Determine if this spell is being cast using evoke
        val wasEvoked = action.useAlternativeCost && cardDef != null &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Evoke }

        // Extract per-color mana spent from payment events (for mana-spent-gated triggers)
        val manaSpentEvent = paymentResult.events.filterIsInstance<ManaSpentEvent>().firstOrNull()

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

        // Derive per-mode target groups from the flat target list when the action arrived
        // with chosenModes but no modeTargetsOrdered (current web-client cast-time UI for
        // choose-1 modal spells). Slice action.targets in mode order using each mode's
        // total target slot count so modal resolution can read per-mode targets.
        val effectiveModeTargetsOrdered = if (
            action.modeTargetsOrdered.isEmpty() &&
            action.chosenModes.isNotEmpty() &&
            modalEffectForTargets != null &&
            action.targets.isNotEmpty()
        ) {
            deriveModeTargetsFromFlat(modalEffectForTargets, action.chosenModes, action.targets)
        } else {
            action.modeTargetsOrdered
        }

        // Cast the spell
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedSnapshots,
            castFaceDown = action.castFaceDown,
            damageDistribution = action.damageDistribution,
            targetRequirements = spellTargetRequirements,
            exiledCardCount = exiledCardCount,
            wasKicked = action.wasKicked,
            wasWarped = wasWarped,
            wasEvoked = wasEvoked,
            chosenModes = action.chosenModes,
            modeTargetsOrdered = effectiveModeTargetsOrdered,
            modeTargetRequirements = perModeTargetRequirements,
            modeDamageDistribution = action.modeDamageDistribution,
            totalManaSpent = manaSpentThisCast,
            beheldCards = beheldCards,
            manaSpentWhite = manaSpentEvent?.white ?: 0,
            manaSpentBlue = manaSpentEvent?.blue ?: 0,
            manaSpentBlack = manaSpentEvent?.black ?: 0,
            manaSpentRed = manaSpentEvent?.red ?: 0,
            manaSpentGreen = manaSpentEvent?.green ?: 0,
            manaSpentColorless = manaSpentEvent?.colorless ?: 0
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

        // Record once-per-turn linked-exile permission usage (e.g., Maralen, Fae Ascendant).
        // Captured against the pre-cast state since the card has now left exile and the granter
        // would no longer be located via its LinkedExileComponent.
        if (linkedExileGranterEntry?.ability?.oncePerTurn == true) {
            currentCastState = currentCastState.updateEntity(linkedExileGranterEntry.granterId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent)
            }
        }

        // Handle Storm keyword: build one PendingTrigger per instance of Storm.
        // Per CR 702.40b each instance of Storm triggers separately. Sources of Storm:
        //   1. The card's printed keyword (Keyword.STORM in keywords) — counts once.
        //   2. Each matching grant in GrantedSpellKeywordsComponent (e.g., Ral's storm emblem) —
        //      counts once per matching grant.
        // Per CR 702.40a Storm triggers whenever the spell is cast; it copies zero times when
        // no other spells have been cast this turn. The executor is a no-op at copyCount == 0
        // but the trigger must still land on the stack so "whenever an ability triggers /
        // is put onto the stack" effects see it.
        val stormGrantCount = run {
            val playerContainer = currentCastState.getEntity(action.playerId)
            val grants = playerContainer?.get<GrantedSpellKeywordsComponent>()?.grants ?: emptyList()
            val evalContext = PredicateContext(controllerId = action.playerId)
            grants.count { grant ->
                grant.keyword == Keyword.STORM &&
                    predicateEvaluator.matches(currentCastState, action.cardId, grant.spellFilter, evalContext)
            }
        }
        val printedStormCount = if (cardDef != null && cardDef.hasKeyword(Keyword.STORM)) 1 else 0
        val stormInstanceCount = printedStormCount + stormGrantCount
        val stormPendingTriggers: List<PendingTrigger> =
            if (!action.castFaceDown && cardDef != null && stormInstanceCount > 0) {
                val spellEffect = cardDef.script.spellEffect
                if (spellEffect != null) {
                    List(stormInstanceCount) {
                        val stormEffect = StormCopyEffect(
                            copyCount = stormCount,
                            spellEffect = spellEffect,
                            spellTargetRequirements = spellTargetRequirements,
                            spellName = cardComponent.name
                        )
                        val ability = TriggeredAbility(
                            id = AbilityId.generate(),
                            trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
                            binding = TriggerBinding.SELF,
                            effect = stormEffect,
                            activeZone = Zone.STACK,
                            descriptionOverride = "Storm — copy ${cardComponent.name} $stormCount time(s)"
                        )
                        PendingTrigger(
                            ability = ability,
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = action.cardId,
                                triggeringPlayerId = action.playerId
                            )
                        )
                    }
                } else emptyList()
            } else emptyList()

        // Handle Conspire (CR 702.78): when the optional additional cost was paid, a reflexive
        // trigger goes on the stack above the spell: "When you do, copy it and you may choose
        // new targets for the copy." Reuses StormCopyEffect with copyCount=1 so the existing
        // retargeting, modal-copy, and SpellOnStackComponent-clone plumbing applies unchanged.
        val conspirePendingTriggers: List<PendingTrigger> =
            if (!action.castFaceDown && cardDef != null && action.conspiredCreatures.isNotEmpty()) {
                val spellEffect = cardDef.script.spellEffect
                if (spellEffect != null) {
                    val copyEffect = StormCopyEffect(
                        copyCount = 1,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    val ability = TriggeredAbility(
                        id = AbilityId.generate(),
                        trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
                        binding = TriggerBinding.SELF,
                        effect = copyEffect,
                        activeZone = Zone.STACK,
                        descriptionOverride = "Conspire — copy ${cardComponent.name}"
                    )
                    listOf(
                        PendingTrigger(
                            ability = ability,
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = action.cardId,
                                triggeringPlayerId = action.playerId
                            )
                        )
                    )
                } else emptyList()
            } else emptyList()

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
                    // sourceId must point to the spell being copied (action.cardId), not the
                    // originating permanent (e.g., Howl of the Horde). StormCopyEffectExecutor
                    // uses sourceId to clone the SpellOnStackComponent via putSpellCopy (Phase 1
                    // of spell-copies-as-spells); the originating permanent may be in the
                    // graveyard by the time the trigger resolves.
                    val copyAbility = TriggeredAbilityOnStackComponent(
                        sourceId = action.cardId,
                        sourceName = cardComponent.name,
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

        // Detect and process triggers from casting (including additional cost events like sacrifice).
        // Storm pending triggers (built above) are prepended so they go on the stack just above the
        // spell itself — per CR 603.3b Storm goes on top of the spell that caused it to trigger.
        // Other AP spell-cast triggers follow (placed higher on the stack), then NAP triggers on top,
        // matching APNAP ordering within processTriggers.
        val detectedTriggers = triggerDetector.detectTriggers(currentCastState, allEvents)
        val triggers = conspirePendingTriggers + stormPendingTriggers + detectedTriggers
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
        sacrificedSnapshots: List<PermanentSnapshot>,
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
            sacrificedPermanents = sacrificedSnapshots,
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

    /**
     * Initial entry point for choose-N modal cast-time mode selection (rule 700.2).
     *
     * Pre-filters modes by 700.2a target legality, then pauses with a ChooseOption
     * decision in the CASTING phase. The resumer iterates until `chooseCount` modes
     * are picked (or "Done" fires once `minChooseCount` is satisfied), then
     * transitions to per-mode target selection or directly back into [execute] with
     * a fully populated action.
     */
    private fun pauseForCastTimeModeSelection(
        currentState: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        modalEffect: ModalEffect
    ): ExecutionResult {
        val available = modalEffect.modes.withIndex()
            .filter { (_, mode) -> modeHasSatisfiableTargets(currentState, action.playerId, action.cardId, mode) }
            .map { it.index }

        if (available.size < modalEffect.minChooseCount) {
            return ExecutionResult.error(currentState, "No legal mode selection available for ${cardComponent.name}")
        }

        return presentCastModalModeDecision(
            state = currentState,
            cardId = action.cardId,
            casterId = action.playerId,
            cardName = cardComponent.name,
            baseCastAction = action,
            modalEffect = modalEffect,
            selectedModeIndices = emptyList(),
            availableIndices = if (modalEffect.allowRepeat) null else available,
            repeatAvailableIndices = if (modalEffect.allowRepeat) available else null
        )
    }

    /**
     * Check whether a modal mode can potentially be cast — either it has no targets, or
     * at least one legal target exists for each of its [TargetRequirement]s (rule 700.2a).
     */
    private fun modeHasSatisfiableTargets(
        state: GameState,
        casterId: EntityId,
        sourceId: EntityId,
        mode: com.wingedsheep.sdk.scripting.effects.Mode
    ): Boolean {
        if (mode.targetRequirements.isEmpty()) return true
        return mode.targetRequirements.all { req ->
            req.effectiveMinCount == 0 ||
                targetFinder.findLegalTargets(state, req, casterId, sourceId).isNotEmpty()
        }
    }

    /**
     * Build a ChooseOptionDecision + CastModalModeSelectionContinuation for the next
     * mode pick. Shared between the initial pause (here) and the iterative resumer.
     */
    internal fun presentCastModalModeDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modalEffect: ModalEffect,
        selectedModeIndices: List<Int>,
        availableIndices: List<Int>?,
        repeatAvailableIndices: List<Int>?
    ): ExecutionResult {
        val offerIndices = availableIndices ?: repeatAvailableIndices ?: modalEffect.modes.indices.toList()
        val doneOffered = selectedModeIndices.size >= modalEffect.minChooseCount &&
            selectedModeIndices.size < modalEffect.chooseCount

        val optionLabels = offerIndices.map { modalEffect.modes[it].description } +
            (if (doneOffered) listOf("Done") else emptyList())

        val decisionId = java.util.UUID.randomUUID().toString()
        val pickNumber = selectedModeIndices.size + 1
        val alreadyPicked = if (selectedModeIndices.isNotEmpty()) {
            val labels = selectedModeIndices.map { modalEffect.modes[it].description }
            "\nAlready picked: ${labels.joinToString("; ")}"
        } else ""
        val prompt = "Choose a mode for $cardName ($pickNumber of ${modalEffect.chooseCount})$alreadyPicked"
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = casterId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = cardId,
                sourceName = cardName,
                phase = DecisionPhase.CASTING
            ),
            options = optionLabels,
            // Cast-time mode selection must be cancellable (rule 601.2b–c, K1 in plan):
            // the pause happens before any cost is paid, so aborting is safe.
            canCancel = true
        )

        val continuation = com.wingedsheep.engine.core.CastModalModeSelectionContinuation(
            decisionId = decisionId,
            cardId = cardId,
            casterId = casterId,
            baseCastAction = baseCastAction,
            modes = modalEffect.modes,
            chooseCount = modalEffect.chooseCount,
            minChooseCount = modalEffect.minChooseCount,
            allowRepeat = modalEffect.allowRepeat,
            offeredIndices = offerIndices,
            availableIndices = availableIndices,
            selectedModeIndices = selectedModeIndices,
            doneOptionOffered = doneOffered
        )

        val pausedState = state
            .pushContinuation(continuation)
            .withPendingDecision(decision)
            .withPriority(casterId)

        return ExecutionResult.paused(
            pausedState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = casterId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Build a ChooseTargetsDecision + CastModalTargetSelectionContinuation for the next
     * mode that needs targets. Skips modes whose requirements are empty, advancing the
     * ordinal and appending an empty target list until it finds one that needs targets
     * or all modes are resolved.
     */
    internal fun presentCastModalTargetDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modes: List<com.wingedsheep.sdk.scripting.effects.Mode>,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>,
        currentOrdinal: Int
    ): ExecutionResult {
        var ordinal = currentOrdinal
        var targetsAccum = resolvedModeTargets

        while (ordinal < chosenModeIndices.size) {
            val modeIndex = chosenModeIndices[ordinal]
            val mode = modes[modeIndex]
            if (mode.targetRequirements.isEmpty()) {
                targetsAccum = targetsAccum + listOf(emptyList())
                ordinal++
                continue
            }

            // Find legal targets per requirement. If any required slot has no legal
            // targets (and is mandatory), this mode can't resolve — surface an error.
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = mode.targetRequirements.mapIndexed { index, req ->
                val legal = targetFinder.findLegalTargets(state, req, casterId, cardId)
                legalTargetsMap[index] = legal
                com.wingedsheep.engine.core.TargetRequirementInfo(
                    index = index,
                    description = req.description,
                    minTargets = req.effectiveMinCount,
                    maxTargets = req.count
                )
            }
            val allSatisfied = requirementInfos.all { info ->
                (legalTargetsMap[info.index]?.isNotEmpty() == true) || info.minTargets == 0
            }
            if (!allSatisfied) {
                return ExecutionResult.error(state, "No legal targets for mode: ${mode.description}")
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val pickNumber = ordinal + 1
            val prompt = "Choose targets for $cardName — ${mode.description} ($pickNumber of ${chosenModeIndices.size})"
            val decision = com.wingedsheep.engine.core.ChooseTargetsDecision(
                id = decisionId,
                playerId = casterId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = cardId,
                    sourceName = cardName,
                    phase = DecisionPhase.CASTING,
                    effectHint = mode.description
                ),
                targetRequirements = requirementInfos,
                legalTargets = legalTargetsMap,
                // Cast-time per-mode target selection must be cancellable (K2 in plan):
                // the pause sits before cost payment, so aborting rolls back cleanly.
                canCancel = true
            )

            val continuation = com.wingedsheep.engine.core.CastModalTargetSelectionContinuation(
                decisionId = decisionId,
                cardId = cardId,
                casterId = casterId,
                baseCastAction = baseCastAction,
                modes = modes,
                chosenModeIndices = chosenModeIndices,
                resolvedModeTargets = targetsAccum,
                currentOrdinal = ordinal
            )

            val pausedState = state
                .pushContinuation(continuation)
                .withPendingDecision(decision)
                .withPriority(casterId)

            return ExecutionResult.paused(
                pausedState,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = casterId,
                        decisionType = "CHOOSE_TARGETS",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // All modes resolved without needing another decision — finalize directly.
        return finalizeModalCast(state, baseCastAction, chosenModeIndices, targetsAccum)
    }

    /**
     * Complete a choose-N modal cast by re-entering [execute] with a finalized
     * [CastSpell] action. `chosenModes`, `modeTargetsOrdered`, and the flat `targets`
     * union are populated so the normal cost / target / stack flow runs exactly once.
     */
    internal fun finalizeModalCast(
        state: GameState,
        baseCastAction: CastSpell,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>
    ): ExecutionResult {
        val flatTargets = resolvedModeTargets.flatten()
        val finalAction = baseCastAction.copy(
            chosenModes = chosenModeIndices,
            modeTargetsOrdered = resolvedModeTargets,
            targets = flatTargets
        )
        return execute(state, finalAction)
    }

    /**
     * Slice a flat target list into per-mode groups using each chosen mode's total
     * target slot count. Used when an action arrives with [CastSpell.chosenModes] and
     * [CastSpell.targets] populated but [CastSpell.modeTargetsOrdered] empty (the
     * web-client choose-1 modal cast path), so resolution can read targets per mode.
     *
     * If the flat target count doesn't line up with the modes' summed slot counts
     * (truncated, missing optional slots, etc.), returns an empty list — the cast
     * proceeds with the pre-existing flat-targets behavior rather than risking a
     * mis-sliced binding.
     */
    private fun deriveModeTargetsFromFlat(
        modalEffect: com.wingedsheep.sdk.scripting.effects.ModalEffect,
        chosenModes: List<Int>,
        flatTargets: List<ChosenTarget>
    ): List<List<ChosenTarget>> {
        // Choose-1: all flat targets belong to the single chosen mode. Using the mode's
        // max `count` here would mis-slice "up to N target" modes when the player picks
        // fewer than the maximum (e.g. Dewdrop Cure's "return up to two/three").
        if (chosenModes.size == 1) {
            return listOf(flatTargets.toList())
        }

        val perModeSlotCounts = chosenModes.map { idx ->
            modalEffect.modes.getOrNull(idx)?.targetRequirements?.sumOf { it.count } ?: 0
        }
        if (perModeSlotCounts.sum() != flatTargets.size) return emptyList()

        val result = mutableListOf<List<ChosenTarget>>()
        var cursor = 0
        for (slotCount in perModeSlotCounts) {
            result.add(flatTargets.subList(cursor, cursor + slotCount).toList())
            cursor += slotCount
        }
        return result
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
                services.triggerProcessor,
                services.targetFinder
            )
        }
    }
}
