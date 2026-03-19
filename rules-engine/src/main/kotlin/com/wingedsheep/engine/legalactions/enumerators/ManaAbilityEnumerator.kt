package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Enumerates mana abilities on battlefield permanents controlled by the player.
 *
 * Mana abilities are special actions that don't use the stack (CR 605).
 * This handles Tap, TapAttachedCreature, TapPermanents, Sacrifice,
 * SacrificeChosenCreatureType, and Composite mana ability costs.
 */
class ManaAbilityEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val entityLostAllAbilities = projected.hasLostAllAbilities(entityId)

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Include granted activated abilities that are mana abilities (both temporary and static)
            val grantedManaAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
                .filter { it.isManaAbility }
            val staticManaAbilities = context.castPermissionUtils
                .getStaticGrantedActivatedAbilities(entityId, state)
                .filter { it.isManaAbility }

            // If entity lost all abilities, only granted/static abilities remain (own abilities suppressed)
            val ownManaAbilities = if (entityLostAllAbilities) emptyList()
            else cardDef.script.activatedAbilities.filter { it.isManaAbility }
            val manaAbilities = ownManaAbilities + grantedManaAbilities + staticManaAbilities

            // Apply text-changing effects to mana ability costs
            val manaTextReplacement = container.get<TextReplacementComponent>()

            for (ability in manaAbilities) {
                // Apply text replacement to cost filters
                val effectiveCost = if (manaTextReplacement != null) {
                    ability.cost.applyTextReplacement(manaTextReplacement)
                } else {
                    ability.cost
                }

                // Check if the ability can be activated and gather cost info
                var tapTargets: List<EntityId>? = null
                var tapCost: AbilityCost.TapPermanents? = null
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: AbilityCost.Sacrifice? = null

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (!context.costUtils.canPayTapCost(state, entityId)) continue
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        if (!context.costUtils.canPayTapAttachedCreatureCost(state, entityId)) continue
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = effectiveCost
                        tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, tapCost.filter)
                        if (tapTargets.size < tapCost.count) continue
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                            state, playerId, sacrificeCost.filter,
                            if (sacrificeCost.excludeSelf) entityId else null
                        )
                        if (sacrificeTargets.size < sacrificeCost.count) continue
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                            ?: continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
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
                                        costCanBePaid = false; break
                                    }
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            costCanBePaid = false; break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!context.manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana)) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                        state, playerId, subCost.filter,
                                        if (subCost.excludeSelf) entityId else null
                                    )
                                    if (sacrificeTargets.size < subCost.count) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                                    if (chosenType == null) {
                                        costCanBePaid = false; break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                                    if (sacrificeTargets.isEmpty()) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapAttachedCreature -> {
                                    if (!context.costUtils.canPayTapAttachedCreatureCost(state, entityId)) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, subCost.filter)
                                    if (tapTargets.size < subCost.count) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    // Bounce costs not typical for mana abilities but handle for completeness
                                }
                                else -> {}
                            }
                        }
                        if (!costCanBePaid) continue
                    }
                    else -> {
                        // Other cost types — allow for now, engine will validate
                    }
                }

                val costInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostData(
                        description = tapCost.description,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count
                    )
                } else if (sacrificeTargets != null && sacrificeCost != null) {
                    AdditionalCostData(
                        description = sacrificeCost.description,
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = sacrificeCost.count
                    )
                } else null

                val manaAbilityManaCostString = when (effectiveCost) {
                    is AbilityCost.Mana -> effectiveCost.cost.toString()
                    is AbilityCost.Composite -> effectiveCost.costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost?.toString()
                    else -> null
                }

                result.add(
                    LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        isManaAbility = true,
                        additionalCostInfo = costInfo,
                        requiresManaColorChoice = ability.effect is AddAnyColorManaEffect ||
                            ability.effect is AddManaOfColorAmongEffect ||
                            (ability.effect is CompositeEffect &&
                                (ability.effect as CompositeEffect).effects.any { it is AddAnyColorManaEffect }),
                        manaCostString = manaAbilityManaCostString
                    )
                )
            }
        }

        return result
    }
}
