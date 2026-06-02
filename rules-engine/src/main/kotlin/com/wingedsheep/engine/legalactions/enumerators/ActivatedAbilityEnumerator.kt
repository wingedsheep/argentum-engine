package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.CounterRemovalCreatureData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType

/**
 * Enumerates non-mana activated abilities on battlefield permanents.
 *
 * Handles:
 * 1. Own permanents: non-mana activated abilities (own + granted + static)
 * 2. Opponent permanents: "any player may activate" abilities
 */
class ActivatedAbilityEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        enumerateOwnPermanents(context, result)
        enumerateAnyPlayerMayAbilities(context, result)
        return result
    }

    /**
     * Non-mana activated abilities on permanents controlled by the player.
     */
    private fun enumerateOwnPermanents(context: EnumerationContext, result: MutableList<LegalAction>) {
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 708.2)
            if (container.has<FaceDownComponent>()) continue

            // Activated abilities of permanents matching a PreventActivatedAbilities filter
            // (Cursed Totem etc.) can't be activated — applies to both mana and non-mana
            // abilities, including those granted by static effects.
            if (context.castPermissionUtils.isActivationPrevented(state, entityId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name)
            // Include granted activated abilities alongside the card's own abilities (both temporary and static)
            val grantedAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            val staticAbilities = context.castPermissionUtils.getStaticGrantedActivatedAbilities(entityId, state)
            val allAbilities = grantedAbilities + staticAbilities

            // If no card definition (e.g., tokens) and no granted/static abilities, skip
            if (cardDef == null && allAbilities.isEmpty()) continue

            // Get class level for Class enchantments (null for non-Class cards)
            val classLevelComponent = container.get<ClassLevelComponent>()
            val classLevel = classLevelComponent?.currentLevel

            // If entity lost all abilities, suppress its own non-mana abilities
            val ownNonManaAbilities = if (cardDef == null || projected.hasLostAllAbilities(entityId)) emptyList()
            else cardDef.script.effectiveActivatedAbilities(classLevel).filter { !it.isManaAbility && it.activateFromZone == Zone.BATTLEFIELD }

            // Generate level-up abilities for Class enchantments
            val levelUpAbilities = if (cardDef != null && classLevelComponent != null && !projected.hasLostAllAbilities(entityId)) {
                generateClassLevelUpAbilities(cardDef, classLevelComponent)
            } else emptyList()

            val nonManaAbilities = ownNonManaAbilities + levelUpAbilities + allAbilities.filter { !it.isManaAbility }

            // Apply text-changing effects to ability costs and targets
            val textReplacement = container.get<TextReplacementComponent>()

            for (ability in nonManaAbilities) {
                // Sorcery-speed abilities: skip during non-main phases / opponent's turn
                if (ability.timing == TimingRule.SorcerySpeed && !context.canPlaySorcerySpeed) continue

                // Planeswalker loyalty abilities: sorcery speed + once per turn + loyalty cost check
                if (ability.isPlaneswalkerAbility) {
                    if (context.cantActivateLoyaltyAbilities) continue
                    if (!context.canPlaySorcerySpeed) continue
                    val tracker = container.get<AbilityActivatedThisTurnComponent>()
                    if (tracker != null && tracker.loyaltyActivationCount > 0) {
                        val maxActivations = context.castPermissionUtils.getMaxLoyaltyActivations(state, playerId)
                        if (tracker.hasReachedLoyaltyLimit(maxActivations)) continue
                    }
                    // Check loyalty cost payability for negative costs
                    val loyaltyCost = ability.cost as? AbilityCost.Loyalty
                    if (loyaltyCost != null && loyaltyCost.change < 0) {
                        val counters = container.get<CountersComponent>()
                        val currentLoyalty = counters?.getCount(CounterType.LOYALTY) ?: 0
                        if (currentLoyalty < -loyaltyCost.change) continue
                    }
                }

                // Apply text replacement to cost filters (e.g., "Sacrifice a Goblin" -> "Sacrifice a Bird")
                val rawCost = if (textReplacement != null) {
                    ability.cost.applyTextReplacement(textReplacement)
                } else {
                    ability.cost
                }
                // Apply ability-specific generic cost reduction so payability is checked against
                // the locked-in cost (e.g., The Dominion Bracelet — "{X} less, where X is this
                // creature's power").
                val effectiveCost = applyAbilityGenericCostReduction(rawCost, ability, state, entityId, playerId, context)

                // Description shown to the player. When the ability has a generic cost reduction
                // that's currently active, rebuild the prefix from [effectiveCost] so the menu
                // reflects what the player will actually pay (e.g., Starport Security drops from
                // "{3}{W}, {T}: ..." to "{1}{W}, {T}: ..." once a +1/+1-counter creature is in
                // play). Otherwise fall through to [ability.description], which honours any
                // descriptionOverride the card defined.
                val displayDescription =
                    if (ability.genericCostReduction != null && effectiveCost != rawCost) {
                        "${effectiveCost.description}: ${ability.effect.description}"
                    } else {
                        ability.description
                    }

                // Ability payment context — lets the solver consider restricted mana that's
                // only spendable on this kind of activation (e.g., Steelswarm Operator's mana
                // restricted to abilities of artifact sources).
                val abilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(cardComponent, projected, entityId)

                // Check cost requirements and gather sacrifice/tap/bounce targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null
                var bounceTargets: List<EntityId>? = null
                var bounceCost: AbilityCost.ReturnToHand? = null
                var hasForageCost = false
                var forageGraveyardCards: List<EntityId> = emptyList()
                var forageFoodTargets: List<EntityId> = emptyList()
                var blightCost: AbilityCost.Blight? = null
                var blightCreatures: List<EntityId> = emptyList()
                var discardCost: AbilityCost.Discard? = null
                var discardTargets: List<EntityId>? = null
                var craftCost: AbilityCost.Craft? = null
                var craftMaterials: List<EntityId> = emptyList()
                var costAffordable = true

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (container.has<TappedComponent>()) continue
                        if (!cardComponent.typeLine.isLand && projected.isCreature(entityId)) {
                            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                            val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                        if (attachedId == null) continue
                        val attachedEntity = state.getEntity(attachedId) ?: continue
                        if (attachedEntity.has<TappedComponent>()) continue
                        if (projected.isCreature(attachedId)) {
                            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                            val hasHaste = projected.hasKeyword(attachedId, Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.Mana -> {
                        if (!context.manaSolver.canPay(state, playerId, effectiveCost.cost, precomputedSources = context.availableManaSources, spellContext = abilityContext)) {
                            // If the ability has convoke, check if affordable with convoke creatures
                            if (ability.hasConvoke) {
                                val creatures = context.costUtils.findConvokeCreatures(state, playerId)
                                if (!context.costUtils.canAffordWithConvoke(state, playerId, effectiveCost.cost, creatures, precomputedSources = context.availableManaSources)) {
                                    costAffordable = false
                                }
                            } else {
                                costAffordable = false
                            }
                        }
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                            state, playerId, sacrificeCost.filter, if (sacrificeCost.excludeSelf) entityId else null
                        )
                        if (sacrificeTargets.size < sacrificeCost.count) continue
                    }
                    is AbilityCost.ReturnToHand -> {
                        bounceCost = effectiveCost
                        bounceTargets = context.costUtils.findAbilityBounceTargets(state, playerId, bounceCost.filter)
                        if (bounceTargets.size < bounceCost.count) continue
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType == null) continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = effectiveCost
                        tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, tapCost.filter)
                            .let { targets -> if (tapCost.excludeSelf) targets.filter { it != entityId } else targets }
                        if (tapTargets.size < tapCost.count) continue
                    }
                    is AbilityCost.SacrificeSelf -> {
                        // Source must be on battlefield (always true when iterating battlefield)
                        sacrificeTargets = listOf(entityId)
                    }
                    is AbilityCost.Blight -> {
                        blightCost = effectiveCost
                        blightCreatures = projected.getBattlefieldControlledBy(playerId)
                            .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
                        if (blightCreatures.isEmpty()) continue
                    }
                    is AbilityCost.Discard -> {
                        val targets = context.costUtils.findDiscardTargets(state, playerId, effectiveCost.filter)
                        if (targets.size < effectiveCost.count) continue
                        // Random discard is paid automatically at cost time — no player selection.
                        if (!effectiveCost.atRandom) {
                            discardCost = effectiveCost
                            discardTargets = targets
                        }
                    }
                    is AbilityCost.RemoveCounterFromSelf -> {
                        val counters = container.get<CountersComponent>()
                        val counterType = resolveCounterType(effectiveCost.counterType)
                        if ((counters?.getCount(counterType) ?: 0) < effectiveCost.count) continue
                    }
                    is AbilityCost.Composite -> {
                        val compositeCost = effectiveCost
                        var costCanBePaid = true
                        // If composite cost includes Tap, exclude the source from mana solving
                        val hasTapCost = compositeCost.costs.any { it is AbilityCost.Tap }
                        val excludeFromMana = if (hasTapCost) setOf(entityId) else emptySet()
                        for (subCost in compositeCost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> {
                                    if (container.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    if (!cardComponent.typeLine.isLand && projected.isCreature(entityId)) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!context.manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana, precomputedSources = context.availableManaSources, spellContext = abilityContext)) {
                                        // If the ability has convoke, check with convoke creatures
                                        if (ability.hasConvoke) {
                                            val creatures = context.costUtils.findConvokeCreatures(state, playerId)
                                            if (!context.costUtils.canAffordWithConvoke(state, playerId, subCost.cost, creatures, precomputedSources = context.availableManaSources)) {
                                                costCanBePaid = false
                                                break
                                            }
                                        } else {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                        state, playerId, subCost.filter, if (subCost.excludeSelf) entityId else null
                                    )
                                    if (sacrificeTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                                    if (chosenType == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                                    if (sacrificeTargets.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    // Source must be on battlefield (always true when iterating battlefield)
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapAttachedCreature -> {
                                    val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
                                    if (attachedId == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val attachedEntity = state.getEntity(attachedId)
                                    if (attachedEntity == null || attachedEntity.has<TappedComponent>()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    if (projected.isCreature(attachedId)) {
                                        val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                                        val hasHaste = projected.hasKeyword(attachedId, Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, subCost.filter)
                                        .let { targets -> if (subCost.excludeSelf) targets.filter { it != entityId } else targets }
                                    if (tapTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    bounceCost = subCost
                                    bounceTargets = context.costUtils.findAbilityBounceTargets(state, playerId, subCost.filter)
                                    if (bounceTargets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ExileFromGraveyard -> {
                                    val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                                    val graveyardCards = state.getZone(graveyardZone)
                                    if (graveyardCards.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Forage -> {
                                    // Forage: can exile 3 from graveyard OR sacrifice a Food
                                    val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                                    val graveyardCards = state.getZone(graveyardZone)
                                    val projected = state.projectedState
                                    val foods = state.getBattlefield().filter { permId ->
                                        state.getEntity(permId) ?: return@filter false
                                        projected.getController(permId) == playerId &&
                                            projected.hasSubtype(permId, com.wingedsheep.sdk.core.Subtype.FOOD.value)
                                    }
                                    if (graveyardCards.size < 3 && foods.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                    hasForageCost = true
                                    forageGraveyardCards = graveyardCards
                                    forageFoodTargets = foods
                                }
                                is AbilityCost.Blight -> {
                                    blightCost = subCost
                                    blightCreatures = projected.getBattlefieldControlledBy(playerId)
                                        .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
                                    if (blightCreatures.isEmpty()) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Discard -> {
                                    val targets = context.costUtils.findDiscardTargets(state, playerId, subCost.filter)
                                    if (targets.size < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                    // Random discard is paid automatically at cost time — no player selection.
                                    if (!subCost.atRandom) {
                                        discardCost = subCost
                                        discardTargets = targets
                                    }
                                }
                                is AbilityCost.ExileXFromGraveyard -> {
                                    // ExileXFromGraveyard: validated via maxAffordableX cap below
                                }
                                is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                                    // RemoveXPlusOnePlusOneCounters: validated via maxAffordableX cap below
                                }
                                is AbilityCost.RemovePlusOnePlusOneCounters -> {
                                    val available = context.costUtils.buildCounterRemovalPermanents(
                                        state, playerId, subCost.filter
                                    ).sumOf { it.availableCounters }
                                    if (available < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.TapXPermanents -> {
                                    // TapXPermanents: validated via maxAffordableX cap below
                                    // Also provide tap targets for the UI
                                    tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, subCost.filter)
                                }
                                is AbilityCost.RemoveCounterFromSelf -> {
                                    val counters = container.get<CountersComponent>()
                                    val counterType = resolveCounterType(subCost.counterType)
                                    if ((counters?.getCount(counterType) ?: 0) < subCost.count) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.Craft -> {
                                    // Combined BF+GY material pool (CR 702.167a-b). Records the cost
                                    // and full candidate list so the UI can render BF + GY side-by-side.
                                    val battlefieldMaterials = projected.getBattlefieldControlledBy(playerId)
                                        .filter { it != entityId }
                                        .filter { context.predicateEvaluator.matches(state, projected, it, subCost.filter, com.wingedsheep.engine.handlers.PredicateContext(controllerId = playerId)) }
                                    val graveyardMaterials = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
                                        .filter { context.predicateEvaluator.matches(state, state.projectedState, it, subCost.filter, com.wingedsheep.engine.handlers.PredicateContext(controllerId = playerId)) }
                                    if (battlefieldMaterials.size + graveyardMaterials.size < subCost.minCount) {
                                        costCanBePaid = false
                                        break
                                    }
                                    craftCost = subCost
                                    craftMaterials = battlefieldMaterials + graveyardMaterials
                                }
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) costAffordable = false
                    }
                    else -> {}
                }

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!context.castPermissionUtils.checkActivationRestriction(state, playerId, restriction, entityId, ability.id)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Compute convoke creature data for abilities with hasConvoke
                val abilityConvokeCreatures = if (ability.hasConvoke) {
                    context.costUtils.findConvokeCreatures(state, playerId)
                } else null

                // If cost is unaffordable, add as greyed-out option and skip expensive computations
                if (!costAffordable) {
                    val abilityManaCostString = when (effectiveCost) {
                        is AbilityCost.Mana -> effectiveCost.cost.toString()
                        is AbilityCost.Composite -> effectiveCost.costs
                            .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost?.toString()
                        else -> null
                    }
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = displayDescription,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        affordable = false,
                        manaCostString = abilityManaCostString
                    ))
                    continue
                }

                // Check for X-variable costs early (needed for counter removal info and cost info)
                val hasRemoveXCountersCostEarly = when (ability.cost) {
                    is AbilityCost.RemoveXPlusOnePlusOneCounters -> true
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .any { it is AbilityCost.RemoveXPlusOnePlusOneCounters }
                    else -> false
                }

                val hasTapXPermanentsCost = when (ability.cost) {
                    is AbilityCost.TapXPermanents -> true
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .any { it is AbilityCost.TapXPermanents }
                    else -> false
                }

                // Build counter removal creature info if ability has RemoveXPlusOnePlusOneCounters
                // (creature-only X-variable) OR RemovePlusOnePlusOneCounters (filtered fixed-count).
                val fixedRemoveCost: AbilityCost.RemovePlusOnePlusOneCounters? = when (ability.cost) {
                    is AbilityCost.RemovePlusOnePlusOneCounters -> ability.cost as AbilityCost.RemovePlusOnePlusOneCounters
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.RemovePlusOnePlusOneCounters>().firstOrNull()
                    else -> null
                }
                val counterRemovalCreatures = when {
                    hasRemoveXCountersCostEarly -> context.costUtils.buildCounterRemovalCreatures(state, playerId)
                    fixedRemoveCost != null -> context.costUtils.buildCounterRemovalPermanents(
                        state, playerId, fixedRemoveCost.filter
                    )
                    else -> emptyList()
                }

                // Build additional cost info for sacrifice, tap, bounce, or counter removal costs
                val costInfo = buildAdditionalCostInfo(
                    ability, tapTargets, tapCost, hasTapXPermanentsCost,
                    sacrificeTargets, sacrificeCost, bounceTargets, bounceCost,
                    counterRemovalCreatures,
                    hasForageCost, forageGraveyardCards, forageFoodTargets,
                    blightCost, blightCreatures,
                    discardCost, discardTargets,
                    craftCost, craftMaterials
                )

                // Calculate X cost info for activated abilities with X in their mana cost
                // or X determined by a variable cost (e.g., RemoveXPlusOnePlusOneCounters).
                // Use [effectiveCost] so generic-cost reductions (e.g., The Dominion Bracelet,
                // Starport Security) flow through to the displayed [manaCostString].
                val abilityManaCost = when (effectiveCost) {
                    is AbilityCost.Mana -> effectiveCost.cost
                    is AbilityCost.Composite -> effectiveCost.costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val abilityManaCostString = abilityManaCost?.toString()
                val abilityHasXInManaCost = abilityManaCost?.hasX == true

                // Reuse the early checks for X-variable costs
                val hasRemoveXCountersCost = hasRemoveXCountersCostEarly
                val abilityHasXCost = abilityHasXInManaCost || hasRemoveXCountersCost || hasTapXPermanentsCost

                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    context.costUtils.calculateMaxAffordableX(state, playerId, ability.cost, abilityManaCost, precomputedSources = context.availableManaSources)
                } else null

                // Compute auto-tap preview for UI highlighting (skipped in ACTIONS_ONLY mode).
                // The solver runs against the full ability cost; the client trims this set
                // down once convoke is applied, and the engine re-solves at payment time
                // with the non-chosen sources excluded (see ActivateAbilityHandler.execute).
                val abilityAutoTapPreview = if (context.skipAutoTapPreview || abilityManaCost == null || abilityHasXCost) null
                else context.manaSolver.solve(state, playerId, abilityManaCost, precomputedSources = context.availableManaSources)?.sources?.map { it.entityId }

                // Compute maxRepeatableActivations for eligible self-targeting abilities.
                // Eligible: pure mana cost, no X, no once-per-turn restriction, not a class level-up,
                // and the effect must "stack" when activated multiple times (e.g., +1/+0 modifiers).
                // Effects that REPLACE base characteristics (BecomeCreature, SetBasePowerToughness, etc.)
                // are excluded — repeating them only re-applies the same end state, so the prompt is meaningless.
                val isRepeatEligible = ability.cost is AbilityCost.Mana
                    && !abilityHasXCost
                    && ability.effect !is LevelUpClassEffect
                    && effectStacksOnRepeat(ability.effect)
                    && !ability.restrictions.any {
                    it is ActivationRestriction.OncePerTurn || it is ActivationRestriction.Once ||
                        it is ActivationRestriction.MaxPerTurn ||
                        (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.OncePerTurn || r is ActivationRestriction.Once || r is ActivationRestriction.MaxPerTurn })
                }
                val maxRepeatableActivations: Int? = if (isRepeatEligible && abilityManaCost != null && abilityManaCost.cmc > 0) {
                    // Upper bound assuming every available mana could pay for a colored symbol;
                    // color requirements only ever reduce this, so it's a safe search ceiling.
                    val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources)
                    val upperBound = availableSources / abilityManaCost.cmc
                    if (upperBound > 1) {
                        // Color-aware: dividing total mana by CMC over-counts (e.g. 3 red + 3 black
                        // can pay {R} only 3 times, not 6). canPay() honors color requirements, and
                        // affordability is monotonic (payable N times ⇒ payable N-1), so binary-search
                        // the largest N whose N-times-repeated cost is actually payable.
                        var lo = 1
                        var hi = upperBound
                        while (lo < hi) {
                            val mid = (lo + hi + 1) / 2
                            val affordable = context.manaSolver.canPay(
                                state, playerId, abilityManaCost * mid,
                                precomputedSources = context.availableManaSources
                            )
                            if (affordable) lo = mid else hi = mid - 1
                        }
                        if (lo > 1) lo else null
                    } else null
                } else null

                // Check for target requirements (apply text-changing effects to filter)
                val targetReqs = if (textReplacement != null) {
                    ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
                } else {
                    ability.targetRequirements
                }
                if (targetReqs.isNotEmpty()) {
                    // Build target info for each requirement (same pattern as spells)
                    val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, sourceId = entityId)

                    // All requirements must be satisfiable
                    if (!context.targetUtils.allRequirementsSatisfied(targetReqInfos)) continue

                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    // Check if we can auto-select player targets (single target requirement, single valid choice)
                    if (targetReqs.size == 1 && context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        result.add(LegalAction(
                            actionType = "ActivateAbility",
                            description = displayDescription,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = abilityManaCostString,
                            hasConvoke = ability.hasConvoke,
                            convokeCreatures = abilityConvokeCreatures
                        ))
                    } else if (targetReqs.size == 1 && firstReqInfo.validTargets.size == 1 && firstReqInfo.validTargets.first() == entityId) {
                        // Self-targeting: only valid target is the source itself — auto-select and offer repeat
                        val autoSelectedTarget = ChosenTarget.Permanent(entityId)
                        result.add(LegalAction(
                            actionType = "ActivateAbility",
                            description = displayDescription,
                            action = ActivateAbility(playerId, entityId, ability.id, targets = listOf(autoSelectedTarget)),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            maxRepeatableActivations = maxRepeatableActivations,
                            manaCostString = abilityManaCostString,
                            hasConvoke = ability.hasConvoke,
                            convokeCreatures = abilityConvokeCreatures
                        ))
                    } else {
                        // Only hold priority when the top of the stack is something this
                        // ability could actually target — otherwise an idle holdPriority
                        // flag would stop auto-pass even when the ability has no relevant
                        // work to do (e.g. the trigger we wanted to copy isn't on top yet).
                        val holdPriorityForTopOfStack = ability.holdPriority &&
                            state.stack.lastOrNull()?.let { it in firstReqInfo.validTargets } == true
                        result.add(LegalAction(
                            actionType = "ActivateAbility",
                            description = displayDescription,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = abilityManaCostString,
                            hasConvoke = ability.hasConvoke,
                            convokeCreatures = abilityConvokeCreatures,
                            holdPriority = holdPriorityForTopOfStack
                        ))
                    }
                } else {
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = displayDescription,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        additionalCostInfo = costInfo,
                        hasXCost = abilityHasXCost,
                        maxAffordableX = abilityMaxAffordableX,
                        autoTapPreview = abilityAutoTapPreview,
                        maxRepeatableActivations = maxRepeatableActivations,
                        manaCostString = abilityManaCostString,
                        hasConvoke = ability.hasConvoke,
                        convokeCreatures = abilityConvokeCreatures
                    ))
                }
            }
        }
    }

    /**
     * Check for "any player may activate" abilities on opponent's permanents (e.g., Lethal Vapors).
     */
    private fun enumerateAnyPlayerMayAbilities(context: EnumerationContext, result: MutableList<LegalAction>) {
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected

        val opponentId = state.turnOrder.firstOrNull { it != playerId } ?: return
        val opponentPermanents = projected.getBattlefieldControlledBy(opponentId)

        for (entityId in opponentPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            if (projected.hasLostAllAbilities(entityId)) continue
            if (context.castPermissionUtils.isActivationPrevented(state, entityId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val anyPlayerAbilities = cardDef.script.activatedAbilities.filter { ability ->
                !ability.isManaAbility && ability.activateFromZone == Zone.BATTLEFIELD && ability.restrictions.any { it is ActivationRestriction.AnyPlayerMay }
            }
            if (anyPlayerAbilities.isEmpty()) continue

            val textReplacement = container.get<TextReplacementComponent>()

            for (ability in anyPlayerAbilities) {
                val effectiveCost = if (textReplacement != null) {
                    ability.cost.applyTextReplacement(textReplacement)
                } else {
                    ability.cost
                }

                // Check cost payability (Free cost always passes)
                val anyPlayerAbilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(cardComponent, projected, entityId)
                val anyPlayerManaCostString = when (effectiveCost) {
                    is AbilityCost.Free -> null
                    is AbilityCost.Mana -> {
                        if (!context.manaSolver.canPay(state, playerId, effectiveCost.cost, precomputedSources = context.availableManaSources, spellContext = anyPlayerAbilityContext)) continue
                        effectiveCost.cost.toString()
                    }
                    else -> continue // Other costs on opponent's permanents not yet supported
                }

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!context.castPermissionUtils.checkActivationRestriction(state, playerId, restriction, entityId, ability.id)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Check target requirements
                val targetReqs = if (textReplacement != null) {
                    ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
                } else {
                    ability.targetRequirements
                }
                if (targetReqs.isNotEmpty()) {
                    val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, sourceId = entityId)

                    if (!context.targetUtils.allRequirementsSatisfied(targetReqInfos)) continue

                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        validTargets = firstReqInfo.validTargets,
                        requiresTargets = true,
                        targetCount = firstReq.count,
                        minTargets = firstReq.effectiveMinCount,
                        targetDescription = firstReq.description,
                        targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                        manaCostString = anyPlayerManaCostString
                    ))
                } else {
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        manaCostString = anyPlayerManaCostString
                    ))
                }
            }
        }
    }

    /**
     * Generate sorcery-speed level-up activated abilities for Class enchantments.
     * Only generates the ability for the next level (current + 1) if it exists.
     */
    private fun generateClassLevelUpAbilities(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        classLevelComponent: ClassLevelComponent
    ): List<ActivatedAbility> {
        val nextLevel = classLevelComponent.currentLevel + 1
        val nextLevelAbility = cardDef.classLevels.find { it.level == nextLevel } ?: return emptyList()
        return listOf(
            ActivatedAbility(
                id = AbilityId.classLevelUp(nextLevel),
                cost = AbilityCost.Mana(nextLevelAbility.cost),
                effect = LevelUpClassEffect(nextLevel),
                timing = TimingRule.SorcerySpeed,
                descriptionOverride = "Level up to level $nextLevel"
            )
        )
    }

    /**
     * Build additional cost info for sacrifice, tap, bounce, sacrifice-self, or counter removal costs.
     */
    private fun buildAdditionalCostInfo(
        ability: ActivatedAbility,
        tapTargets: List<EntityId>?,
        tapCost: AbilityCost.TapPermanents?,
        hasTapXPermanentsCost: Boolean,
        sacrificeTargets: List<EntityId>?,
        sacrificeCost: AbilityCost.Sacrifice?,
        bounceTargets: List<EntityId>?,
        bounceCost: AbilityCost.ReturnToHand?,
        counterRemovalCreatures: List<CounterRemovalCreatureData>,
        hasForageCost: Boolean = false,
        forageGraveyardCards: List<EntityId> = emptyList(),
        forageFoodTargets: List<EntityId> = emptyList(),
        blightCost: AbilityCost.Blight? = null,
        blightCreatures: List<EntityId> = emptyList(),
        discardCost: AbilityCost.Discard? = null,
        discardTargets: List<EntityId>? = null,
        craftCost: AbilityCost.Craft? = null,
        craftMaterials: List<EntityId> = emptyList()
    ): AdditionalCostData? {
        if (craftCost != null) {
            // Craft (CR 702.167) is handled exclusively: when a Composite cost contains a
            // [AbilityCost.Craft] sub-cost, we surface only the Craft payload, dropping any
            // sibling AdditionalCost data (tap, sacrifice, discard, etc.). The DSL helper
            // `card { craft(...) }` always pairs Craft with `Mana` only — mana is handled
            // separately via [costPayment.manaPayment] — so this is exhaustive in practice.
            // Composing Craft with another AdditionalCost-bearing sub-cost (e.g.
            // `Composite(Tap, Craft(...))`) would need this branch generalized to merge the
            // two payloads; flag any such authoring upstream until that's added.
            return AdditionalCostData(
                description = craftCost.description,
                costType = "Craft",
                validCraftMaterials = craftMaterials,
                craftMinCount = craftCost.minCount,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (tapTargets != null && tapCost != null) {
            return AdditionalCostData(
                description = tapCost.description,
                costType = "TapPermanents",
                validTapTargets = tapTargets,
                tapCount = tapCost.count,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (tapTargets != null && hasTapXPermanentsCost) {
            // TapXPermanents: tap count is variable (chosen by player as X value)
            val tapXDesc = when (ability.cost) {
                is AbilityCost.TapXPermanents -> (ability.cost as AbilityCost.TapXPermanents).description
                is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                    .filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()?.description
                    ?: "Tap X permanents you control"
                else -> "Tap X permanents you control"
            }
            return AdditionalCostData(
                description = tapXDesc,
                costType = "TapPermanents",
                validTapTargets = tapTargets,
                tapCount = 0, // Variable — UI uses X value selector instead
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (sacrificeTargets != null && sacrificeCost != null) {
            return AdditionalCostData(
                description = sacrificeCost.description,
                costType = "SacrificePermanent",
                validSacrificeTargets = sacrificeTargets,
                sacrificeCount = sacrificeCost.count,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (blightCost != null && blightCreatures.isNotEmpty()) {
            return AdditionalCostData(
                description = "creature to blight",
                costType = "Blight",
                validBlightTargets = blightCreatures,
                blightAmount = blightCost.amount,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (discardCost != null && discardTargets != null && discardTargets.isNotEmpty()) {
            return AdditionalCostData(
                description = discardCost.description,
                costType = "DiscardCard",
                validDiscardTargets = discardTargets,
                discardCount = discardCost.count,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (bounceTargets != null && bounceCost != null) {
            return AdditionalCostData(
                description = bounceCost.description,
                costType = "BouncePermanent",
                validBounceTargets = bounceTargets,
                bounceCount = bounceCost.count,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (sacrificeTargets != null) {
            // SacrificeSelf cost — sacrifice target is the source itself
            return AdditionalCostData(
                description = "Sacrifice this permanent",
                costType = "SacrificeSelf",
                validSacrificeTargets = sacrificeTargets,
                sacrificeCount = 1,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (counterRemovalCreatures.isNotEmpty()) {
            return AdditionalCostData(
                description = "Remove +1/+1 counters from creatures you control",
                costType = "RemoveCounters",
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (hasForageCost) {
            // Prefer the exile path when 3+ cards are in the graveyard — lets the player
            // pick exactly which cards to exile. Otherwise fall back to sacrificing a Food.
            if (forageGraveyardCards.size >= 3) {
                return AdditionalCostData(
                    description = "Forage — exile 3 cards from your graveyard",
                    costType = "ExileFromGraveyard",
                    validExileTargets = forageGraveyardCards,
                    exileMinCount = 3,
                    exileMaxCount = 3
                )
            }
            if (forageFoodTargets.isNotEmpty()) {
                return AdditionalCostData(
                    description = "Forage — sacrifice a Food",
                    costType = "SacrificePermanent",
                    validSacrificeTargets = forageFoodTargets,
                    sacrificeCount = 1
                )
            }
        }
        return null
    }

    /**
     * True when activating this effect N times produces a different result than activating it once.
     *
     * Repeat-mode is only meaningful for additive abilities (e.g., +1/+0 pump, add a counter, draw a card).
     * Effects that REPLACE base characteristics — BecomeCreature, SetBasePowerToughness, SetCreatureSubtypes,
     * AnimateLand, BecomeCreatureType — produce the same end state regardless of how many times you activate
     * them, so offering "Activate How Many Times?" for those is just clutter.
     *
     * Regenerate is also excluded: a single shield is enough to survive a destruction, so stacking
     * redundant shields has no practical payoff and the prompt would only be clutter.
     *
     * Walks through CompositeEffect / ConditionalEffect / ModalEffect wrappers so an ability whose
     * "real" effect is hidden inside (e.g., Figure of Fable's `ConditionalEffect(... BecomeCreature)`) is
     * also excluded.
     */
    private fun effectStacksOnRepeat(effect: Effect): Boolean = when (effect) {
        is BecomeCreatureEffect,
        is BecomeCreatureTypeEffect,
        is SetBasePowerEffect,
        is SetBasePowerToughnessEffect,
        is SetCreatureSubtypesEffect,
        is AnimateLandEffect,
        is RegenerateEffect -> false
        is CompositeEffect -> effect.effects.all { effectStacksOnRepeat(it) }
        is ConditionalEffect -> effectStacksOnRepeat(effect.effect) &&
            (effect.elseEffect?.let { effectStacksOnRepeat(it) } ?: true)
        is ModalEffect -> effect.modes.all { effectStacksOnRepeat(it.effect) }
        else -> true
    }

    /**
     * Apply [ActivatedAbility.genericCostReduction] to the mana portion of [cost].
     * The reduction is evaluated against the activating entity (e.g., the equipped creature
     * for The Dominion Bracelet, where X = the creature's power).
     *
     * When the ability requires a target, the player hasn't chosen one yet at enumeration time,
     * so a reduction that reads the chosen target (e.g. Dragonfire Blade — "costs {1} less to
     * activate for each color of the creature it targets") can't resolve a specific target here.
     * We gate affordability on the *cheapest* reachable cost — the largest reduction over the
     * currently-legal targets — so the ability is offered (and its displayed cost shown) whenever
     * it's payable for at least one target. The handler re-derives the exact reduction from the
     * target the player actually chose (ActivateAbilityHandler.applyGenericCostReduction), and in
     * auto-tap mode pays that exact per-target cost. The reduction only ever lowers the cost, so a
     * best-case preview never causes the client to under-tap for the chosen target in auto-tap mode.
     */
    private fun applyAbilityGenericCostReduction(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: com.wingedsheep.engine.state.GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        enumerationContext: EnumerationContext
    ): AbilityCost {
        val reduction = ability.genericCostReduction ?: return cost
        val evaluator = com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
        val baseContext = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            opponentId = null
        )
        val amount = if (ability.targetRequirements.isNotEmpty()) {
            maxReductionOverLegalTargets(reduction, ability, state, sourceId, controllerId, enumerationContext, evaluator)
        } else {
            evaluator.evaluate(state, reduction, baseContext)
        }
        if (amount <= 0) return cost
        return reduceGenericInAbilityCost(cost, amount)
    }

    /**
     * Largest [reduction] achievable across the ability's currently-legal first-requirement
     * targets. Evaluates the reduction once per legal target (as if that target were chosen) and
     * keeps the maximum. For a reduction that doesn't read the target this collapses to a constant,
     * so it stays correct for non-target-dependent reductions on targeted abilities too. Returns 0
     * when there are no legal targets (the ability won't be offered anyway).
     */
    private fun maxReductionOverLegalTargets(
        reduction: com.wingedsheep.sdk.scripting.values.DynamicAmount,
        ability: ActivatedAbility,
        state: com.wingedsheep.engine.state.GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        enumerationContext: EnumerationContext,
        evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator
    ): Int {
        val validTargets = enumerationContext.targetUtils
            .buildTargetInfos(state, controllerId, ability.targetRequirements, sourceId = sourceId)
            .firstOrNull()?.validTargets ?: emptyList()
        if (validTargets.isEmpty()) return 0
        return validTargets.maxOf { targetId ->
            val targetContext = com.wingedsheep.engine.handlers.EffectContext(
                sourceId = sourceId,
                controllerId = controllerId,
                opponentId = null,
                targets = listOf(ChosenTarget.Permanent(targetId))
            )
            evaluator.evaluate(state, reduction, targetContext)
        }
    }

    private fun reduceGenericInAbilityCost(cost: AbilityCost, amount: Int): AbilityCost = when (cost) {
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
}
