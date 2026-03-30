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
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.engine.legalactions.ConvokeCreatureData

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

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

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
            else cardDef.script.effectiveActivatedAbilities(classLevel).filter { !it.isManaAbility }

            // Generate level-up abilities for Class enchantments
            val levelUpAbilities = if (cardDef != null && classLevelComponent != null && !projected.hasLostAllAbilities(entityId)) {
                generateClassLevelUpAbilities(cardDef, classLevelComponent)
            } else emptyList()

            val nonManaAbilities = ownNonManaAbilities + levelUpAbilities + allAbilities.filter { !it.isManaAbility }

            // Apply text-changing effects to ability costs and targets
            val textReplacement = container.get<TextReplacementComponent>()

            for (ability in nonManaAbilities) {
                // Planeswalker loyalty abilities: sorcery speed + once per turn + loyalty cost check
                if (ability.isPlaneswalkerAbility) {
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
                val effectiveCost = if (textReplacement != null) {
                    ability.cost.applyTextReplacement(textReplacement)
                } else {
                    ability.cost
                }

                // Check cost requirements and gather sacrifice/tap/bounce targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null
                var bounceTargets: List<EntityId>? = null
                var bounceCost: AbilityCost.ReturnToHand? = null
                var costAffordable = true

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (container.has<TappedComponent>()) continue
                        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
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
                        val attachedCard = attachedEntity.get<CardComponent>()
                        if (attachedCard != null && attachedCard.typeLine.isCreature) {
                            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                            val hasHaste = projected.hasKeyword(attachedId, Keyword.HASTE)
                            if (hasSummoningSickness && !hasHaste) continue
                        }
                    }
                    is AbilityCost.Mana -> {
                        if (!context.manaSolver.canPay(state, playerId, effectiveCost.cost)) {
                            // If the ability has convoke, check if affordable with convoke creatures
                            if (ability.hasConvoke) {
                                val creatures = context.costUtils.findConvokeCreatures(state, playerId)
                                if (!context.costUtils.canAffordWithConvoke(state, playerId, effectiveCost.cost, creatures)) {
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
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!context.manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana)) {
                                        // If the ability has convoke, check with convoke creatures
                                        if (ability.hasConvoke) {
                                            val creatures = context.costUtils.findConvokeCreatures(state, playerId)
                                            if (!context.costUtils.canAffordWithConvoke(state, playerId, subCost.cost, creatures)) {
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
                                    val attachedCard = attachedEntity.get<CardComponent>()
                                    if (attachedCard != null && attachedCard.typeLine.isCreature) {
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
                                    val graveyardSize = state.getZone(graveyardZone).size
                                    val hasFood = state.getBattlefield().any { permId ->
                                        val pc = state.getEntity(permId) ?: return@any false
                                        val pCard = pc.get<CardComponent>() ?: return@any false
                                        val pCtrl = pc.get<ControllerComponent>()?.playerId
                                        pCtrl == playerId && pCard.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype.FOOD)
                                    }
                                    if (graveyardSize < 3 && !hasFood) {
                                        costCanBePaid = false
                                        break
                                    }
                                }
                                is AbilityCost.ExileXFromGraveyard -> {
                                    // ExileXFromGraveyard: validated via maxAffordableX cap below
                                }
                                is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                                    // RemoveXPlusOnePlusOneCounters: validated via maxAffordableX cap below
                                }
                                is AbilityCost.TapXPermanents -> {
                                    // TapXPermanents: validated via maxAffordableX cap below
                                    // Also provide tap targets for the UI
                                    tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, subCost.filter)
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
                    val abilityManaCostString = when (ability.cost) {
                        is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost.toString()
                        is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                            .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost?.toString()
                        else -> null
                    }
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
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

                // Build counter removal creature info if ability has RemoveXPlusOnePlusOneCounters cost
                val counterRemovalCreatures = if (hasRemoveXCountersCostEarly) {
                    context.costUtils.buildCounterRemovalCreatures(state, playerId)
                } else emptyList()

                // Build additional cost info for sacrifice, tap, bounce, or counter removal costs
                val costInfo = buildAdditionalCostInfo(
                    ability, tapTargets, tapCost, hasTapXPermanentsCost,
                    sacrificeTargets, sacrificeCost, bounceTargets, bounceCost,
                    counterRemovalCreatures
                )

                // Calculate X cost info for activated abilities with X in their mana cost
                // or X determined by a variable cost (e.g., RemoveXPlusOnePlusOneCounters)
                val abilityManaCost = when (ability.cost) {
                    is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val abilityManaCostString = abilityManaCost?.toString()
                val abilityHasXInManaCost = abilityManaCost?.hasX == true

                // Reuse the early checks for X-variable costs
                val hasRemoveXCountersCost = hasRemoveXCountersCostEarly
                val abilityHasXCost = abilityHasXInManaCost || hasRemoveXCountersCost || hasTapXPermanentsCost

                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    context.costUtils.calculateMaxAffordableX(state, playerId, ability.cost, abilityManaCost)
                } else null

                // Compute auto-tap preview for UI highlighting
                val abilityAutoTapPreview = if (abilityManaCost != null && !abilityHasXCost) {
                    context.manaSolver.solve(state, playerId, abilityManaCost)?.sources?.map { it.entityId }
                } else null

                // Compute maxRepeatableActivations for eligible self-targeting abilities
                // Eligible: pure mana cost, no X, no once-per-turn restriction, not a class level-up
                val isRepeatEligible = ability.cost is AbilityCost.Mana
                    && !abilityHasXCost
                    && ability.effect !is LevelUpClassEffect
                    && !ability.restrictions.any {
                    it is ActivationRestriction.OncePerTurn || it is ActivationRestriction.Once ||
                        (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.OncePerTurn || r is ActivationRestriction.Once })
                }
                val maxRepeatableActivations: Int? = if (isRepeatEligible && abilityManaCost != null) {
                    val availableSources = context.manaSolver.getAvailableManaCount(state, playerId)
                    val costPerActivation = abilityManaCost.cmc
                    if (costPerActivation > 0) {
                        val maxRepeats = availableSources / costPerActivation
                        if (maxRepeats > 1) maxRepeats else null
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
                            description = ability.description,
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
                            description = ability.description,
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
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = abilityManaCostString,
                            hasConvoke = ability.hasConvoke,
                            convokeCreatures = abilityConvokeCreatures
                        ))
                    }
                } else {
                    result.add(LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
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

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val anyPlayerAbilities = cardDef.script.activatedAbilities.filter { ability ->
                !ability.isManaAbility && ability.restrictions.any { it is ActivationRestriction.AnyPlayerMay }
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
                val anyPlayerManaCostString = when (effectiveCost) {
                    is AbilityCost.Free -> null
                    is AbilityCost.Mana -> {
                        if (!context.manaSolver.canPay(state, playerId, effectiveCost.cost)) continue
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
        counterRemovalCreatures: List<CounterRemovalCreatureData>
    ): AdditionalCostData? {
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
        return null
    }
}
