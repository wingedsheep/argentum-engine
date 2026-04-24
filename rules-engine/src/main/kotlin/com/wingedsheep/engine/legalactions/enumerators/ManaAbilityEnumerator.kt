package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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

            val cardDef = context.cardRegistry.getCard(cardComponent.name)

            // Include granted activated abilities that are mana abilities (both temporary and static)
            val grantedManaAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
                .filter { it.isManaAbility }
            val staticManaAbilities = context.castPermissionUtils
                .getStaticGrantedActivatedAbilities(entityId, state)
                .filter { it.isManaAbility }

            // If no card definition (e.g., tokens) and no granted/static mana abilities, skip
            if (cardDef == null && grantedManaAbilities.isEmpty() && staticManaAbilities.isEmpty()) continue

            // If entity lost all abilities, only granted/static abilities remain (own abilities suppressed)
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            val ownManaAbilities = if (cardDef == null || entityLostAllAbilities) emptyList()
            else cardDef.script.effectiveActivatedAbilities(classLevel).filter { it.isManaAbility }
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
                var affordable = true

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (!context.costUtils.canPayTapCost(state, entityId)) affordable = false
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        if (!context.costUtils.canPayTapAttachedCreatureCost(state, entityId)) affordable = false
                    }
                    is AbilityCost.TapPermanents -> {
                        tapCost = effectiveCost
                        tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, tapCost.filter)
                        if (tapTargets.size < tapCost.count) affordable = false
                    }
                    is AbilityCost.Sacrifice -> {
                        sacrificeCost = effectiveCost
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                            state, playerId, sacrificeCost.filter,
                            if (sacrificeCost.excludeSelf) entityId else null
                        )
                        if (sacrificeTargets.size < sacrificeCost.count) affordable = false
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType == null) {
                            affordable = false
                        } else {
                            val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                            sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                            sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                            if (sacrificeTargets.isEmpty()) affordable = false
                        }
                    }
                    is AbilityCost.Composite -> {
                        val compositeCost = effectiveCost
                        // If composite cost includes Tap, exclude the source from mana solving
                        val hasTapCost = compositeCost.costs.any { it is AbilityCost.Tap }
                        val excludeFromMana = if (hasTapCost) setOf(entityId) else emptySet()
                        for (subCost in compositeCost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> {
                                    if (container.has<TappedComponent>()) {
                                        affordable = false; break
                                    }
                                    if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
                                        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                                        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
                                        if (hasSummoningSickness && !hasHaste) {
                                            affordable = false; break
                                        }
                                    }
                                }
                                is AbilityCost.Mana -> {
                                    if (!context.manaSolver.canPay(state, playerId, subCost.cost, excludeSources = excludeFromMana, precomputedSources = context.availableManaSources)) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.Sacrifice -> {
                                    sacrificeCost = subCost
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                        state, playerId, subCost.filter,
                                        if (subCost.excludeSelf) entityId else null
                                    )
                                    if (sacrificeTargets.size < subCost.count) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.get<ChosenCreatureTypeComponent>()?.creatureType
                                    if (chosenType == null) {
                                        affordable = false; break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = AbilityCost.Sacrifice(dynamicFilter)
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                                    if (sacrificeTargets.isEmpty()) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.SacrificeSelf -> {
                                    sacrificeTargets = listOf(entityId)
                                }
                                is AbilityCost.TapAttachedCreature -> {
                                    if (!context.costUtils.canPayTapAttachedCreatureCost(state, entityId)) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.TapPermanents -> {
                                    tapCost = subCost
                                    tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, subCost.filter)
                                    if (tapTargets.size < subCost.count) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    // Bounce costs not typical for mana abilities but handle for completeness
                                }
                                is AbilityCost.Forage -> {
                                    val graveyardSize = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).size
                                    val projected = state.projectedState
                                    val hasFood = state.getBattlefield().any { permId ->
                                        state.getEntity(permId) ?: return@any false
                                        projected.getController(permId) == playerId &&
                                            projected.hasSubtype(permId, Subtype.FOOD.value)
                                    }
                                    if (graveyardSize < 3 && !hasFood) {
                                        affordable = false; break
                                    }
                                }
                                else -> {}
                            }
                        }
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

                // Compute runtime description for abilities with dynamic mana amounts
                val description = runtimeDescription(ability, state, entityId, playerId, context)

                result.add(
                    LegalAction(
                        actionType = "ActivateAbility",
                        description = description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        affordable = affordable,
                        isManaAbility = true,
                        additionalCostInfo = costInfo,
                        requiresManaColorChoice = ability.effect is AddAnyColorManaEffect ||
                            ability.effect is AddAnyColorManaSpendOnChosenTypeEffect ||
                            ability.effect is AddManaOfColorAmongEffect ||
                            (ability.effect is CompositeEffect &&
                                (ability.effect as CompositeEffect).effects.any {
                                    it is AddAnyColorManaEffect || it is AddAnyColorManaSpendOnChosenTypeEffect
                                }),
                        manaCostString = manaAbilityManaCostString
                    )
                )
            }
        }

        return result
    }

    /**
     * Computes a runtime description for mana abilities that produce a dynamic amount of mana,
     * replacing the generic text with the actual creature count and chosen type.
     *
     * Also overrides the description when an aura attached to the source forces the land
     * to produce a different color (e.g., Shimmerwilds Growth → "{T}: Add {U}").
     */
    private fun runtimeDescription(
        ability: com.wingedsheep.sdk.scripting.ActivatedAbility,
        state: com.wingedsheep.engine.state.GameState,
        entityId: EntityId,
        playerId: EntityId,
        context: EnumerationContext
    ): String {
        val effect = ability.effect

        // Mana-color override from an attached aura (Shimmerwilds Growth etc.).
        if (effect is AddManaEffect) {
            val override = findEnchantedLandManaColorOverride(state, entityId, context)
            if (override != null) {
                val costDesc = ability.cost.description
                return "$costDesc: Add {${override.symbol}}"
            }
        }

        val amount = when (effect) {
            is AddAnyColorManaEffect -> effect.amount
            else -> null
        }

        // Detect AggregateBattlefield with HasChosenSubtype predicate (e.g., Three Tree City)
        val hasChosenSubtypeFilter = amount is DynamicAmount.AggregateBattlefield &&
            amount.filter.cardPredicates.any { it is CardPredicate.HasChosenSubtype }
        if (!hasChosenSubtypeFilter) {
            return ability.description
        }

        val chosenType = state.getEntity(entityId)
            ?.get<ChosenCreatureTypeComponent>()?.creatureType
        if (chosenType == null) {
            return ability.description
        }

        val projected = state.projectedState
        val count = state.getBattlefield().count { eid ->
            val controllerId = projected.getController(eid)
                ?: state.getEntity(eid)?.get<ControllerComponent>()?.playerId
            if (controllerId != playerId) return@count false
            if ("CREATURE" !in projected.getTypes(eid)) return@count false
            chosenType in projected.getSubtypes(eid)
        }

        val costDesc = ability.cost.description
        return "$costDesc: Add $count mana of any color ($count ${chosenType}s)"
    }

    /**
     * Resolves the mana-color override contributed by auras attached to the source
     * (via [OverrideEnchantedLandManaColor]). Returns `null` if none applies.
     * Kept in sync with the identical helpers in `ActivateAbilityHandler` and
     * `ManaSolver` — all three must agree or UI/label/solver/resolver drift apart.
     */
    private fun findEnchantedLandManaColorOverride(
        state: com.wingedsheep.engine.state.GameState,
        sourceId: EntityId,
        context: EnumerationContext
    ): Color? {
        var override: Color? = null
        for (id in state.getBattlefield()) {
            val container = state.getEntity(id) ?: continue
            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val o = staticAbility as? OverrideEnchantedLandManaColor ?: continue
                override = o.color
                    ?: container.get<com.wingedsheep.engine.state.components.identity.ChosenColorComponent>()?.color
                    ?: continue
            }
        }
        return override
    }
}
