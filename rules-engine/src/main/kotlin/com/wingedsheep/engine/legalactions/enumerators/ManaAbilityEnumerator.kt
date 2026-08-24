package com.wingedsheep.engine.legalactions.enumerators
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.SummoningSicknessRules
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.mechanics.mana.LandManaColorInspector
import com.wingedsheep.engine.mechanics.mana.ManaColorSetResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

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

            // A face-down permanent has no characteristics beyond those the rules that made it
            // face down list (CR 708.2), so none of its *card's* mana abilities are offered — but
            // a mana ability another effect grants it applies in Layer 6 to the object on the
            // battlefield, not to the hidden card, and stays activatable. Folded into
            // `ownManaAbilities` below rather than skipping the permanent outright, so this
            // enumerator and `ActivatedAbilityEnumerator` answer the same rule the same way.
            val isFaceDown = container.has<FaceDownComponent>()

            // PreventActivatedAbilities (Cursed Totem etc.) blocks mana abilities too —
            // per ruling, "Cursed Totem stops players from activating mana abilities of
            // creatures." A `nonManaAbilitiesOnly` lock (Sharkey) exempts mana abilities,
            // so pass abilityIsManaAbility = true here.
            if (context.castPermissionUtils.isActivationPrevented(state, entityId, abilityIsManaAbility = true)) continue

            // PlayersCantActivateAbilities (Grand Abolisher) likewise blocks mana abilities of
            // matching permanents for the affected player — "can't activate abilities of …".
            if (context.castPermissionUtils.isActivationPreventedForPlayer(state, entityId, playerId)) continue

            val entityLostAllAbilities = projected.hasLostAllAbilities(entityId)

            // By definition id, not name: a renamed copy (CR 707.9 — Absorbing Man copying a mana
            // rock "except his name is Absorbing Man") keeps its printed name but presents the
            // copied definition. `ActivateAbilityHandler` resolves by id, so a name lookup here
            // would hide a mana ability the engine would still let the player activate.
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId)

            // Include granted activated abilities that are mana abilities (both temporary and static)
            val grantedManaAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
                .filter { it.isManaAbility }
            val staticManaAbilities = context.castPermissionUtils
                .getStaticGrantedActivatedAbilities(entityId, state)
                .filter { it.isManaAbility }

            // Intrinsic mana abilities from projected basic-land subtypes (CR 305.7).
            // When present, they replace the card definition's own mana abilities so
            // type-changing effects (Sea's Claim, Spreading Seas) and shock lands
            // — whose printed mana ability is only ever the basic-land-derived one —
            // produce the correct colors via the projected type line.
            val intrinsicManaAbilities = IntrinsicManaAbilities.forEntity(state, projected, entityId)

            // If no card definition (e.g., tokens) and no granted/static/intrinsic mana abilities, skip
            if (cardDef == null && grantedManaAbilities.isEmpty() && staticManaAbilities.isEmpty() && intrinsicManaAbilities.isEmpty()) continue
            if (isFaceDown && grantedManaAbilities.isEmpty() && staticManaAbilities.isEmpty()) continue

            // If entity lost all abilities, only granted/static abilities remain (own abilities
            // suppressed) — this includes intrinsic basic-land-subtype abilities: a Plains hit by
            // Imprisoned in the Moon keeps its "Plains" subtype (CR 205.4b types/subtypes are
            // untouched by ability removal) but per ruling "loses any intrinsic mana abilities
            // associated with them", so `entityLostAllAbilities` must be checked before falling
            // back to the intrinsic-subtype inference.
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            val ownManaAbilities = when {
                isFaceDown -> emptyList()
                // Blood Moon / Zhao ("nonbasic lands are Mountains"): an effect that SET this
                // land's basic types also grants the type's intrinsic mana ability (CR 305.7),
                // which survives the same effect's ability removal — so the land still taps for
                // its new color. Intrinsic mana from a printed/unchanged subtype does NOT survive
                // (Imprisoned in the Moon), which the plain `entityLostAllAbilities` branch keeps.
                entityLostAllAbilities && projected.hasBasicLandTypesSetByEffect(entityId) -> intrinsicManaAbilities
                entityLostAllAbilities -> emptyList()
                intrinsicManaAbilities.isNotEmpty() -> intrinsicManaAbilities
                cardDef == null -> emptyList()
                else -> cardDef.script.effectiveActivatedAbilities(classLevel).filter { it.isManaAbility }
            }
            val manaAbilities = ownManaAbilities + grantedManaAbilities + staticManaAbilities

            // Apply text-changing effects to mana ability costs
            val manaTextReplacement = container.get<TextReplacementComponent>()

            // `ability = null` is safe for every ability below: a mana ability is never an equip
            // ability (equip attaches an Equipment, CR 702.6a — it adds no mana, CR 605.1a), so the
            // only ability-specific fact in the context reads false for all of them anyway.
            val manaAbilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(
                cardComponent, projected, entityId, ability = null
            )

            for (ability in manaAbilities) {
                // Kang the Conqueror's turn-scoped power-up lockout says "power-up abilities can't
                // be activated", with no carve-out for mana abilities. No printed power-up ability
                // is a mana ability today; the guard is here so a future one can't be offered as a
                // mana source that `ActivateAbilityHandler.validate` would then reject.
                if (context.castPermissionUtils.isPowerUpActivationRestricted(state, ability)) continue

                // Apply text replacement to cost filters
                val effectiveCost = if (manaTextReplacement != null) {
                    ability.cost.applyTextReplacement(manaTextReplacement)
                } else {
                    ability.cost
                }

                // Check if the ability can be activated and gather cost info
                var tapTargets: List<EntityId>? = null
                var tapCost: CostAtom.TapPermanents? = null
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: CostAtom.Sacrifice? = null
                var affordable = true

                when (effectiveCost) {
                    is AbilityCost.Tap -> {
                        if (!context.costUtils.canPayTapCost(state, entityId)) affordable = false
                    }
                    is AbilityCost.TapAttachedCreature -> {
                        if (!context.costUtils.canPayTapAttachedCreatureCost(state, entityId)) affordable = false
                    }
                    is AbilityCost.Atom -> when (val atom = effectiveCost.atom) {
                        is CostAtom.TapPermanents -> {
                            tapCost = atom
                            tapTargets = context.costUtils.findAbilityTapTargets(
                                state, playerId, atom.filter,
                                if (atom.excludeSelf) entityId else null
                            )
                            if (tapTargets.size < atom.count) affordable = false
                        }
                        is CostAtom.Sacrifice -> {
                            sacrificeCost = atom
                            sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                state, playerId, atom.filter,
                                if (atom.excludeSelf) entityId else null, sourceId = entityId
                            )
                            if (sacrificeTargets.size < atom.count) affordable = false
                        }
                        // CR 701.17b — a mill cost is unpayable when the library holds fewer cards,
                        // so the mana ability isn't legal at all (Deranged Assistant on an empty
                        // library).
                        is CostAtom.Mill -> {
                            if (state.getZone(ZoneKey(playerId, Zone.LIBRARY)).size < atom.count) affordable = false
                        }
                        // CR 118.3 — same shallow-library gate for exiling the top N.
                        is CostAtom.ExileTopOfLibrary -> {
                            if (state.getZone(ZoneKey(playerId, Zone.LIBRARY)).size < atom.count) affordable = false
                        }
                        // Other atoms (mana, life, discard, …) — engine validates at payment.
                        else -> {}
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.chosenCreatureType()
                        if (chosenType == null) {
                            affordable = false
                        } else {
                            val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                            sacrificeCost = CostAtom.Sacrifice(dynamicFilter)
                            sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter, sourceId = entityId)
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
                                    if (projected.isCreature(entityId) &&
                                        SummoningSicknessRules.blocksTapOrUntapCost(entityId, container, projected)
                                    ) {
                                        affordable = false; break
                                    }
                                }
                                is AbilityCost.Atom -> when (val atom = subCost.atom) {
                                    is CostAtom.Mana -> {
                                        if (!context.manaSolver.canPay(state, playerId, atom.cost, excludeSources = excludeFromMana, precomputedSources = context.availableManaSources, spellContext = manaAbilityContext)) {
                                            affordable = false; break
                                        }
                                    }
                                    is CostAtom.Sacrifice -> {
                                        sacrificeCost = atom
                                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                            state, playerId, atom.filter,
                                            if (atom.excludeSelf) entityId else null, sourceId = entityId
                                        )
                                        if (sacrificeTargets.size < atom.count) {
                                            affordable = false; break
                                        }
                                    }
                                    is CostAtom.TapPermanents -> {
                                        tapCost = atom
                                        tapTargets = context.costUtils.findAbilityTapTargets(
                                            state, playerId, atom.filter,
                                            if (atom.excludeSelf) entityId else null
                                        )
                                        if (tapTargets.size < atom.count) {
                                            affordable = false; break
                                        }
                                    }
                                    is CostAtom.ReturnToHand -> {
                                        // Bounce costs not typical for mana abilities but handle for completeness
                                    }
                                    // CR 701.17b — see the single-atom branch above.
                                    is CostAtom.Mill -> {
                                        if (state.getZone(ZoneKey(playerId, Zone.LIBRARY)).size < atom.count) {
                                            affordable = false; break
                                        }
                                    }
                                    // Other atoms (life, discard, exile, reveal) — engine validates at payment.
                                    else -> {}
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.chosenCreatureType()
                                    if (chosenType == null) {
                                        affordable = false; break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = CostAtom.Sacrifice(dynamicFilter)
                                    sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter, sourceId = entityId)
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

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!context.castPermissionUtils.checkActivationRestriction(state, playerId, restriction, entityId, ability)) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                val costInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostData(
                        description = tapCost.description.replaceFirstChar { it.uppercase() },
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count
                    )
                } else if (sacrificeTargets != null && sacrificeCost != null) {
                    AdditionalCostData(
                        description = sacrificeCost.description.replaceFirstChar { it.uppercase() },
                        costType = "SacrificePermanent",
                        validSacrificeTargets = sacrificeTargets,
                        sacrificeCount = sacrificeCost.count
                    )
                } else null

                val manaAbilityManaCostString = when (effectiveCost) {
                    is AbilityCost.Atom -> effectiveCost.manaCostOrNull?.toString()
                    is AbilityCost.Composite -> effectiveCost.costs
                        .firstNotNullOfOrNull { it.manaCostOrNull }?.toString()
                    else -> null
                }

                // Compute runtime description for abilities with dynamic mana amounts
                val description = runtimeDescription(ability, state, entityId, playerId, context)

                val availableManaColors = constrainedColors(ability.effect, state, entityId, playerId, context)

                // A mana ability can still make the player choose an X that isn't paid in mana —
                // the storage lands' "{T}, Remove any number of storage counters: Add {B} for each"
                // (Bottomless Vault and its cycle). Without these fields the client has no X picker
                // to open, activates with X unset, and the cost dutifully removes zero counters for
                // zero mana. Same helper the non-mana enumerator uses, so the two can't drift.
                val hasNonManaX = context.costUtils.hasPlayerChosenNonManaX(effectiveCost)
                val manaAbilityMaxX: Int? = if (hasNonManaX) {
                    context.costUtils.calculateMaxAffordableX(
                        state, playerId, effectiveCost, effectiveCost.manaCostOrNull,
                        precomputedSources = context.availableManaSources, sourceId = entityId
                    )
                } else null

                result.add(
                    LegalAction(
                        actionType = "ActivateAbility",
                        description = description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        affordable = affordable,
                        isManaAbility = true,
                        hasXCost = hasNonManaX,
                        maxAffordableX = manaAbilityMaxX,
                        minX = if (hasNonManaX) ability.minimumXValue else 0,
                        additionalCostInfo = costInfo,
                        requiresManaColorChoice = ability.effect is AddManaOfChoiceEffect ||
                            ability.effect is AddAnyColorManaSpendOnChosenTypeEffect ||
                            (ability.effect is CompositeEffect &&
                                (ability.effect as CompositeEffect).effects.any {
                                    it is AddManaOfChoiceEffect || it is AddAnyColorManaSpendOnChosenTypeEffect
                                }),
                        availableManaColors = availableManaColors,
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
    /**
     * The combined [com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap] factor for [entityId]
     * (Virtue of Strength: 3), for labelling only — the authoritative scaling happens in
     * `ActivateAbilityHandler` (manual tap) and `ManaSolver` (auto-tap). Instances multiply
     * together, matching both of those.
     */
    private fun sourceTapManaMultiplier(
        state: com.wingedsheep.engine.state.GameState,
        entityId: EntityId,
        context: EnumerationContext
    ): Int {
        if (context.manaStatics.sourceTapMultipliers.isEmpty()) return 1
        var multiplier = 1
        for (entry in context.manaStatics.sourceTapMultipliers) {
            if (entry.static.multiplier <= 1) continue
            val filterContext = PredicateContext(
                controllerId = entry.sourceControllerId,
                sourceId = entry.sourceId
            )
            if (context.predicateEvaluator.matches(
                    state, state.projectedState, entityId, entry.static.sourceFilter, filterContext
                )
            ) {
                multiplier *= entry.static.multiplier
            }
        }
        return multiplier
    }

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
            val override = context.manaStatics.landColorOverrideByTarget[entityId]
            if (override != null) {
                val costDesc = ability.cost.description
                return "$costDesc: Add {${override.symbol}}"
            }
        }

        // Multiplied output (Virtue of Strength) — label what the tap will actually produce, so
        // the button reads "{T}: Add {G}{G}{G}" rather than the printed "{T}: Add {G}".
        val manaMultiplier = sourceTapManaMultiplier(state, entityId, context)
        if (manaMultiplier > 1) {
            val symbol = when {
                effect is AddManaEffect && effect.amount is DynamicAmount.Fixed -> "{${effect.color.symbol}}"
                effect is AddColorlessManaEffect && effect.amount is DynamicAmount.Fixed -> "{C}"
                else -> null
            }
            if (symbol != null) {
                val baseAmount = when (effect) {
                    is AddManaEffect -> (effect.amount as DynamicAmount.Fixed).amount
                    is AddColorlessManaEffect -> (effect.amount as DynamicAmount.Fixed).amount
                    else -> 0
                }
                val total = baseAmount * manaMultiplier
                if (total > 0) return "${ability.cost.description}: Add ${symbol.repeat(total)}"
            }
        }

        val amount = when (effect) {
            is AddManaOfChoiceEffect -> effect.amount
            else -> null
        }

        // Detect AggregateBattlefield with HasChosenSubtype predicate (e.g., Three Tree City)
        val hasChosenSubtypeFilter = amount is DynamicAmount.AggregateBattlefield &&
            amount.filter.cardPredicates.any { it is CardPredicate.HasChosenSubtype }
        if (!hasChosenSubtypeFilter) {
            return ability.description
        }

        val chosenType = state.getEntity(entityId)
            ?.chosenCreatureType()
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
     * Resolves the constrained set of producible colors for an ability whose effect
     * narrows the player's color choice (Mox Amber, Fellwar Stone, Reflecting Pool,
     * Command Tower, Uncharted Haven, ...).
     *
     * Returns `null` for [ManaColorSet.AnyColor], signalling "no constraint — render
     * the full five-color picker." Returns an explicit list for everything else,
     * resolved through [ManaColorSetResolver] so this site stays in sync with the
     * executor and solver paths. An empty list means "no producible color right now"
     * (e.g., Mox Amber with no legendary creatures in play).
     */
    private fun constrainedColors(
        effect: Effect,
        state: GameState,
        sourceId: EntityId,
        playerId: EntityId,
        context: EnumerationContext,
    ): List<Color>? {
        val choice = pickChoiceEffect(effect) ?: return null
        if (choice.colorSet is ManaColorSet.AnyColor) return null
        return ManaColorSetResolver.resolve(
            colorSet = choice.colorSet,
            state = state,
            projected = context.projected,
            sourceId = sourceId,
            controllerId = playerId,
            cardRegistry = context.cardRegistry,
        ).toList()
    }

    private fun pickChoiceEffect(effect: Effect): AddManaOfChoiceEffect? = when (effect) {
        is AddManaOfChoiceEffect -> effect
        is CompositeEffect -> effect.effects.firstNotNullOfOrNull { it as? AddManaOfChoiceEffect }
        else -> null
    }
}
