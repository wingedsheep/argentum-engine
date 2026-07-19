package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.ProjectedValues
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.battlefield.DealtCombatDamageToPlayersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.combat.AttackedThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.PutIntoGraveyardFromBattlefieldThisTurnMarker
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
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
     * Whether an object with **no battlefield projection entry** (a spell on the stack, a card in
     * hand/library/graveyard/exile) effectively has [subtype]. Honors its printed subtypes,
     * Changeling (all creature types in all zones, Rule 702.73), and cross-zone "is the chosen
     * type" grants (Conspiracy / Leyline of Transformation — see
     * [ProjectedState.crossZoneGrantedSubtypes]). Battlefield permanents never reach this helper;
     * their subtypes come from the projected `subtypes` set.
     */
    private fun matchesBaseSubtype(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        card: CardComponent,
        subtype: Subtype
    ): Boolean =
        card.typeLine.hasSubtype(subtype) ||
            (Keyword.CHANGELING in card.baseKeywords && subtype.value in Subtype.ALL_CREATURE_TYPES) ||
            projected.crossZoneGrantedSubtypes(state, entityId).any { it.equals(subtype.value, ignoreCase = true) }

    /**
     * The creature/other subtypes an object effectively has for "shares a type with" / chosen-type
     * comparisons. Battlefield permanents use their projected subtype set (empty if face down,
     * Rule 708.2); non-battlefield objects use their printed subtypes plus any cross-zone "is the
     * chosen type" grants ([ProjectedState.crossZoneGrantedSubtypes]). Changeling is intentionally
     * *not* folded in here (callers that honor Changeling check it separately), preserving the
     * pre-existing semantics of the share/chosen predicates.
     */
    private fun effectiveSubtypes(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        card: CardComponent,
        projectedValues: ProjectedValues?
    ): Set<String> {
        if (projectedValues != null) {
            return if (projectedValues.isFaceDown) emptySet() else projectedValues.subtypes
        }
        return card.typeLine.subtypes.map { it.value }.toSet() +
            projected.crossZoneGrantedSubtypes(state, entityId)
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
        // Composite predicates must recurse per-branch *before* the CardComponent null-check below,
        // so a heterogeneous Or/And/Not whose branches mix spell predicates (need a CardComponent)
        // with ability predicates (no CardComponent) evaluates each branch on its own terms. Without
        // this, `Or(IsInstant, IsSorcery, IsActivatedOrTriggeredAbility)` would short-circuit to
        // false for an ability on the stack (the whole composite bails at the missing CardComponent)
        // — breaking "copy/counter target spell or ability" (Return the Favor, Stifle).
        if (predicate is CardPredicate.Or) {
            return predicate.predicates.any { matchesCardPredicate(state, projected, entityId, it, context) }
        }
        if (predicate is CardPredicate.And) {
            return predicate.predicates.all { matchesCardPredicate(state, projected, entityId, it, context) }
        }
        if (predicate is CardPredicate.Not) {
            return !matchesCardPredicate(state, projected, entityId, predicate.predicate, context)
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
            // Adventure-ness is a static characteristic of the whole card (not a projected type),
            // read straight off the CardComponent flag stamped at entity creation.
            CardPredicate.HasAdventure -> card.hasAdventure
            CardPredicate.IsBasicLand -> "LAND" in types && card.typeLine.supertypes.any { it.name == "BASIC" }
            CardPredicate.IsPermanent -> types.any { it in setOf("CREATURE", "LAND", "ARTIFACT", "ENCHANTMENT", "PLANESWALKER") }
            CardPredicate.IsNonland -> "LAND" !in types
            CardPredicate.IsNoncreature -> "CREATURE" !in types
            CardPredicate.IsNonenchantment -> "ENCHANTMENT" !in types
            CardPredicate.IsNonartifact -> "ARTIFACT" !in types
            CardPredicate.IsToken -> container.has<TokenComponent>()
            CardPredicate.IsNontoken -> !container.has<TokenComponent>()
            CardPredicate.IsLegendary -> "LEGENDARY" in types
            CardPredicate.IsNonlegendary -> "LEGENDARY" !in types
            CardPredicate.HasNonManaActivatedAbility -> card.hasNonManaActivatedAbility
            CardPredicate.HasActivatedAbility -> card.hasActivatedAbility

            // Color predicates - use projected colors
            is CardPredicate.HasColor -> predicate.color.name in colors
            is CardPredicate.NotColor -> predicate.color.name !in colors
            CardPredicate.HasChosenColor -> context?.chosenColor?.let { it.name in colors } ?: false
            CardPredicate.IsColorless -> colors.isEmpty()
            CardPredicate.IsColored -> colors.isNotEmpty()
            CardPredicate.IsMulticolored -> colors.size > 1
            CardPredicate.IsMonocolored -> colors.size == 1

            // Subtype predicates - use projected subtypes when available (for text-changing effects)
            // Face-down creatures have no subtypes (Rule 708.2)
            is CardPredicate.HasSubtype -> {
                if (projectedValues?.isFaceDown == true) {
                    false
                } else {
                    projectedValues?.subtypes?.any { it.equals(predicate.subtype.value, ignoreCase = true) }
                        ?: matchesBaseSubtype(state, projected, entityId, card, predicate.subtype)
                }
            }
            is CardPredicate.NotSubtype -> {
                if (projectedValues?.isFaceDown == true) {
                    true  // Face-down has no subtypes
                } else {
                    val hasSubtype = projectedValues?.subtypes?.any { it.equals(predicate.subtype.value, ignoreCase = true) }
                        ?: matchesBaseSubtype(state, projected, entityId, card, predicate.subtype)
                    !hasSubtype
                }
            }
            is CardPredicate.HasAnyOfSubtypes -> {
                if (projectedValues?.isFaceDown == true) {
                    false
                } else {
                    predicate.subtypes.any { subtype ->
                        projectedValues?.subtypes?.any { it.equals(subtype.value, ignoreCase = true) }
                            ?: matchesBaseSubtype(state, projected, entityId, card, subtype)
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

            // CR 709 + Central Elevator ruling: a Room card may be found only if none of its
            // door names matches an *unlocked* door name of a Room the searcher controls. A Room
            // with no unlocked doors contributes neither of its names; a split Room card carries
            // both door names and is excluded if either matches. Fails open with no controller.
            is CardPredicate.NameNotSharedWithControlledRoom -> {
                val controllerId = context?.controllerId
                if (controllerId == null) {
                    true
                } else {
                    val controlledDoorNames = state.getBattlefield().flatMapTo(mutableSetOf<String>()) { id ->
                        val room = state.getEntity(id)?.get<RoomComponent>()
                        if (room == null || projected.getController(id) != controllerId) {
                            emptyList()
                        } else {
                            room.faces.filter { it.id in room.unlocked }.map { it.name }
                        }
                    }
                    card.name.split(" // ").map { it.trim() }.none { it in controlledDoorNames }
                }
            }

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
                val manaSpent = ManaSpentReader.totalSpent(state, refEntityId)
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= manaSpent
            }
            is CardPredicate.ManaValueAtMostColorsSpent -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val colorsSpent = ManaSpentReader.distinctColorsSpent(state, refEntityId)
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= colorsSpent
            }
            is CardPredicate.ManaValueAtMostDynamic -> {
                val cap = evaluateDynamicCap(state, predicate.amount, context) ?: return false
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc <= cap
            }
            CardPredicate.ManaValueIsEven -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc % 2 == 0
            }
            CardPredicate.ManaValueIsOdd -> {
                val cmc = if (projectedValues?.isFaceDown == true) 0 else card.manaValue
                cmc % 2 != 0
            }
            CardPredicate.HasXInManaCost -> {
                // Inspect the printed cost's {X} symbol, not the computed CMC. Face-down objects
                // (Rule 708.2 — no mana cost) never match.
                if (projectedValues?.isFaceDown == true) false else card.manaCost.hasX
            }

            // Power/toughness predicates - use projected P/T
            is CardPredicate.PowerEquals -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower
                power == predicate.value
            }
            is CardPredicate.PowerEqualsX -> {
                // Null xValue means X is unbound (legal-action enumeration runs before the
                // player chooses X). Match permissively so the ability is offered; the chosen
                // X is enforced at activation-time validation and resolution-time re-check —
                // mirrors ManaValueAtMostX. Once X is bound, require power to equal it exactly.
                val xValue = context?.xValue
                if (xValue == null) true
                else {
                    val power = projectedValues?.power ?: card.baseStats?.basePower
                    power == xValue
                }
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
            is CardPredicate.ToughnessAtMostX -> {
                // Only meaningful at resolution, where X is bound (e.g. Zero Point Ballad's
                // non-targeted DestroyAll). A null xValue is unexpected here; match nothing
                // rather than everything so an unbound X can't silently wipe the board.
                val xValue = context?.xValue
                if (xValue == null) false
                else {
                    val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                    toughness <= xValue
                }
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
            is CardPredicate.PowerOrToughnessAtMost -> {
                val power = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                val toughness = projectedValues?.toughness ?: card.baseStats?.baseToughness ?: 0
                power <= predicate.max || toughness <= predicate.max
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

            is CardPredicate.PowerAtMostEntity -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val refContainer = state.getEntity(refEntityId) ?: return false
                val refPower = state.projectedState.getPower(refEntityId)
                    ?: refContainer.get<CardComponent>()?.baseStats?.basePower
                    ?: return false
                val candidatePower = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                candidatePower <= refPower
            }

            is CardPredicate.PowerLessThanEntity -> {
                val refEntityId = resolveEntityReference(predicate.reference, context) ?: return false
                val refContainer = state.getEntity(refEntityId) ?: return false
                val refPower = state.projectedState.getPower(refEntityId)
                    ?: refContainer.get<CardComponent>()?.baseStats?.basePower
                    ?: return false
                val candidatePower = projectedValues?.power ?: card.baseStats?.basePower ?: 0
                candidatePower < refPower
            }

            // Source-relative predicates
            CardPredicate.NotOfSourceChosenType -> {
                val sourceId = context?.sourceId ?: return true
                val chosenType = state.getEntity(sourceId)
                    ?.chosenCreatureType()
                    ?: return true
                val hasSubtype = projectedValues?.subtypes?.any { it.equals(chosenType, ignoreCase = true) }
                    ?: (card.typeLine.hasSubtype(Subtype(chosenType)) ||
                        projected.crossZoneGrantedSubtypes(state, entityId).any { it.equals(chosenType, ignoreCase = true) })
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
                val entitySubtypes = effectiveSubtypes(state, projected, entityId, card, projectedValues)
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
                val entitySubtypes = effectiveSubtypes(state, projected, entityId, card, projectedValues)
                entitySubtypes.any { entitySubtype ->
                    triggeringSubtypes.any { it.equals(entitySubtype, ignoreCase = true) }
                }
            }

            CardPredicate.HasChosenSubtype -> {
                val sourceId = context?.sourceId ?: return false
                val chosenType = state.getEntity(sourceId)
                    ?.chosenCreatureType()
                    ?: return false
                val entitySubtypes = effectiveSubtypes(state, projected, entityId, card, projectedValues)
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
                val entitySubtypes = effectiveSubtypes(state, projected, entityId, card, projectedValues)
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

            is CardPredicate.SharesColorWithPermanentYouControl -> {
                if (colors.isEmpty()) return false
                val controllerId = context?.controllerId ?: return false
                state.getBattlefield().any { otherId ->
                    projected.getController(otherId) == controllerId &&
                        matches(state, projected, otherId, predicate.filter, context) &&
                        projected.getColors(otherId).any { it in colors }
                }
            }

            is CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl -> {
                val controllerId = context?.controllerId ?: return false
                val entitySubtypes = projectedValues?.subtypes ?: card.typeLine.subtypes.map { it.value }.toSet()
                // No shared type when no permanent you control shares any creature type with the candidate.
                state.getBattlefield().none { otherId ->
                    projected.getController(otherId) == controllerId &&
                        matches(state, projected, otherId, predicate.filter, context) &&
                        projected.getSubtypes(otherId).any { otherSubtype ->
                            entitySubtypes.any { it.equals(otherSubtype, ignoreCase = true) }
                        }
                }
            }

            CardPredicate.SharesChosenColorWithSource -> {
                val sourceId = context?.sourceId ?: return false
                val chosenColor = state.getEntity(sourceId)
                    ?.chosenColor()
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

            // Source-component name reference: the name durably chosen by the source permanent as it
            // entered (Petrified Hamlet). Read from the source's CastChoicesComponent[slot] — works
            // in static/projection contexts where there is no pipeline, as long as the predicate
            // context supplies the granting permanent as the source.
            is CardPredicate.NameEqualsChosenComponent -> {
                val sourceId = context?.sourceId ?: return false
                val chosenName = (state.getEntity(sourceId)
                    ?.get<CastChoicesComponent>()?.chosen?.get(predicate.slot)
                    as? ChoiceValue.TextChoice)?.text ?: return false
                card.name.equals(chosenName, ignoreCase = true)
            }

            is CardPredicate.OriginallyPrintedInSet ->
                card.originalSetCode?.equals(predicate.setCode, ignoreCase = true) == true

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
            is ControllerPredicate.And -> predicate.predicates.all {
                matchesControllerPredicate(state, projected, entityId, it, context)
            }
            is ControllerPredicate.Or -> predicate.predicates.any {
                matchesControllerPredicate(state, projected, entityId, it, context)
            }
            is ControllerPredicate.Not ->
                !matchesControllerPredicate(state, projected, entityId, predicate.predicate, context)
            ControllerPredicate.OwnedByYou -> {
                val card = container.get<CardComponent>()
                card?.ownerId == context.controllerId
            }
            ControllerPredicate.OwnedByOpponent -> {
                val card = container.get<CardComponent>()
                card?.ownerId != null && card.ownerId != context.controllerId
            }
            ControllerPredicate.OwnedByTargetPlayer -> {
                val card = container.get<CardComponent>()
                val targetPlayer = context.targetPlayerId
                card?.ownerId != null && targetPlayer != null && card.ownerId == targetPlayer
            }
            ControllerPredicate.OwnedByTriggeringPlayer -> {
                val card = container.get<CardComponent>()
                // Mirror TargetResolutionUtils' Player.TriggeringPlayer resolution: for a damage
                // trigger the damaged player rides on triggeringEntityId (triggeringPlayerId is only
                // set by triggers that name a distinct player), so fall back to it. A non-player
                // triggeringEntityId (e.g. a creature) can never equal a card's owner, so it's safe.
                val triggeringPlayer = context.triggeringPlayerId ?: context.triggeringEntityId
                card?.ownerId != null && triggeringPlayer != null && card.ownerId == triggeringPlayer
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
                    ControllerPredicate.ControlledByTriggeringPlayer -> {
                        // Mirror OwnedByTriggeringPlayer's resolution: the damaged player rides on
                        // triggeringEntityId for a damage trigger (triggeringPlayerId is only set by
                        // triggers that name a distinct player), so fall back to it. A non-player
                        // triggeringEntityId (e.g. a creature) can never equal a controller playerId.
                        val triggeringPlayer = context.triggeringPlayerId ?: context.triggeringEntityId
                        triggeringPlayer != null && controllerId == triggeringPlayer
                    }
                    is ControllerPredicate.ControlledByReferencedPlayer -> {
                        val referenced = context.resolvePlayerTarget(predicate.target)
                            ?: resolveReferencedPlayerFromState(state, projected, predicate.target, context)
                        referenced?.let { controllerId == it } ?: false
                    }
                    // Already handled above
                    ControllerPredicate.OwnedByYou, ControllerPredicate.OwnedByOpponent,
                    ControllerPredicate.OwnedByTriggeringPlayer,
                    is ControllerPredicate.And, is ControllerPredicate.Or, is ControllerPredicate.Not -> true
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
                ?: state.getEntity(triggeringId)
                    ?.get<LastKnownPermanentComponent>()?.snapshot?.controllerId
        }
        // The controller of the spell/ability's first chosen target — lets a filter scope to
        // "creatures with the same controller as the target" (Fear, Fire, Foes!). A target the
        // effect itself has already moved off the battlefield reads its last-known controller
        // (CR 608.2h).
        EffectTarget.TargetController -> {
            val targetId = when (val first = context.targets.firstOrNull()) {
                is ChosenTarget.Permanent -> first.entityId
                is ChosenTarget.Card -> first.cardId
                else -> null
            } ?: return null
            projected.getController(targetId)
                ?: state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(targetId)
                    ?.get<LastKnownPermanentComponent>()?.snapshot?.controllerId
        }
        else -> null
    }

    /**
     * Resolve an EntityReference to an EntityId using the predicate context.
     */
    /**
     * Resolve a [DynamicAmount] cap for [CardPredicate.ManaValueAtMostDynamic]. The amount is
     * evaluated through [DynamicAmountEvaluator] against a minimal [EffectContext] reconstructed
     * from the predicate context's controller/source/X. Returns null when there is no controller
     * to resolve player-scoped amounts against (e.g. legal-action enumeration with no context), so
     * the predicate fails closed rather than treating the cap as 0 and silently matching only
     * mana-value-0 cards.
     */
    private fun evaluateDynamicCap(
        state: GameState,
        amount: DynamicAmount,
        context: PredicateContext?,
    ): Int? {
        val controllerId = context?.controllerId ?: return null
        val effectContext = EffectContext(
            sourceId = context.sourceId,
            controllerId = controllerId,
            xValue = context.xValue,
        )
        return DynamicAmountEvaluator().evaluate(state, amount, effectContext)
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
            // Pipeline-stored entities (cost-chosen, amassed Army) are threaded into PredicateContext
            // via [storedCollections] so a target/affected-entity filter can compare against them
            // ("power <= the amassed Army's power"). Mirrors TargetResolutionUtils.resolveEntityReference.
            is EntityReference.FromCostStorage ->
                context?.storedCollections?.get(ref.collectionName)?.getOrNull(ref.index)
            is EntityReference.AmassedArmy ->
                context?.storedCollections?.get(EntityReference.AmassedArmy.STORAGE_KEY)?.firstOrNull()
            is EntityReference.EnchantedCreature -> null // Attachment lookup needs state, not threaded here
            is EntityReference.RingBearer -> null // Ring-bearer lookup needs state, not threaded here
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

            // Creature blocking the effect's source (CR 509). Source-relative: the candidate is a
            // blocker whose blocked-attacker set contains context.sourceId. Inert with no source.
            StatePredicate.IsBlockingSource -> {
                val sourceId = context?.sourceId
                sourceId != null &&
                    container.get<BlockingComponent>()?.blockedAttackerIds?.contains(sourceId) == true
            }

            // Token created by the effect's source permanent (CR 111). Source-relative: the
            // candidate's stamped CreatedByComponent.creatorId equals context.sourceId. Inert with
            // no source context or for tokens with no recorded creator.
            StatePredicate.CreatedBySource -> {
                val sourceId = context?.sourceId
                sourceId != null &&
                    container.get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()
                        ?.creatorId == sourceId
            }

            // Goblin Artisans: the candidate object isn't the target of an ability on the stack whose
            // source is another battlefield permanent sharing the effect source's name.
            StatePredicate.NotTargetedByAbilityFromSameNamedSource -> {
                val sourceId = context?.sourceId
                val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
                if (sourceId == null || sourceName == null) {
                    true
                } else {
                    val battlefield = state.getBattlefield()
                    val targetedByOtherSameNamed = state.stack.any { stackId ->
                        val stackContainer = state.getEntity(stackId) ?: return@any false
                        // Only abilities (not spells) count — "an ability from another creature named …".
                        val abilitySourceId = stackContainer.get<ActivatedAbilityOnStackComponent>()?.sourceId
                            ?: stackContainer.get<TriggeredAbilityOnStackComponent>()?.sourceId
                            ?: return@any false
                        if (abilitySourceId == sourceId) return@any false // not "another" source
                        if (abilitySourceId !in battlefield) return@any false
                        val otherName = state.getEntity(abilitySourceId)?.get<CardComponent>()?.name
                        if (otherName != sourceName) return@any false
                        // Does this ability target the candidate object?
                        val targets = stackContainer.get<TargetsComponent>()?.targets ?: emptyList()
                        targets.any { t ->
                            when (t) {
                                is ChosenTarget.Spell -> t.spellEntityId == entityId
                                is ChosenTarget.Permanent -> t.entityId == entityId
                                is ChosenTarget.Card -> t.cardId == entityId
                                else -> false
                            }
                        }
                    }
                    !targetedByOtherSameNamed
                }
            }

            // Crewed/saddled the effect's source permanent this turn (CR 702.122 / 702.171).
            // Source-relative: reads the source's CrewSaddleContributorsComponent and checks
            // membership. Inert with no source context.
            StatePredicate.CrewedOrSaddledSourceThisTurn -> {
                val sourceId = context?.sourceId
                val contributors = sourceId
                    ?.let { state.getEntity(it)?.get<CrewSaddleContributorsComponent>() }
                contributors != null && entityId in contributors.creatureIds
            }
            StatePredicate.CrewedOrSaddledBySourceThisTurn -> {
                // Mirror of CrewedOrSaddledSourceThisTurn: the candidate (entityId) is the Vehicle,
                // the source is the crewer. Read the candidate's contributors and ask whether the
                // source is among them.
                val sourceId = context?.sourceId
                val contributors = state.getEntity(entityId)?.get<CrewSaddleContributorsComponent>()
                sourceId != null && contributors != null && sourceId in contributors.creatureIds
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

            // Dealt combat damage this turn to the player who controls the effect's source.
            // Source-relative: resolves context.sourceId's controller and checks the candidate's
            // per-turn recipient marker. "...a creature that dealt combat damage to you this turn"
            // (Witch-king of Angmar). Inert with no source context.
            StatePredicate.DealtCombatDamageToSourceControllerThisTurn -> {
                val sourceId = context?.sourceId
                val sourceController = sourceId
                    ?.let { state.getEntity(it)?.get<ControllerComponent>()?.playerId }
                sourceController != null &&
                    container.get<DealtCombatDamageToPlayersThisTurnComponent>()
                        ?.playerIds?.contains(sourceController) == true
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

            // Whether this creature was declared as an attacker / blocker this combat — backed by the
            // per-entity AttackedThisCombatComponent / BlockedThisCombatComponent markers, stamped at
            // declaration time and cleared only when the combat phase ends (so they survive deaths and
            // removal-from-combat that clear the live AttackingComponent/BlockingComponent).
            StatePredicate.AttackedThisCombat -> {
                container.has<AttackedThisCombatComponent>()
            }

            StatePredicate.BlockedThisCombat -> {
                container.has<BlockedThisCombatComponent>()
            }

            // "Put there from the battlefield this turn" filter for graveyard-zone targets
            // (Samwise the Stouthearted, Lobelia Sackville-Baggins — LTR). Reads the marker
            // set by ZoneTransitionService on battlefield→graveyard moves. The marker is
            // stripped when the card leaves the graveyard (so a later mill→graveyard or
            // exile→graveyard arrival doesn't falsely match) AND at the start of every
            // turn by BeginningPhaseManager (so the "this turn" window matches MTG's
            // per-turn semantics, not the engine's per-round turn counter).
            StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn -> {
                container.has<PutIntoGraveyardFromBattlefieldThisTurnMarker>()
            }

            // "Blocked or was blocked by a legendary creature this turn" (You Cannot Pass! — LTR).
            // Reads the marker stamped at block declaration; survives the legendary partner leaving.
            StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn -> {
                container.has<com.wingedsheep.engine.state.components.combat.BlockedOrWasBlockedByLegendaryThisTurnComponent>()
            }

            // Face-down state
            StatePredicate.IsFaceDown -> container.has<FaceDownComponent>()
            StatePredicate.IsFaceUp -> !container.has<FaceDownComponent>()

            // Morph ability — check both the runtime component (face-down permanents)
            // and the card definition tag (cards in hand/library/graveyard)
            StatePredicate.HasMorphAbility ->
                container.has<MorphDataComponent>() ||
                container.has<HasMorphAbilityComponent>()

            // Ring-bearer designation (CR 701.54e): only while it has the component AND is controlled
            // by the player who designated it.
            StatePredicate.IsRingBearer -> {
                val bearer = container.get<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()
                bearer != null && state.projectedState.getController(entityId) == bearer.ownerId
            }

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

            // Attached-to-type — entity has an AttachedToComponent and the referenced
            // permanent currently has the requested CardType (Pyramids: "Aura attached
            // to a land"). Reads the attached-to permanent's projected types so that a
            // permanent animated into a different type via continuous effects (e.g. a
            // land turned into a creature) is matched correctly.
            is StatePredicate.AttachedToCardType -> {
                val attached = container.get<AttachedToComponent>() ?: return false
                state.projectedState.hasType(attached.targetId, predicate.cardType.name)
            }

            // General attachment match — entity is attached to a host that matches the nested
            // filter, evaluated against projected battlefield state so the host's control ("a
            // creature you control"), card type, keywords, P/T all compose (Stolen Uniform's
            // "attached to a creature you control"). The "you" of any controller predicate is the
            // controllerId carried in this evaluation context.
            is StatePredicate.AttachedTo -> {
                val attached = container.get<AttachedToComponent>() ?: return false
                // A controller predicate in the nested filter needs a "you"; without an evaluation
                // context fall back to the host's actual controller so the match can still run.
                val ctx = context ?: PredicateContext(
                    controllerId = state.projectedState.getController(attached.targetId) ?: return false
                )
                matches(state, state.projectedState, attached.targetId, predicate.filter, ctx)
            }

            // Source-relative — the candidate IS the effect's source permanent itself
            // (GameObjectFilter counterpart of GroupFilter's Scope.Self). Backs the granted
            // PreventActivatedAbilities form (Braided Net), where the activation-legality
            // check supplies the grant's holder as the source. False with no source context.
            StatePredicate.IsSource -> context?.sourceId == entityId
            StatePredicate.IsGrantingPermanent -> context?.granterId != null && context.granterId == entityId

            // Source-relative — the candidate is the permanent the effect's source is attached
            // to (its enchanted/equipped creature). Read the source's AttachedToComponent and
            // compare its targetId to the candidate. False with no source or an unattached source.
            // Negated via StatePredicate.Not for "other than enchanted creature" edicts
            // (Sporogenic Infection).
            StatePredicate.IsAttachedToBySource -> {
                val sourceId = context?.sourceId ?: return false
                val attachedTo = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                attachedTo == entityId
            }

            // Mirror of IsAttachedToBySource: the candidate is an Aura/Equipment attached TO the
            // source (static on the host, e.g. Cloud's "an Equipment attached to it").
            StatePredicate.IsAttachedToSource -> {
                val sourceId = context?.sourceId ?: return false
                val attachedTo = state.getEntity(entityId)?.get<AttachedToComponent>()?.targetId
                attachedTo == sourceId
            }

            // Source-relative: the candidate card was exiled by the effect's source permanent, i.e.
            // its id is recorded in the source's LinkedExileComponent. Backs "target card exiled
            // with ~" reanimation (The Darkness Crystal). Inert with no source context.
            StatePredicate.ExiledWithSource -> {
                val sourceId = context?.sourceId ?: return false
                state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                    ?.exiledIds?.contains(entityId) == true
            }

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

            // Cast-origin zone of a spell on the stack — reads the engine-stamped
            // SpellOnStackComponent.castFromZone (Wash Away's "wasn't cast from its owner's hand").
            is StatePredicate.WasCastFromZone ->
                container.get<SpellOnStackComponent>()?.castFromZone == predicate.zone

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

            // Global least power: the minimum power among *all* creatures on the battlefield,
            // regardless of controller. Ties leave every minimum-power creature matching, so a
            // downstream "choose one" selection breaks them (Drop of Honey).
            StatePredicate.HasLeastPowerAmongAllCreatures -> {
                val projected = state.projectedState
                if (!projected.isCreature(entityId)) return false
                val entityPower = projected.getPower(entityId) ?: return false
                val minPower = state.getBattlefield()
                    .filter { projected.isCreature(it) }
                    .minOfOrNull { projected.getPower(it) ?: Int.MAX_VALUE }
                    ?: return false
                entityPower <= minPower
            }

            StatePredicate.HasLeastPower -> {
                val projected = state.projectedState
                val entityController = projected.getController(entityId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: return false
                val entityPower = projected.getPower(entityId) ?: return false
                val minPower = state.getBattlefield()
                    .filter { id ->
                        val ctrl = projected.getController(id)
                            ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
                        ctrl == entityController && projected.isCreature(id)
                    }
                    .minOfOrNull { projected.getPower(it) ?: Int.MAX_VALUE }
                    ?: return false
                entityPower <= minPower
            }

            // Rooms: has at least one locked door (CR 709.5c). The unlock-a-door targeting
            // restriction — a fully-unlocked Room (or any non-Room) has no door to unlock.
            StatePredicate.HasLockedDoor ->
                container.get<RoomComponent>()?.lockedFaces?.isNotEmpty() == true

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
            // A cast-spell record stores only the resolved characteristics, not the card's layout,
            // so adventure-ness can't be recovered here. No in-scope card queries it against cast
            // history; fall through to the safe default.
            CardPredicate.HasAdventure -> false
            CardPredicate.IsBasicLand -> typeLine.isBasicLand
            CardPredicate.IsPermanent -> typeLine.isPermanent
            CardPredicate.IsNonland -> !typeLine.isLand
            CardPredicate.IsNoncreature -> !typeLine.isCreature
            CardPredicate.IsNonenchantment -> !typeLine.isEnchantment
            CardPredicate.IsNonartifact -> !typeLine.isArtifact
            CardPredicate.IsToken -> false // cast spells are never tokens
            CardPredicate.IsNontoken -> true
            CardPredicate.IsLegendary -> typeLine.isLegendary
            CardPredicate.IsNonlegendary -> !typeLine.isLegendary

            // Color predicates
            is CardPredicate.HasColor -> predicate.color in record.colors
            is CardPredicate.NotColor -> predicate.color !in record.colors
            CardPredicate.IsColorless -> record.colors.isEmpty()
            CardPredicate.IsColored -> record.colors.isNotEmpty()
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
            is CardPredicate.ManaValueAtMostColorsSpent -> false
            is CardPredicate.ManaValueAtMostDynamic -> false
            CardPredicate.ManaValueIsEven -> record.manaValue % 2 == 0
            CardPredicate.ManaValueIsOdd -> record.manaValue % 2 != 0
            // A cast-spell record stores the resolved mana value, not the printed cost, so we
            // cannot recover whether {X} was in the printed cost.
            CardPredicate.HasXInManaCost -> false

            // Power/toughness — not meaningful for cast records
            is CardPredicate.PowerEquals, is CardPredicate.PowerAtMost, is CardPredicate.PowerAtLeast,
            CardPredicate.PowerEqualsX,
            is CardPredicate.ToughnessEquals, is CardPredicate.ToughnessAtMost, is CardPredicate.ToughnessAtLeast,
            CardPredicate.ToughnessAtMostX,
            is CardPredicate.PowerOrToughnessAtLeast,
            is CardPredicate.PowerOrToughnessAtMost,
            is CardPredicate.TotalPowerAndToughnessAtMost,
            is CardPredicate.PowerGreaterThanEntity,
            is CardPredicate.PowerAtMostEntity,
            is CardPredicate.PowerLessThanEntity,
            CardPredicate.ToughnessGreaterThanPower -> false

            // Name predicates — matched against the record's card name; a record without a
            // name (predating name tracking) is unknown and never equals a given name.
            is CardPredicate.NameEquals -> record.name == predicate.name
            is CardPredicate.NameEqualsChosen -> false
            CardPredicate.NameNotSharedWithControlledRoom -> false
            is CardPredicate.OriginallyPrintedInSet -> false

            // Keyword predicates — not stored in record
            is CardPredicate.HasKeyword, is CardPredicate.NotKeyword -> false

            // Source-relative and context predicates — not applicable
            CardPredicate.NotOfSourceChosenType, CardPredicate.SharesCreatureTypeWithSource,
            CardPredicate.SharesCreatureTypeWithTriggeringEntity, CardPredicate.HasChosenSubtype,
            CardPredicate.HasChosenColor, CardPredicate.SharesChosenColorWithSource,
            CardPredicate.SharesColorWithRecipient,
            is CardPredicate.SharesCreatureTypeWith,
            is CardPredicate.SharesColorWith,
            is CardPredicate.SharesColorWithPermanentYouControl,
            is CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl -> false
            is CardPredicate.HasSubtypeFromVariable, is CardPredicate.HasSubtypeInStoredList,
            is CardPredicate.HasSubtypeInEachStoredGroup -> false
            // Source-component name reference is a permanent-static predicate, not meaningful for a
            // cast-spell record.
            is CardPredicate.NameEqualsChosenComponent -> false

            // Stack ability check — cast spells are not abilities
            CardPredicate.IsActivatedOrTriggeredAbility -> false
            CardPredicate.IsTriggeredAbility -> false
            CardPredicate.IsActivatedAbility -> false

            // A cast-spell record has no battlefield permanent to inspect for activated abilities.
            CardPredicate.HasNonManaActivatedAbility -> false
            CardPredicate.HasActivatedAbility -> false

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
    /**
     * The permanent whose static ability granted the resolving ability (the Equipment/Aura bearing
     * a `GrantActivatedAbility`/`GrantTriggeredAbility`). Lets a target filter resolve
     * [StatePredicate.IsGrantingPermanent] — e.g. "an artifact other than [this granting Equipment]"
     * (Dire Blunderbuss). Null for ungranted abilities.
     */
    val granterId: EntityId? = null,
    /** The entity that caused the trigger to fire (for SharesCreatureTypeWithTriggeringEntity) */
    val triggeringEntityId: EntityId? = null,
    /**
     * The player associated with the trigger (the damaged player for damage triggers, the player
     * whose event fired otherwise). Lets a target filter resolve
     * [EffectTarget.PlayerRef] of [Player.TriggeringPlayer] — e.g. "target creature **that player**
     * controls" on Fear of Burning Alive's "deals noncombat damage to an opponent" trigger.
     */
    val triggeringPlayerId: EntityId? = null,
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
    /**
     * Named lists of entity ids stored by pipeline effects — e.g. the amassed Army under
     * [EntityReference.AmassedArmy.STORAGE_KEY], or a cost-chosen entity under its `storeAs` key.
     * Lets a target/affected-entity filter resolve [EntityReference.AmassedArmy] /
     * [EntityReference.FromCostStorage] and compare against the stored entity's projected
     * characteristics ("power <= the amassed Army's power" — Grishnákh, Brash Instigator).
     * Threaded from `EffectContext.pipeline.storedCollections`; empty when no pipeline state exists.
     */
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
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
            is EffectTarget.PlayerRef -> return when (target.player) {
                Player.You -> controllerId
                Player.TriggeringPlayer -> triggeringPlayerId
                Player.TargetPlayer, Player.TargetOpponent -> targetPlayerId
                else -> null
            }
            else -> null
        }
        return (chosen as? ChosenTarget.Player)?.playerId
    }

    companion object {
        /**
         * Create from EffectContext for compatibility.
         */
        fun fromEffectContext(context: EffectContext): PredicateContext {
            // "Target opponent" / "target player" predicates read the concretely chosen
            // player target — never a turn-order-derived opponent.
            val chosenPlayerTarget = context.targets.firstNotNullOfOrNull { target ->
                (target as? ChosenTarget.Player)?.playerId
            }
            return PredicateContext(
                controllerId = context.controllerId,
                targetOpponentId = chosenPlayerTarget,
                targetPlayerId = chosenPlayerTarget,
                sourceId = context.sourceId,
                granterId = context.granterId,
                triggeringEntityId = context.triggeringEntityId,
                triggeringPlayerId = context.triggeringPlayerId,
                affectedEntityId = context.affectedEntityId,
                chosenValues = context.pipeline.chosenValues,
                storedStringLists = context.pipeline.storedStringLists,
                storedSubtypeGroups = context.pipeline.storedSubtypeGroups,
                storedCollections = context.pipeline.storedCollections,
                targets = context.targets,
                namedTargets = context.pipeline.namedTargets,
                xValue = context.xValue,
                chosenColor = context.chosenColor
            )
        }
    }
}
