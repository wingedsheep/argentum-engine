package com.wingedsheep.engine.legalactions.enumerators
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType

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
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType
import com.wingedsheep.sdk.dsl.craft

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
                // Sorcery-speed abilities: skip during non-main phases / opponent's turn.
                // Equip abilities are exempt while the controller has an active instant-speed-equip
                // permission (Forge Anew "During your turn …", Leonin Shikari) — CR 702.6e timing lifted.
                if (ability.timing == TimingRule.SorcerySpeed && !context.canPlaySorcerySpeed &&
                    !(ability.isEquipAbility && context.castPermissionUtils.canEquipAtInstantSpeed(state, playerId))
                ) continue

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
                // creature's power"). Then apply Forge Anew's free-first-equip discount, so the
                // displayed cost and affordability reflect the {0} the player will actually pay.
                val effectiveCost = context.castPermissionUtils.applyFreeFirstEquipDiscount(
                    applyAbilityGenericCostReduction(rawCost, ability, state, entityId, playerId, context),
                    ability, state, playerId
                )

                // Description shown to the player. When the effective cost differs from the printed
                // cost — a generic cost reduction (Starport Security "{3}{W}…" → "{1}{W}…"), or a
                // free-first-equip discount (Forge Anew dropping an equip to {0}) — rebuild the
                // prefix from [effectiveCost] so the menu reflects what the player will actually pay.
                // Cards with an explicit descriptionOverride keep it (we can't safely splice a cost
                // into custom text).
                val displayDescription =
                    if (effectiveCost != rawCost && ability.descriptionOverride == null) {
                        // A cost reduced all the way to zero renders as an empty string; show "{0}"
                        // so a free (e.g. Forge-Anew-discounted) equip reads as free, not blank.
                        "${effectiveCost.description.ifEmpty { "{0}" }}: ${ability.effect.description}"
                    } else {
                        ability.description
                    }

                // Ability payment context — lets the solver consider restricted mana that's
                // only spendable on this kind of activation (e.g., Steelswarm Operator's mana
                // restricted to abilities of artifact sources).
                val abilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(cardComponent, projected, entityId)

                // Check cost requirements and gather sacrifice/tap/bounce targets if needed
                var sacrificeTargets: List<EntityId>? = null
                var sacrificeCost: CostAtom.Sacrifice? = null
                var tapTargets: List<EntityId>? = null
                var tapCost: CostAtom.TapPermanents? = null
                var bounceTargets: List<EntityId>? = null
                var bounceCost: CostAtom.ReturnToHand? = null
                var hasForageCost = false
                var forageGraveyardCards: List<EntityId> = emptyList()
                var forageFoodTargets: List<EntityId> = emptyList()
                var blightCost: AbilityCost.Blight? = null
                var blightCreatures: List<EntityId> = emptyList()
                var discardCost: CostAtom.Discard? = null
                var discardTargets: List<EntityId>? = null
                var craftCost: AbilityCost.Craft? = null
                var craftMaterials: List<EntityId> = emptyList()
                var exileCost: CostAtom.ExileFrom? = null
                var exileTargets: List<EntityId>? = null
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
                    is AbilityCost.Atom -> when (val atom = effectiveCost.atom) {
                        is CostAtom.Mana -> {
                            if (!context.manaSolver.canPay(state, playerId, atom.cost, precomputedSources = context.availableManaSources, spellContext = abilityContext)) {
                                // If the ability has convoke or waterbend, check if the tap-to-pay
                                // helpers close the affordability gap.
                                val affordableViaConvoke = ability.hasConvoke &&
                                    context.costUtils.canAffordWithConvoke(
                                        state, playerId, atom.cost,
                                        context.costUtils.findConvokeCreatures(state, playerId),
                                        precomputedSources = context.availableManaSources
                                    )
                                val affordableViaWaterbend = ability.hasWaterbend &&
                                    context.costUtils.canAffordWithWaterbend(
                                        state, playerId, atom.cost,
                                        context.costUtils.findWaterbendPermanents(state, playerId),
                                        precomputedSources = context.availableManaSources
                                    )
                                if (!affordableViaConvoke && !affordableViaWaterbend) {
                                    costAffordable = false
                                }
                            }
                        }
                        is CostAtom.Sacrifice -> {
                            sacrificeCost = atom
                            sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                state, playerId, atom.filter, if (atom.excludeSelf) entityId else null
                            )
                            if (sacrificeTargets.size < atom.count) continue
                        }
                        is CostAtom.ReturnToHand -> {
                            bounceCost = atom
                            bounceTargets = context.costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                            if (bounceTargets.size < atom.count) continue
                        }
                        is CostAtom.TapPermanents -> {
                            tapCost = atom
                            tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, atom.filter)
                                .let { targets -> if (atom.excludeSelf) targets.filter { it != entityId } else targets }
                            if (tapTargets.size < atom.count) continue
                        }
                        is CostAtom.Discard -> {
                            val targets = context.costUtils.findDiscardTargets(state, playerId, atom.filter)
                            if (targets.size < atom.count) continue
                            // Random discard is paid automatically at cost time — no player selection.
                            if (!atom.random) {
                                discardCost = atom
                                discardTargets = targets
                            }
                        }
                        is CostAtom.ExileFrom -> {
                            val targets = context.costUtils.findExileTargets(
                                state, playerId, atom.filter, atom.zone
                            )
                            if (targets.size < atom.count) continue
                            exileCost = atom
                            exileTargets = targets
                        }
                        // Pay-life / reveal carry no enumeration-time selection or affordability gate
                        // here (life payability is validated at payment time, matching the prior
                        // fall-through behavior for these costs).
                        is CostAtom.PayLife, is CostAtom.RevealFromHand -> {}
                    }
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = container.chosenCreatureType()
                        if (chosenType == null) continue
                        val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                        sacrificeCost = CostAtom.Sacrifice(dynamicFilter)
                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(state, playerId, dynamicFilter)
                        if (sacrificeTargets.isEmpty()) continue
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
                    is AbilityCost.DiscardLastDrawnThisTurn -> {
                        // No player choice — engine picks the tracked entity at payment. Gate the
                        // ability if the controller hasn't drawn a card this turn or the tracked
                        // card has since left their hand (Jandor's Ring's "if you do not have the
                        // card still in your hand, you can't pay the cost" ruling).
                        val tracked = state.lastCardDrawnThisTurnByPlayer[playerId] ?: continue
                        if (tracked !in state.getZone(ZoneKey(playerId, Zone.HAND))) continue
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
                                is AbilityCost.Atom -> when (val atom = subCost.atom) {
                                    is CostAtom.Mana -> {
                                        if (!context.manaSolver.canPay(state, playerId, atom.cost, excludeSources = excludeFromMana, precomputedSources = context.availableManaSources, spellContext = abilityContext)) {
                                            // If the ability has convoke or waterbend, check with the tap-to-pay helpers.
                                            val affordableViaConvoke = ability.hasConvoke &&
                                                context.costUtils.canAffordWithConvoke(
                                                    state, playerId, atom.cost,
                                                    context.costUtils.findConvokeCreatures(state, playerId),
                                                    precomputedSources = context.availableManaSources
                                                )
                                            val affordableViaWaterbend = ability.hasWaterbend &&
                                                context.costUtils.canAffordWithWaterbend(
                                                    state, playerId, atom.cost,
                                                    context.costUtils.findWaterbendPermanents(state, playerId),
                                                    precomputedSources = context.availableManaSources
                                                )
                                            if (!affordableViaConvoke && !affordableViaWaterbend) {
                                                costCanBePaid = false
                                                break
                                            }
                                        }
                                    }
                                    is CostAtom.Sacrifice -> {
                                        sacrificeCost = atom
                                        sacrificeTargets = context.costUtils.findAbilitySacrificeTargets(
                                            state, playerId, atom.filter, if (atom.excludeSelf) entityId else null
                                        )
                                        if (sacrificeTargets.size < atom.count) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                    is CostAtom.TapPermanents -> {
                                        tapCost = atom
                                        tapTargets = context.costUtils.findAbilityTapTargets(state, playerId, atom.filter)
                                            .let { targets -> if (atom.excludeSelf) targets.filter { it != entityId } else targets }
                                        if (tapTargets.size < atom.count) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                    is CostAtom.ReturnToHand -> {
                                        bounceCost = atom
                                        bounceTargets = context.costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                                        if (bounceTargets.size < atom.count) {
                                            costCanBePaid = false
                                            break
                                        }
                                    }
                                    is CostAtom.ExileFrom -> {
                                        // Filter the source zone by the cost's GameObjectFilter so the
                                        // payability check matches what CostHandler will see, and so
                                        // we can surface the matching cards to the UI via
                                        // AdditionalCostData.validExileTargets (the picker prompt
                                        // for Rust Harvester's "Exile an artifact card from your
                                        // graveyard" cost).
                                        val targets = context.costUtils.findExileTargets(
                                            state, playerId, atom.filter, atom.zone
                                        )
                                        if (targets.size < atom.count) {
                                            costCanBePaid = false
                                            break
                                        }
                                        exileCost = atom
                                        exileTargets = targets
                                    }
                                    is CostAtom.Discard -> {
                                        val targets = context.costUtils.findDiscardTargets(state, playerId, atom.filter)
                                        if (targets.size < atom.count) {
                                            costCanBePaid = false
                                            break
                                        }
                                        // Random discard is paid automatically at cost time — no player selection.
                                        if (!atom.random) {
                                            discardCost = atom
                                            discardTargets = targets
                                        }
                                    }
                                    // Pay-life / reveal carry no enumeration-time gate here (matching
                                    // the prior else fall-through for these sub-costs).
                                    is CostAtom.PayLife, is CostAtom.RevealFromHand -> {}
                                }
                                is AbilityCost.SacrificeChosenCreatureType -> {
                                    val chosenType = container.chosenCreatureType()
                                    if (chosenType == null) {
                                        costCanBePaid = false
                                        break
                                    }
                                    val dynamicFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                                    sacrificeCost = CostAtom.Sacrifice(dynamicFilter)
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
                                is AbilityCost.DiscardLastDrawnThisTurn -> {
                                    val tracked = state.lastCardDrawnThisTurnByPlayer[playerId]
                                    if (tracked == null || tracked !in state.getZone(ZoneKey(playerId, Zone.HAND))) {
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

                // Compute waterbend permanent data for abilities with hasWaterbend
                val abilityWaterbendPermanents = if (ability.hasWaterbend) {
                    context.costUtils.findWaterbendPermanents(state, playerId)
                } else null

                // If cost is unaffordable, add as greyed-out option and skip expensive computations
                if (!costAffordable) {
                    val abilityManaCostString = (effectiveCost.manaCostOrNull
                        ?: (effectiveCost as? AbilityCost.Composite)?.costs?.firstNotNullOfOrNull { it.manaCostOrNull })
                        ?.toString()
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
                    craftCost, craftMaterials,
                    exileCost, exileTargets
                )

                // Calculate X cost info for activated abilities with X in their mana cost
                // or X determined by a variable cost (e.g., RemoveXPlusOnePlusOneCounters).
                // Use [effectiveCost] so generic-cost reductions (e.g., The Dominion Bracelet,
                // Starport Security) flow through to the displayed [manaCostString].
                val abilityManaCost = when (effectiveCost) {
                    is AbilityCost.Atom -> effectiveCost.manaCostOrNull
                    is AbilityCost.Composite -> effectiveCost.costs
                        .firstNotNullOfOrNull { it.manaCostOrNull }
                    else -> null
                }
                // A mana cost reduced all the way to {0} (e.g. Forge Anew's free first equip)
                // renders as an empty string; surface "{0}" so the client shows it as free rather
                // than falling back to the card's printed mana cost (ActionMenu's
                // `manaCostString || cardInfo.manaCost`).
                val abilityManaCostString = abilityManaCost?.toString()?.ifEmpty { "{0}" }
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
                val isRepeatEligible = ability.cost.manaCostOrNull != null
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
                val allTargetReqs = if (textReplacement != null) {
                    ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
                } else {
                    ability.targetRequirements
                }
                // "… of an opponent's choice" requirements (Cuombajj Witches) are picked by an
                // opponent at announcement, not by the activating player. Gate activation on them
                // being satisfiable, but surface only the controller-chosen requirements for this
                // player to select; the handler routes the opponent's pick. Satisfiability is
                // computed relative to the controller (playerId), matching how the handler finds
                // legal targets for the opponent.
                if (allTargetReqs.any { it.chooser != com.wingedsheep.sdk.scripting.targets.TargetChooser.Controller }) {
                    val allReqInfos = context.targetUtils.buildTargetInfos(state, playerId, allTargetReqs, sourceId = entityId)
                    if (!context.targetUtils.allRequirementsSatisfied(allReqInfos)) continue
                }
                val targetReqs = allTargetReqs.filter { it.chooser == com.wingedsheep.sdk.scripting.targets.TargetChooser.Controller }
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
                            convokeCreatures = abilityConvokeCreatures,
                            hasWaterbend = ability.hasWaterbend,
                            waterbendPermanents = abilityWaterbendPermanents
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
                            convokeCreatures = abilityConvokeCreatures,
                            hasWaterbend = ability.hasWaterbend,
                            waterbendPermanents = abilityWaterbendPermanents
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
                            hasWaterbend = ability.hasWaterbend,
                            waterbendPermanents = abilityWaterbendPermanents,
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
                        convokeCreatures = abilityConvokeCreatures,
                        hasWaterbend = ability.hasWaterbend,
                        waterbendPermanents = abilityWaterbendPermanents
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

        val opponentPermanents = state.getOpponents(playerId)
            .flatMap { projected.getBattlefieldControlledBy(it) }

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
                    is AbilityCost.Atom -> {
                        // Only mana costs on opponents' permanents are supported ("any player may
                        // activate"); other atoms (sacrifice/discard/…) fall through to continue.
                        val mana = effectiveCost.manaCostOrNull ?: continue
                        if (!context.manaSolver.canPay(state, playerId, mana, precomputedSources = context.availableManaSources, spellContext = anyPlayerAbilityContext)) continue
                        mana.toString()
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
                cost = AbilityCost.Atom(CostAtom.Mana(nextLevelAbility.cost)),
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
        tapCost: CostAtom.TapPermanents?,
        hasTapXPermanentsCost: Boolean,
        sacrificeTargets: List<EntityId>?,
        sacrificeCost: CostAtom.Sacrifice?,
        bounceTargets: List<EntityId>?,
        bounceCost: CostAtom.ReturnToHand?,
        counterRemovalCreatures: List<CounterRemovalCreatureData>,
        hasForageCost: Boolean = false,
        forageGraveyardCards: List<EntityId> = emptyList(),
        forageFoodTargets: List<EntityId> = emptyList(),
        blightCost: AbilityCost.Blight? = null,
        blightCreatures: List<EntityId> = emptyList(),
        discardCost: CostAtom.Discard? = null,
        discardTargets: List<EntityId>? = null,
        craftCost: AbilityCost.Craft? = null,
        craftMaterials: List<EntityId> = emptyList(),
        exileCost: CostAtom.ExileFrom? = null,
        exileTargets: List<EntityId>? = null
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
                description = tapCost.description.replaceFirstChar { it.uppercase() },
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
                description = sacrificeCost.description.replaceFirstChar { it.uppercase() },
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
                description = discardCost.description.replaceFirstChar { it.uppercase() },
                costType = "DiscardCard",
                validDiscardTargets = discardTargets,
                discardCount = discardCost.count,
                counterRemovalCreatures = counterRemovalCreatures
            )
        }
        if (bounceTargets != null && bounceCost != null) {
            return AdditionalCostData(
                description = bounceCost.description.replaceFirstChar { it.uppercase() },
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
        if (exileCost != null && exileTargets != null) {
            // ExileFromGraveyard cost (e.g. Rust Harvester's "Exile an artifact card from your
            // graveyard"). Surface the filtered candidate list and exact count so the client
            // renders a card-picker; ActivateAbilityHandler's matching fast-path pauses for a
            // SelectCardsDecision when candidate count > required count.
            return AdditionalCostData(
                description = exileCost.description.replaceFirstChar { it.uppercase() },
                costType = "ExileFromGraveyard",
                validExileTargets = exileTargets,
                exileMinCount = exileCost.count,
                exileMaxCount = exileCost.count,
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
    private fun effectStacksOnRepeat(effect: Effect): Boolean {
        effect.asConditional()?.let { conditional ->
            return effectStacksOnRepeat(conditional.then) &&
                (conditional.otherwise?.let { effectStacksOnRepeat(it) } ?: true)
        }
        return when (effect) {
            is BecomeCreatureEffect,
            is BecomeCreatureTypeEffect,
            is SetBasePowerEffect,
            is SetBasePowerToughnessEffect,
            is SetCreatureSubtypesEffect,
            is AnimateLandEffect,
            is RegenerateEffect -> false
            is CompositeEffect -> effect.effects.all { effectStacksOnRepeat(it) }
            is ModalEffect -> effect.modes.all { effectStacksOnRepeat(it.effect) }
            else -> true
        }
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
                targets = listOf(ChosenTarget.Permanent(targetId))
            )
            evaluator.evaluate(state, reduction, targetContext)
        }
    }

    private fun reduceGenericInAbilityCost(cost: AbilityCost, amount: Int): AbilityCost = when (cost) {
        is AbilityCost.Atom -> cost.manaCostOrNull
            ?.let { AbilityCost.Atom(CostAtom.Mana(it.reduceGeneric(amount))) } ?: cost
        is AbilityCost.Composite -> {
            var applied = false
            AbilityCost.Composite(cost.costs.map { sub ->
                val subMana = sub.manaCostOrNull
                if (!applied && subMana != null) {
                    applied = true
                    AbilityCost.Atom(CostAtom.Mana(subMana.reduceGeneric(amount)))
                } else sub
            })
        }
        else -> cost
    }
}
