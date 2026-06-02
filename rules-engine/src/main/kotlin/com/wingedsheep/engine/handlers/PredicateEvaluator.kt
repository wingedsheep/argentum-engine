package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent

/**
 * Evaluates the new unified predicates and filters against game state.
 *
 * This provides evaluation for:
 * - CardPredicate (type, color, subtype, mana value, P/T, keywords)
 * - StatePredicate (tapped, attacking, blocking)
 * - ControllerPredicate (youControl, opponentControls)
 * - GameObjectFilter (composed from the above predicates)
 */
class PredicateEvaluator {

    /**
     * Evaluate a GameObjectFilter against an entity using projected state.
     *
     * Always pass a [ProjectedState] — for battlefield entities this is required to see
     * continuous effects (type/color/control changes, granted keywords, P/T mods). For
     * entities in non-battlefield zones (hand, library, graveyard, stack), the projection
     * has no entry and the predicate matchers fall back to base [CardComponent] data, so
     * passing `state.projectedState` is always correct.
     *
     * Mid-projection callers (e.g. [com.wingedsheep.engine.mechanics.layers.EffectApplicator])
     * pass their intermediate projected state instead of the cached one.
     *
     * @param state Current game state
     * @param projected Projected (post-Rule 613) state — typically `state.projectedState`
     * @param entityId Entity to evaluate
     * @param filter The filter to match against
     * @param context Evaluation context (for controller resolution)
     * @return true if the entity matches all predicates in the filter
     */
    fun matches(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        filter: GameObjectFilter,
        context: PredicateContext
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        // Check controller predicate first (if specified)
        filter.controllerPredicate?.let { controllerPred ->
            if (!matchesControllerPredicate(state, projected, entityId, controllerPred, context)) {
                return false
            }
        }

        // Check all card predicates using projected state. The list is always a
        // conjunction; OR is expressed within a single CardPredicate.Or, not by a
        // filter-level flag.
        val cardMatches = filter.cardPredicates.all { predicate ->
            matchesCardPredicate(state, projected, entityId, predicate, context)
        }

        if (!cardMatches) return false

        // Check all state predicates (these use base state, not projected)
        val stateMatches = filter.statePredicates.all { predicate ->
            matchesStatePredicate(state, entityId, predicate, context)
        }

        if (!stateMatches) return false

        // Recursive union (the `or` infix): match if any branch matches in full.
        if (filter.anyOf.isNotEmpty()) {
            return filter.anyOf.any { matches(state, projected, entityId, it, context) }
        }

        return true
    }

    /**
     * Evaluate a CardPredicate against an entity using projected state.
     */
    fun matchesCardPredicate(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        predicate: CardPredicate,
        context: PredicateContext? = null
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        // Handle stack item type predicates before CardComponent check
        // since abilities on the stack don't have CardComponent
        if (predicate is CardPredicate.IsActivatedOrTriggeredAbility) {
            return container.has<ActivatedAbilityOnStackComponent>() ||
                container.has<TriggeredAbilityOnStackComponent>()
        }
        if (predicate is CardPredicate.IsTriggeredAbility) {
            return container.has<TriggeredAbilityOnStackComponent>()
        }
        if (predicate is CardPredicate.IsActivatedAbility) {
            return container.has<ActivatedAbilityOnStackComponent>()
        }
        // Stack-relative targeting predicate: read the stack entity's TargetsComponent
        // and match each chosen target against the subfilter. Works for both spells and
        // activated/triggered abilities (none of which have CardComponent for the
        // permanent-target case, but TargetsComponent is independent of card data).
        if (predicate is CardPredicate.TargetsMatching) {
            val targets = container.get<TargetsComponent>()?.targets ?: return false
            if (targets.isEmpty()) return false
            val subContext = context ?: return false
            return targets.any { chosen ->
                val targetEntityId = when (chosen) {
                    is ChosenTarget.Permanent -> chosen.entityId
                    is ChosenTarget.Card -> chosen.cardId
                    is ChosenTarget.Spell -> chosen.spellEntityId
                    is ChosenTarget.Player -> return@any false
                }
                matches(state, projected, targetEntityId, predicate.subfilter, subContext)
            }
        }

        val card = container.get<CardComponent>() ?: return false
        val projectedValues = projected.getProjectedValues(entityId)

        // Use projected types/colors/keywords if available, otherwise fall back to base.
        // The projected `types` set includes supertypes (LEGENDARY, BASIC, SNOW, ...) baked in
        // by StateProjector — mirror that here so predicates like IsLegendary work for non-
        // battlefield entities (cards in hand/library/graveyard) which have no projection entry.
        val types = projectedValues?.types ?: (
            card.typeLine.cardTypes.map { it.name } + card.typeLine.supertypes.map { it.name }
        ).toSet()
        val colors = projectedValues?.colors ?: card.colors.map { it.name }.toSet()
        val keywords = projectedValues?.keywords ?: (card.baseKeywords.map { it.name } + card.baseFlags.map { it.name }).toSet()

        return when (predicate) {
            // Type predicates - use projected types
            CardPredicate.IsCreature -> "CREATURE" in types
            CardPredicate.IsLand -> "LAND" in types
            CardPredicate.IsArtifact -> "ARTIFACT" in types
            CardPredicate.IsEnchantment -> "ENCHANTMENT" in types
            CardPredicate.IsPlaneswalker -> "PLANESWALKER" in types
            CardPredicate.IsInstant -> "INSTANT" in types
            CardPredicate.IsSorcery -> "SORCERY" in types
            CardPredicate.IsBasicLand -> "LAND" in types && card.typeLine.supertypes.any { it.name == "BASIC" }
            CardPredicate.IsPermanent -> types.any { it in setOf("CREATURE", "LAND", "ARTIFACT", "ENCHANTMENT", "PLANESWALKER") }
            CardPredicate.IsNonland -> "LAND" !in types
            CardPredicate.IsNoncreature -> "CREATURE" !in types
            CardPredicate.IsNonenchantment -> "ENCHANTMENT" !in types
            CardPredicate.IsToken -> container.has<TokenComponent>()
            CardPredicate.IsNontoken -> !container.has<TokenComponent>()
            CardPredicate.IsLegendary -> "LEGENDARY" in types
            CardPredicate.IsNonlegendary -> "LEGENDARY" !in types
            CardPredicate.HasNonManaActivatedAbility -> card.hasNonManaActivatedAbility

            // Color predicates - use projected colors
            is CardPredicate.HasColor -> predicate.color.name in colors
            is CardPredicate.NotColor -> predicate.color.name !in colors
            CardPredicate.HasChosenColor -> context?.chosenColor?.let { it.name in colors } ?: false
            CardPredicate.IsColorless -> colors.isEmpty()
            CardPredicate.IsMulticolored -> colors.size > 1
            CardPredicate.IsMonocolored -> colors.size == 1

            // Subtype predicates - use projected subtypes when available (for text-changing effects)
            // Face-down creatures have no subtypes (Rule 708.2)
            is CardPredicate.HasSubtype -> {
                if (projectedValues?.isFaceDown == true) {
                    false
                } else {
                    projectedValues?.subtypes?.any { it.equals(predicate.subtype.value, ignoreCase = true) }
                        ?: (card.typeLine.hasSubtype(predicate.subtype) ||
                            // Changeling: has all creature types in all zones
                            (Keyword.CHANGELING in card.baseKeywords &&
                                predicate.subtype.value in Subtype.ALL_CREATURE_TYPES))
                }
            }
            is CardPredicate.NotSubtype -> {
                if (projectedValues?.isFaceDown == true) {
                    true  // Face-down has no subtypes
                } else {
                    val hasSubtype = projectedValues?.subtypes?.any { it.equals(predicate.subtype.value, ignoreCase = true) }
                        ?: (card.typeLine.hasSubtype(predicate.subtype) ||
                            // Changeling: has all creature types in all zones
                            (Keyword.CHANGELING in card.baseKeywords &&
                                predicate.subtype.value in Subtype.ALL_CREATURE_TYPES))
                    !hasSubtype
                }
            }
            is CardPredicate.HasAnyOfSubtypes -> {
                if (projectedValues?.isFaceDown == true) {
                    false
                } else {
                    predicate.subtypes.any { subtype ->
                        projectedValues?.subtypes?.any { it.equals(subtype.value, ignoreCase = true) }
                            ?: (card.typeLine.hasSubtype(subtype) ||
                                (Keyword.CHANGELING in card.baseKeywords &&
                                    subtype.value in Subtype.ALL_CREATURE_TYPES))
                    }
                }
            }
            is CardPredicate.HasBasicLandType -> {
                if (projectedValues?.isFaceDown == true) {
                    false  // Face-down has no land types
                } else {
                    projectedValues?.subtypes?.any { it.equals(predicate.landType, ignoreCase = true) }
                        ?: card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(predicate.landType))
                }
            }

            // Name predicates
            is CardPredicate.NameEquals -> card.name == predicate.name

            // Keyword predicates - use projected keywords
            is CardPredicate.HasKeyword -> predicate.keyword.name in keywords
            is CardPredicate.NotKeyword -> predicate.keyword.name !in keywords

            // Mana value predicates - face-down has CMC 0 (Rule 708.2)
            is CardPredicate.ManaValueEquals -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc == predicate.value
            }
            is CardPredicate.ManaValueAtMost -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= predicate.max
            }
            is CardPredicate.ManaValueEqualsX -> {
                // The chosen number is stamped onto the context as xValue. Unbound = no match.
                val xValue = context?.xValue ?: return false
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc == xValue
            }
            is CardPredicate.ManaValueAtMostX -> {
                // Null xValue means X is unbound (legal-action enumeration runs before the
                // player chooses X). Match permissively so the cast action is offered; the
                // chosen X is enforced at cast-time validation and resolution-time re-check.
                val xValue = context?.xValue
                if (xValue == null) true
                else {
                    val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                    cmc <= xValue
                }
            }
            is CardPredicate.ManaValueAtLeast -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc >= predicate.min
            }
            is CardPredicate.ManaValueAtMostEntity -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val refManaValue = state.getEntity(refEntityId)?.get<CardComponent>()?.manaValue ?: return false
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= refManaValue
            }
            is CardPredicate.ManaValueAtMostEntityManaSpent -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val manaSpent = manaSpentToCast(state, refEntityId)
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= manaSpent
            }
            CardPredicate.ManaValueIsEven -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc % 2 == 0
            }
            CardPredicate.ManaValueIsOdd -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc % 2 != 0
            }

            // Power/toughness predicates - use projected P/T
            is CardPredicate.PowerEquals -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower
                power == predicate.value
            }
            is CardPredicate.PowerAtMost -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                power <= predicate.max
            }
            is CardPredicate.PowerAtLeast -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                power >= predicate.min
            }
            is CardPredicate.ToughnessEquals -> {
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness
                toughness == predicate.value
            }
            is CardPredicate.ToughnessAtMost -> {
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                toughness <= predicate.max
            }
            is CardPredicate.ToughnessAtLeast -> {
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                toughness >= predicate.min
            }
            is CardPredicate.PowerOrToughnessAtLeast -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                power >= predicate.min || toughness >= predicate.min
            }
            is CardPredicate.TotalPowerAndToughnessAtMost -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                (power + toughness) <= predicate.max
            }
            CardPredicate.ToughnessGreaterThanPower -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                toughness > power
            }

            is CardPredicate.PowerGreaterThanEntity -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val refContainer = state.getEntity(refEntityId) ?: return false
                // Prefer projected power for the reference (layer effects, +1/+1 counters, etc.);
                // fall back to its base printed power when projection has no entry (e.g., off-battlefield).
                val refPower = state.projectedState.getPower(refEntityId)
                    ?: refContainer.get<CardComponent>()?.baseStats?.basePower
                    ?: return false
                val candidatePower = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                candidatePower > refPower
            }

            // Source-relative predicates
            CardPredicate.NotOfSourceChosenType -> {
                val sourceId = context?.sourceId ?: return true
                val chosenType = state.getEntity(sourceId)
                    ?.get<ChosenCreatureTypeComponent>()?.creatureType
                    ?: return true
                val hasSubtype = projectedValues?.subtypes?.any { it.equals(chosenType, ignoreCase = true) }
                    ?: card.typeLine.hasSubtype(Subtype(chosenType))
                !hasSubtype
            }

            CardPredicate.SharesCreatureTypeWithSource -> {
                val sourceId = context?.sourceId ?: return false
                // Source may be a spell on the stack (e.g., Amplify resolution-time reveal) —
                // projection only covers the battlefield, so fall back to base CardComponent.
                val sourceSubtypes = projected.getSubtypes(sourceId).ifEmpty {
                    state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                }
                if (sourceSubtypes.isEmpty()) return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { entitySubtype ->
                    sourceSubtypes.any { it.equals(entitySubtype, ignoreCase = true) }
                }
            }

            CardPredicate.SharesCreatureTypeWithTriggeringEntity -> {
                val triggeringId = context?.triggeringEntityId ?: return false
                // Try projected state first, fall back to base CardComponent (for dead creatures)
                val triggeringSubtypes = projected.getSubtypes(triggeringId).ifEmpty {
                    state.getEntity(triggeringId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                }
                if (triggeringSubtypes.isEmpty()) return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { entitySubtype ->
                    triggeringSubtypes.any { it.equals(entitySubtype, ignoreCase = true) }
                }
            }

            CardPredicate.HasChosenSubtype -> {
                val sourceId = context?.sourceId ?: return false
                val chosenType = state.getEntity(sourceId)
                    ?.get<ChosenCreatureTypeComponent>()?.creatureType
                    ?: return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { it.equals(chosenType, ignoreCase = true) } ||
                    // Changeling: has all creature types in all zones
                    (Keyword.CHANGELING in card.baseKeywords &&
                        chosenType in Subtype.ALL_CREATURE_TYPES)
            }

            is CardPredicate.SharesCreatureTypeWith -> {
                val referenceId = resolveEntityReference(predicate.entity, context) ?: return false
                val referenceSubtypes = projected.getSubtypes(referenceId).ifEmpty {
                    state.getEntity(referenceId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()
                }
                if (referenceSubtypes.isEmpty()) return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { entitySubtype ->
                    referenceSubtypes.any { it.equals(entitySubtype, ignoreCase = true) }
                }
            }

            is CardPredicate.SharesColorWith -> {
                val referenceId = resolveEntityReference(predicate.entity, context) ?: return false
                val referenceColors = projected.getColors(referenceId).ifEmpty {
                    state.getEntity(referenceId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }
                if (referenceColors.isEmpty()) return false
                colors.any { it in referenceColors }
            }

            CardPredicate.SharesChosenColorWithSource -> {
                val sourceId = context?.sourceId ?: return false
                val chosenColor = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.identity.ChosenColorComponent>()?.color
                    ?: return false
                chosenColor.name in colors
            }

            CardPredicate.SharesColorWithRecipient -> {
                val recipientId = context?.recipientId ?: return false
                if (recipientId == entityId) return false
                val recipientColors = projected.getColors(recipientId).ifEmpty {
                    state.getEntity(recipientId)?.get<CardComponent>()?.colors?.map { it.name }?.toSet()
                        ?: emptySet()
                }
                if (recipientColors.isEmpty() || colors.isEmpty()) return false
                colors.any { it in recipientColors }
            }

            // Context-relative predicates (pipeline variable references)
            is CardPredicate.NameEqualsChosen -> {
                val chosenName = context?.chosenValues?.get(predicate.variableName) ?: return false
                card.name.equals(chosenName, ignoreCase = true)
            }

            is CardPredicate.HasSubtypeFromVariable -> {
                val chosenType = context?.chosenValues?.get(predicate.variableName) ?: return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { it.equals(chosenType, ignoreCase = true) } ||
                    // Changeling: has all creature types in all zones
                    (Keyword.CHANGELING in card.baseKeywords &&
                        chosenType in Subtype.ALL_CREATURE_TYPES)
            }

            is CardPredicate.HasSubtypeInStoredList -> {
                val storedTypes = context?.storedStringLists?.get(predicate.listName) ?: return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                entitySubtypes.any { subtype ->
                    storedTypes.any { it.equals(subtype, ignoreCase = true) }
                }
            }

            is CardPredicate.HasSubtypeInEachStoredGroup -> {
                val groups = context?.storedSubtypeGroups?.get(predicate.groupName) ?: return false
                if (groups.isEmpty()) return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                if (entitySubtypes.isEmpty()) return false
                groups.all { group ->
                    entitySubtypes.any { entitySubtype ->
                        group.any { it.equals(entitySubtype, ignoreCase = true) }
                    }
                }
            }

            // Composite predicates
            is CardPredicate.And -> {
                predicate.predicates.all { matchesCardPredicate(state, projected, entityId, it, context) }
            }
            is CardPredicate.Or -> {
                predicate.predicates.any { matchesCardPredicate(state, projected, entityId, it, context) }
            }
            is CardPredicate.Not -> {
                !matchesCardPredicate(state, projected, entityId, predicate.predicate, context)
            }

            // Handled before CardComponent check above — unreachable here
            CardPredicate.IsActivatedOrTriggeredAbility -> false
            CardPredicate.IsTriggeredAbility -> false
            CardPredicate.IsActivatedAbility -> false
            is CardPredicate.TargetsMatching -> false
        }
    }

    /**
     * Evaluate a ControllerPredicate using projected state (for controller-changing effects).
     */
    private fun matchesControllerPredicate(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        predicate: ControllerPredicate,
        context: PredicateContext
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        // OwnedByYou/OwnedByOpponent check the card's owner, not controller.
        // These predicates are used for cards in graveyards/exile which don't have ControllerComponent —
        // so handle them before the controller lookup below.
        return when (predicate) {
            ControllerPredicate.OwnedByYou -> {
                val card = container.get<CardComponent>()
                card?.ownerId == context.controllerId
            }
            ControllerPredicate.OwnedByOpponent -> {
                val card = container.get<CardComponent>()
                card?.ownerId != null && card.ownerId != context.controllerId
            }
            else -> {
                // Use projected controller if available; otherwise fall back to the base
                // ControllerComponent or, for stack objects (spells and abilities), the
                // controllerId stored on their stack components.
                val controllerId = projected.getController(entityId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: container.get<SpellOnStackComponent>()?.casterId
                    ?: container.get<TriggeredAbilityOnStackComponent>()?.controllerId
                    ?: container.get<ActivatedAbilityOnStackComponent>()?.controllerId
                    ?: return false
                when (predicate) {
                    ControllerPredicate.ControlledByYou -> controllerId == context.controllerId
                    ControllerPredicate.ControlledByOpponent -> controllerId != context.controllerId
                    ControllerPredicate.ControlledByAny -> true
                    ControllerPredicate.ControlledByActivePlayer -> controllerId == state.activePlayerId
                    ControllerPredicate.ControlledByTargetOpponent -> {
                        context.targetOpponentId?.let { controllerId == it } ?: false
                    }
                    ControllerPredicate.ControlledByTargetPlayer -> {
                        context.targetPlayerId?.let { controllerId == it } ?: false
                    }
                    is ControllerPredicate.ControlledByReferencedPlayer -> {
                        val referenced = context.resolvePlayerTarget(predicate.target)
                            ?: resolveReferencedPlayerFromState(state, projected, predicate.target, context)
                        referenced?.let { controllerId == it } ?: false
                    }
                    // Already handled above
                    ControllerPredicate.OwnedByYou, ControllerPredicate.OwnedByOpponent -> true
                }
            }
        }
    }

    /**
     * Resolve an [EffectTarget] player reference that needs [GameState] (so it can't be
     * answered by [PredicateContext.resolvePlayerTarget] alone). Currently covers
     * [EffectTarget.ControllerOfTriggeringEntity] — the controller of the entity that
     * caused the trigger (e.g. Tectonic Instability: "tap all lands its controller controls").
     */
    private fun resolveReferencedPlayerFromState(
        state: GameState,
        projected: ProjectedState,
        target: EffectTarget,
        context: PredicateContext,
    ): EntityId? = when (target) {
        EffectTarget.ControllerOfTriggeringEntity -> {
            val triggeringId = context.triggeringEntityId ?: return null
            projected.getController(triggeringId)
                ?: state.getEntity(triggeringId)?.get<ControllerComponent>()?.playerId
        }
        else -> null
    }

    /**
     * Resolve an EntityReference to an EntityId using the predicate context.
     */
    /**
     * Total mana actually spent to cast an entity. Reads the live [SpellOnStackComponent]
     * buckets while the entity is still a spell on the stack, otherwise the
     * [CastRecordComponent] snapshot stamped when it resolved onto the battlefield. Returns 0
     * when neither is present (entity was put onto the battlefield without being cast, or is
     * a copy created on the stack — no mana was spent in either case).
     */
    private fun manaSpentToCast(state: GameState, entityId: EntityId): Int {
        val container = state.getEntity(entityId) ?: return 0
        container.get<SpellOnStackComponent>()?.let { spell ->
            return spell.manaSpentWhite + spell.manaSpentBlue + spell.manaSpentBlack +
                spell.manaSpentRed + spell.manaSpentGreen + spell.manaSpentColorless
        }
        container.get<CastRecordComponent>()?.let { record ->
            return record.whiteSpent + record.blueSpent + record.blackSpent +
                record.redSpent + record.greenSpent + record.colorlessSpent
        }
        return 0
    }

    private fun resolveEntityReference(ref: EntityReference, context: PredicateContext?): EntityId? {
        return when (ref) {
            is EntityReference.Source -> context?.sourceId
            is EntityReference.Triggering -> context?.triggeringEntityId
            is EntityReference.Target -> null // Not available in predicate context
            is EntityReference.Sacrificed -> null
            is EntityReference.TappedAsCost -> null
            is EntityReference.AffectedEntity -> context?.affectedEntityId
            is EntityReference.IterationEntity -> null // Only available during ForEachInGroup iteration
            is EntityReference.FromCostStorage -> null // Cost-pipeline state is not threaded into PredicateContext
            is EntityReference.AmassedArmy -> null // Pipeline state is not threaded into PredicateContext
            is EntityReference.EnchantedCreature -> null // Attachment lookup needs state, not threaded here
        }
    }

    /**
     * Evaluate a StatePredicate against an entity.
     */
    fun matchesStatePredicate(
        state: GameState,
        entityId: EntityId,
        predicate: StatePredicate,
        context: PredicateContext? = null
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        return when (predicate) {
            // Tap state
            StatePredicate.IsTapped -> container.has<TappedComponent>()
            StatePredicate.IsUntapped -> !container.has<TappedComponent>()

            // Combat state
            StatePredicate.IsAttacking -> container.has<AttackingComponent>()
            StatePredicate.IsBlocking -> container.has<BlockingComponent>()
            StatePredicate.IsBlocked -> {
                // Check if this attacking creature has any blockers assigned
                val attackingComp = container.get<AttackingComponent>()
                attackingComp != null && state.getBattlefield().any { blockerId ->
                    state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
                }
            }
            StatePredicate.IsUnblocked -> {
                val attackingComp = container.get<AttackingComponent>()
                attackingComp != null && state.getBattlefield().none { blockerId ->
                    state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
                }
            }

            // Same combat band as the effect's source (CR 702.22). Resolves against
            // context.sourceId: matches the source creature itself, or a creature sharing the
            // source's non-null band id. Yields false when there's no source context or the
            // source isn't attacking, so it's inert outside combat / damage-prevention contexts.
            StatePredicate.InSameBandAsSource -> {
                val sourceId = context?.sourceId
                val sourceAttacking = sourceId?.let { state.getEntity(it)?.get<AttackingComponent>() }
                when {
                    sourceAttacking == null -> false
                    entityId == sourceId -> true
                    sourceAttacking.bandId == null -> false
                    else -> container.get<AttackingComponent>()?.bandId == sourceAttacking.bandId
                }
            }

            // Summoning sickness / ETB
            StatePredicate.EnteredThisTurn -> {
                container.has<EnteredThisTurnComponent>()
            }

            // Damage state
            StatePredicate.WasDealtDamageThisTurn -> {
                container.has<WasDealtDamageThisTurnComponent>()
            }
            StatePredicate.HasDealtDamage -> {
                container.has<HasDealtDamageComponent>()
            }
            StatePredicate.HasDealtCombatDamageToPlayer -> {
                container.has<HasDealtCombatDamageToPlayerComponent>()
            }

            // Whether this creature has been declared as an attacker this turn — derived
            // from the controller's PlayerAttackersThisTurnComponent, the same set that
            // backs raid / "you attacked with N creatures this turn" tribal triggers.
            StatePredicate.AttackedThisTurn -> {
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: return false
                val attackerSet = state.getEntity(controllerId)
                    ?.get<PlayerAttackersThisTurnComponent>()
                    ?.attackerIds ?: emptySet()
                entityId in attackerSet
            }

            // Face-down state
            StatePredicate.IsFaceDown -> container.has<FaceDownComponent>()
            StatePredicate.IsFaceUp -> !container.has<FaceDownComponent>()

            // Morph ability — check both the runtime component (face-down permanents)
            // and the card definition tag (cards in hand/library/graveyard)
            StatePredicate.HasMorphAbility ->
                container.has<MorphDataComponent>() ||
                container.has<HasMorphAbilityComponent>()

            // Counter state
            is StatePredicate.HasCounter -> {
                val countersComponent = container.get<CountersComponent>()
                if (countersComponent == null) return false
                val counterType = when (predicate.counterType) {
                    "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
                    "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
                    else -> try {
                        CounterType.valueOf(predicate.counterType.uppercase().replace(' ', '_'))
                    } catch (_: IllegalArgumentException) {
                        return false
                    }
                }
                countersComponent.getCount(counterType) > 0
            }
            StatePredicate.HasAnyCounter -> {
                val countersComponent = container.get<CountersComponent>()
                countersComponent != null && countersComponent.counters.values.any { it > 0 }
            }

            // Equipment state
            StatePredicate.IsEquipped -> {
                val attachments = container.get<AttachmentsComponent>()
                if (attachments == null || attachments.attachedIds.isEmpty()) return false
                attachments.attachedIds.any { attachId ->
                    val attachContainer = state.getEntity(attachId)
                    val card = attachContainer?.get<CardComponent>()
                    card?.typeLine?.isEquipment == true
                }
            }

            StatePredicate.IsModified -> com.wingedsheep.engine.handlers.predicates.isModified(state, entityId)

            // Saddled marker — set by BecomeSaddledExecutor when a Saddle ability resolves
            // (CR 702.171b). Cleared at end-of-turn cleanup or when the permanent leaves play.
            StatePredicate.IsSaddled -> container.has<SaddledComponent>()

            // Zone-specific marker — set by WarpExileExecutor when a warped
            // permanent is exiled at end of turn (CR 702.185b).
            StatePredicate.IsWarpExiled ->
                container.has<com.wingedsheep.engine.state.components.identity.WarpExiledComponent>()

            // Battlefield marker — set when a warped spell resolves (CR 702.185).
            StatePredicate.WasCastForWarp ->
                container.has<com.wingedsheep.engine.state.components.battlefield.WarpedComponent>()

            // Relative power
            StatePredicate.HasGreatestPower -> {
                val projected = state.projectedState
                val entityController = projected.getController(entityId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: return false
                val entityPower = projected.getPower(entityId) ?: return false
                val maxPower = state.getBattlefield()
                    .filter { id ->
                        val ctrl = projected.getController(id)
                            ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
                        ctrl == entityController && projected.isCreature(id)
                    }
                    .maxOfOrNull { projected.getPower(it) ?: Int.MIN_VALUE }
                    ?: return false
                entityPower >= maxPower
            }

            // Composite / logical combinators
            is StatePredicate.Or -> predicate.predicates.any { matchesStatePredicate(state, entityId, it, context) }
            is StatePredicate.And -> predicate.predicates.all { matchesStatePredicate(state, entityId, it, context) }
            is StatePredicate.Not -> !matchesStatePredicate(state, entityId, predicate.predicate, context)
        }
    }

    // =========================================================================
    // CastSpellRecord matching (for retroactive spell-type queries)
    // =========================================================================

    /**
     * Evaluate a GameObjectFilter against a CastSpellRecord (a snapshot of a spell's
     * characteristics at cast time). Used for "did you cast a historic spell this turn?" etc.
     *
     * Only card predicates are evaluated — state and controller predicates are skipped
     * since they're not meaningful for historical cast records.
     *
     * Face-down spells have no characteristics per CR 708.2, so only filters with
     * no card predicates (i.e. GameObjectFilter.Any) match them.
     */
    fun matchesFilter(record: CastSpellRecord, filter: GameObjectFilter): Boolean {
        if (filter.cardPredicates.isEmpty() && filter.anyOf.isEmpty()) return true
        if (record.isFaceDown) return false

        // Conjunction over card predicates; OR lives inside a CardPredicate.Or.
        if (!filter.cardPredicates.all { matchesRecordPredicate(record, it) }) return false
        // Recursive union (`or` infix): only the card predicates of each branch are
        // meaningful for a cast record; state/controller branches are skipped as above.
        if (filter.anyOf.isNotEmpty()) return filter.anyOf.any { matchesFilter(record, it) }
        return true
    }

    private fun matchesRecordPredicate(record: CastSpellRecord, predicate: CardPredicate): Boolean {
        val typeLine = record.typeLine
        return when (predicate) {
            // Type predicates
            CardPredicate.IsCreature -> typeLine.isCreature
            CardPredicate.IsLand -> typeLine.isLand
            CardPredicate.IsArtifact -> typeLine.isArtifact
            CardPredicate.IsEnchantment -> typeLine.isEnchantment
            CardPredicate.IsPlaneswalker -> com.wingedsheep.sdk.core.CardType.PLANESWALKER in typeLine.cardTypes
            CardPredicate.IsInstant -> typeLine.isInstant
            CardPredicate.IsSorcery -> typeLine.isSorcery
            CardPredicate.IsBasicLand -> typeLine.isBasicLand
            CardPredicate.IsPermanent -> typeLine.isPermanent
            CardPredicate.IsNonland -> !typeLine.isLand
            CardPredicate.IsNoncreature -> !typeLine.isCreature
            CardPredicate.IsNonenchantment -> !typeLine.isEnchantment
            CardPredicate.IsToken -> false // cast spells are never tokens
            CardPredicate.IsNontoken -> true
            CardPredicate.IsLegendary -> typeLine.isLegendary
            CardPredicate.IsNonlegendary -> !typeLine.isLegendary

            // Color predicates
            is CardPredicate.HasColor -> predicate.color in record.colors
            is CardPredicate.NotColor -> predicate.color !in record.colors
            CardPredicate.IsColorless -> record.colors.isEmpty()
            CardPredicate.IsMulticolored -> record.colors.size > 1
            CardPredicate.IsMonocolored -> record.colors.size == 1

            // Subtype predicates
            is CardPredicate.HasSubtype -> typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.NotSubtype -> !typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.any { typeLine.hasSubtype(it) }
            is CardPredicate.HasBasicLandType -> typeLine.hasSubtype(Subtype(predicate.landType))

            // Mana value predicates
            is CardPredicate.ManaValueEquals -> record.manaValue == predicate.value
            is CardPredicate.ManaValueAtMost -> record.manaValue <= predicate.max
            // ManaValueAtMostX / ManaValueEqualsX are resolution-time chosen-number predicates;
            // without context they cannot match record-level cast history.
            CardPredicate.ManaValueAtMostX -> false
            CardPredicate.ManaValueEqualsX -> false
            is CardPredicate.ManaValueAtLeast -> record.manaValue >= predicate.min
            // Entity-relative — no entity context for cast records
            is CardPredicate.ManaValueAtMostEntity -> false
            is CardPredicate.ManaValueAtMostEntityManaSpent -> false
            CardPredicate.ManaValueIsEven -> record.manaValue % 2 == 0
            CardPredicate.ManaValueIsOdd -> record.manaValue % 2 != 0

            // Power/toughness — not meaningful for cast records
            is CardPredicate.PowerEquals, is CardPredicate.PowerAtMost, is CardPredicate.PowerAtLeast,
            is CardPredicate.ToughnessEquals, is CardPredicate.ToughnessAtMost, is CardPredicate.ToughnessAtLeast,
            is CardPredicate.PowerOrToughnessAtLeast,
            is CardPredicate.TotalPowerAndToughnessAtMost,
            is CardPredicate.PowerGreaterThanEntity,
            CardPredicate.ToughnessGreaterThanPower -> false

            // Name predicates — not stored in record
            is CardPredicate.NameEquals -> false
            is CardPredicate.NameEqualsChosen -> false

            // Keyword predicates — not stored in record
            is CardPredicate.HasKeyword, is CardPredicate.NotKeyword -> false

            // Source-relative and context predicates — not applicable
            CardPredicate.NotOfSourceChosenType, CardPredicate.SharesCreatureTypeWithSource,
            CardPredicate.SharesCreatureTypeWithTriggeringEntity, CardPredicate.HasChosenSubtype,
            CardPredicate.HasChosenColor, CardPredicate.SharesChosenColorWithSource,
            CardPredicate.SharesColorWithRecipient,
            is CardPredicate.SharesCreatureTypeWith,
            is CardPredicate.SharesColorWith -> false
            is CardPredicate.HasSubtypeFromVariable, is CardPredicate.HasSubtypeInStoredList,
            is CardPredicate.HasSubtypeInEachStoredGroup -> false

            // Stack ability check — cast spells are not abilities
            CardPredicate.IsActivatedOrTriggeredAbility -> false
            CardPredicate.IsTriggeredAbility -> false
            CardPredicate.IsActivatedAbility -> false

            // A cast-spell record has no battlefield permanent to inspect for activated abilities.
            CardPredicate.HasNonManaActivatedAbility -> false

            // Stack-relative targeting predicate — historical cast records have no
            // chosen-target snapshot, so this always returns false here.
            is CardPredicate.TargetsMatching -> false

            // Composite predicates
            is CardPredicate.And -> predicate.predicates.all { matchesRecordPredicate(record, it) }
            is CardPredicate.Or -> predicate.predicates.any { matchesRecordPredicate(record, it) }
            is CardPredicate.Not -> !matchesRecordPredicate(record, predicate.predicate)
        }
    }
}

/**
 * Context for predicate evaluation, providing player references.
 */
data class PredicateContext(
    val controllerId: EntityId,
    val targetOpponentId: EntityId? = null,
    val targetPlayerId: EntityId? = null,
    val sourceId: EntityId? = null,
    /** Owner of the entity being evaluated (for graveyard targeting) */
    val ownerId: EntityId? = null,
    /** The entity that caused the trigger to fire (for SharesCreatureTypeWithTriggeringEntity) */
    val triggeringEntityId: EntityId? = null,
    /**
     * The entity a continuous effect is being applied to during projection (e.g. the creature an
     * Aura is enchanting). Lets filters resolve [EntityReference.AffectedEntity] — needed by
     * `AggregateBattlefield(filter = ...sharingCreatureTypeWith(AffectedEntity))` for Alpha Status.
     * Only set during projection-time evaluation; null otherwise.
     */
    val affectedEntityId: EntityId? = null,
    /** Named values chosen by the player during pipeline execution (e.g., creature type, color). */
    val chosenValues: Map<String, String> = emptyMap(),
    /** Named string lists stored by pipeline effects (e.g., chosen creature types). */
    val storedStringLists: Map<String, List<String>> = emptyMap(),
    /**
     * Named lists of subtype sets stored by pipeline effects — one `Set<String>` per
     * source entity. Populated by `GatherSubtypesEffect`. Used by
     * [CardPredicate.HasSubtypeInEachStoredGroup] to implement "shares a subtype with
     * each of" semantics.
     */
    val storedSubtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
    /** Ordered targets chosen for the effect; used to resolve explicit EffectTarget references. */
    val targets: List<ChosenTarget> = emptyList(),
    /** Named targets bound via the DSL, mapped by name. */
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    /**
     * The X chosen for the source spell/ability (cast-time selection or snapshot from
     * SpellOnStackComponent at resolution). Used by [CardPredicate.ManaValueAtMostX] to
     * filter targets by "mana value X or less".
     */
    val xValue: Int? = null,
    /**
     * The color chosen during the current effect's resolution (e.g. via `ChooseColorThen`).
     * Read by [CardPredicate.HasChosenColor] so filters can match "permanents of that color".
     */
    val chosenColor: Color? = null,
    /**
     * The recipient of the in-flight damage, when evaluating a damage replacement's source
     * filter. Read by [CardPredicate.SharesColorWithRecipient] so a source filter can be
     * relative to what's being damaged (Well-Laid Plans).
     */
    val recipientId: EntityId? = null
) {
    /**
     * Resolve an [EffectTarget] reference to a concrete player [EntityId].
     *
     * Supports [EffectTarget.BoundVariable] (maps by target name), [EffectTarget.ContextTarget]
     * (maps by position in [targets]), and [EffectTarget.Controller]. Returns `null` when the
     * reference doesn't resolve to a player — filters that depend on a player target will then
     * match nothing, which is the safe default.
     */
    fun resolvePlayerTarget(target: EffectTarget): EntityId? {
        val chosen: ChosenTarget? = when (target) {
            is EffectTarget.BoundVariable -> namedTargets[target.name]
            is EffectTarget.ContextTarget -> targets.getOrNull(target.index)
            EffectTarget.Controller -> return controllerId
            else -> null
        }
        return (chosen as? ChosenTarget.Player)?.playerId
    }

    companion object {
        /**
         * Create from EffectContext for compatibility.
         */
        fun fromEffectContext(context: EffectContext): PredicateContext {
            // Derive the concretely chosen player target from the effect's targets.
            // Falls back to opponentId when no player target is present — preserves the
            // historic behavior for cards that reference "target opponent" implicitly.
            val chosenPlayerTarget = context.targets.firstNotNullOfOrNull { target ->
                (target as? ChosenTarget.Player)?.playerId
            }
            return PredicateContext(
                controllerId = context.controllerId,
                targetOpponentId = context.opponentId,
                targetPlayerId = chosenPlayerTarget ?: context.opponentId,
                sourceId = context.sourceId,
                triggeringEntityId = context.triggeringEntityId,
                affectedEntityId = context.affectedEntityId,
                chosenValues = context.pipeline.chosenValues,
                storedStringLists = context.pipeline.storedStringLists,
                storedSubtypeGroups = context.pipeline.storedSubtypeGroups,
                targets = context.targets,
                namedTargets = context.pipeline.namedTargets,
                xValue = context.xValue,
                chosenColor = context.chosenColor
            )
        }
    }
}
