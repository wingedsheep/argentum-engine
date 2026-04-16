package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Resolves which entities are affected by continuous effects based on AffectsFilter.
 */
internal class AffectsFilterResolver {

    /**
     * Check if an entity is a creature, preferring projected types over base types.
     * This ensures that Layer 4 type changes (e.g., Opalescence making enchantments into creatures)
     * are visible to creature-filtering effects in later layers.
     */
    private fun isCreatureInProjection(
        state: GameState,
        entityId: EntityId,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): Boolean {
        val projected = projectedValues[entityId]
        if (projected != null) {
            return "CREATURE" in projected.types || projected.isFaceDown
        }
        val container = state.getEntity(entityId) ?: return false
        val card = container.get<CardComponent>() ?: return false
        return card.typeLine.isCreature || container.has<FaceDownComponent>()
    }

    fun resolveAffectedEntities(
        state: GameState,
        sourceId: EntityId,
        filter: AffectsFilter?,
        projectedValues: Map<EntityId, MutableProjectedValues> = emptyMap()
    ): Set<EntityId> {
        if (filter == null) return setOf(sourceId)

        return when (filter) {
            is AffectsFilter.Self -> setOf(sourceId)
            is AffectsFilter.AllCreaturesYouControl -> {
                val controller = projectedController(state, sourceId, projectedValues)
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    val entityController = projectedController(state, entityId, projectedValues)
                    isCreatureInProjection(state, entityId, projectedValues) && entityController == controller
                }.toSet()
            }
            is AffectsFilter.AllCreatures -> {
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    isCreatureInProjection(state, entityId, projectedValues)
                }.toSet()
            }
            is AffectsFilter.AllCreaturesOpponentsControl -> {
                val controller = projectedController(state, sourceId, projectedValues)
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    val entityController = projectedController(state, entityId, projectedValues)
                    isCreatureInProjection(state, entityId, projectedValues) && entityController != controller
                }.toSet()
            }
            is AffectsFilter.SpecificEntities -> filter.entityIds
            is AffectsFilter.WithSubtype -> {
                state.getBattlefield().filter { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    val projected = projectedValues[entityId]
                    if (projected != null) {
                        projected.subtypes.any { it.equals(filter.subtype, ignoreCase = true) }
                    } else {
                        card.typeLine.hasSubtype(Subtype(filter.subtype))
                    }
                }.toSet()
            }
            is AffectsFilter.OtherCreaturesWithSubtype -> {
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val projected = projectedValues[entityId]
                    val hasSubtype = if (projected != null) {
                        projected.subtypes.any { it.equals(filter.subtype, ignoreCase = true) }
                    } else {
                        card.typeLine.hasSubtype(Subtype(filter.subtype))
                    }
                    isCreatureInProjection(state, entityId, projectedValues) && hasSubtype
                }.toSet()
            }
            is AffectsFilter.OtherTappedCreaturesYouControl -> {
                val controller = projectedController(state, sourceId, projectedValues)
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false
                    val container = state.getEntity(entityId) ?: return@filter false
                    container.get<CardComponent>() ?: return@filter false
                    val entityController = projectedController(state, entityId, projectedValues)
                    val isTapped = container.has<TappedComponent>()
                    isCreatureInProjection(state, entityId, projectedValues) && entityController == controller && isTapped
                }.toSet()
            }
            is AffectsFilter.OtherCreaturesYouControl -> {
                val controller = projectedController(state, sourceId, projectedValues)
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false
                    val container = state.getEntity(entityId) ?: return@filter false
                    container.get<CardComponent>() ?: return@filter false
                    val entityController = projectedController(state, entityId, projectedValues)
                    isCreatureInProjection(state, entityId, projectedValues) && entityController == controller
                }.toSet()
            }
            is AffectsFilter.AllOtherCreatures -> {
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false
                    state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    isCreatureInProjection(state, entityId, projectedValues)
                }.toSet()
            }
            is AffectsFilter.AttachedPermanent -> {
                val attachedTo = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                if (attachedTo != null) setOf(attachedTo.targetId) else emptySet()
            }
            is AffectsFilter.FaceDownCreatures -> {
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }.toSet()
            }
            is AffectsFilter.CreaturesWithCounter -> {
                val counterType = parseCounterType(filter.counterType) ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val counters = container.get<CountersComponent>()
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && (counters?.getCount(counterType) ?: 0) > 0
                }.toSet()
            }
            is AffectsFilter.OwnCreaturesWithCounter -> {
                val counterType = parseCounterType(filter.counterType) ?: return emptySet()
                val sourceController = projectedController(state, sourceId, projectedValues)
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val counters = container.get<CountersComponent>()
                    val controller = projectedController(state, entityId, projectedValues)
                    controller == sourceController &&
                        (card.typeLine.isCreature || container.has<FaceDownComponent>()) &&
                        (counters?.getCount(counterType) ?: 0) > 0
                }.toSet()
            }
            is AffectsFilter.LandsWithCounter -> {
                val counterType = parseCounterType(filter.counterType) ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val counters = container.get<CountersComponent>()
                    card.typeLine.isLand && (counters?.getCount(counterType) ?: 0) > 0
                }.toSet()
            }
            is AffectsFilter.Generic -> {
                resolveGenericFilter(state, sourceId, filter.groupFilter, projectedValues)
            }
        }
    }

    fun isSubtypeDependentFilter(filter: AffectsFilter): Boolean {
        return filter is AffectsFilter.OtherCreaturesWithSubtype ||
            filter is AffectsFilter.WithSubtype ||
            (filter is AffectsFilter.Generic && (
                filter.groupFilter.baseFilter.cardPredicates.any { it is CardPredicate.HasSubtype } ||
                filter.groupFilter.chosenSubtypeKey != null
            ))
    }

    fun isControllerDependentFilter(filter: AffectsFilter): Boolean {
        return filter is AffectsFilter.AllCreaturesYouControl ||
            filter is AffectsFilter.AllCreaturesOpponentsControl ||
            filter is AffectsFilter.OtherTappedCreaturesYouControl ||
            filter is AffectsFilter.OtherCreaturesYouControl ||
            filter is AffectsFilter.OwnCreaturesWithCounter ||
            filter is AffectsFilter.OtherCreaturesWithSubtype ||
            (filter is AffectsFilter.Generic && filter.groupFilter.baseFilter.controllerPredicate != null)
    }

    /**
     * Returns true if the filter depends on an entity's creature status.
     * These filters must be re-resolved after Layer 4 (type-changing) effects apply,
     * since a permanent may have become a creature (e.g., Opalescence) or stopped
     * being a creature after Layer 4.
     */
    fun isCreatureDependentFilter(filter: AffectsFilter): Boolean {
        return filter is AffectsFilter.AllCreatures ||
            filter is AffectsFilter.AllCreaturesYouControl ||
            filter is AffectsFilter.AllCreaturesOpponentsControl ||
            filter is AffectsFilter.OtherCreaturesYouControl ||
            filter is AffectsFilter.AllOtherCreatures ||
            filter is AffectsFilter.OtherCreaturesWithSubtype ||
            filter is AffectsFilter.OtherTappedCreaturesYouControl ||
            filter is AffectsFilter.OwnCreaturesWithCounter ||
            filter is AffectsFilter.CreaturesWithCounter ||
            filter is AffectsFilter.FaceDownCreatures ||
            (filter is AffectsFilter.Generic && filter.groupFilter.baseFilter.cardPredicates.any {
                it is CardPredicate.IsCreature
            })
    }

    private fun resolveGenericFilter(
        state: GameState,
        sourceId: EntityId,
        groupFilter: GroupFilter,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): Set<EntityId> {
        val baseFilter = groupFilter.baseFilter
        val controller = projectedController(state, sourceId, projectedValues)

        // Read chosen subtype once from source's ChosenCreatureTypeComponent if needed
        val chosenSubtype = if (groupFilter.chosenSubtypeKey != null) {
            state.getEntity(sourceId)?.get<ChosenCreatureTypeComponent>()?.creatureType
                ?: return emptySet()
        } else null

        return state.getBattlefield().filter { entityId ->
            if (groupFilter.excludeSelf && entityId == sourceId) return@filter false

            val container = state.getEntity(entityId) ?: return@filter false
            val card = container.get<CardComponent>() ?: return@filter false
            val projected = projectedValues[entityId]

            // Check controller predicate
            if (baseFilter.controllerPredicate != null) {
                val entityController = projectedController(state, entityId, projectedValues)
                when (baseFilter.controllerPredicate) {
                    ControllerPredicate.ControlledByYou -> if (entityController != controller) return@filter false
                    ControllerPredicate.ControlledByOpponent -> if (entityController == controller) return@filter false
                    ControllerPredicate.ControlledByAny -> { /* matches all */ }
                    else -> { /* other predicates not applicable in static ability context */ }
                }
            }

            // Check card predicates using projected values when available
            val types = projected?.types ?: card.typeLine.cardTypes.map { it.name }.toSet()
            val subtypes = projected?.subtypes ?: run {
                val baseSubtypes = card.typeLine.subtypes.map { it.value }.toSet()
                if (Keyword.CHANGELING in card.baseKeywords) baseSubtypes + Subtype.ALL_CREATURE_TYPES
                else baseSubtypes
            }
            val colors = projected?.colors ?: card.colors.map { it.name }.toSet()
            val keywords = projected?.keywords ?: (card.baseKeywords.map { it.name } + card.baseFlags.map { it.name }).toSet()
            val isFaceDown = projected?.isFaceDown ?: container.has<FaceDownComponent>()

            for (predicate in baseFilter.cardPredicates) {
                if (!matchesCardPredicateForProjection(predicate, card, container, projected, types, subtypes, colors, keywords, isFaceDown)) {
                    return@filter false
                }
            }

            // Check state predicates
            for (predicate in baseFilter.statePredicates) {
                if (!matchesStatePredicateForProjection(state, entityId, predicate, container, isFaceDown, projectedValues)) {
                    return@filter false
                }
            }

            // Check chosen subtype constraint (from source's ChosenCreatureTypeComponent)
            if (chosenSubtype != null) {
                if (!subtypes.any { it.equals(chosenSubtype, ignoreCase = true) }) return@filter false
            }

            true
        }.toSet()
    }

    private fun matchesStatePredicateForProjection(
        state: GameState,
        entityId: EntityId,
        predicate: StatePredicate,
        container: ComponentContainer,
        isFaceDown: Boolean,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): Boolean = when (predicate) {
        StatePredicate.IsTapped -> container.has<TappedComponent>()
        StatePredicate.IsUntapped -> !container.has<TappedComponent>()
        StatePredicate.IsAttacking -> container.has<AttackingComponent>()
        StatePredicate.IsBlocking -> container.has<BlockingComponent>()
        StatePredicate.IsBlocked -> {
            container.has<AttackingComponent>() && state.getBattlefield().any { blockerId ->
                state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
            }
        }
        StatePredicate.IsUnblocked -> {
            container.has<AttackingComponent>() && state.getBattlefield().none { blockerId ->
                state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
            }
        }
        StatePredicate.EnteredThisTurn -> container.has<EnteredThisTurnComponent>()
        StatePredicate.WasDealtDamageThisTurn -> container.has<WasDealtDamageThisTurnComponent>()
        StatePredicate.HasDealtDamage -> container.has<HasDealtDamageComponent>()
        StatePredicate.HasDealtCombatDamageToPlayer -> container.has<HasDealtCombatDamageToPlayerComponent>()
        StatePredicate.IsFaceDown -> isFaceDown
        StatePredicate.IsFaceUp -> !isFaceDown
        StatePredicate.HasMorphAbility ->
            container.has<MorphDataComponent>() || container.has<HasMorphAbilityComponent>()
        StatePredicate.IsEquipped -> {
            val attachments = container.get<AttachmentsComponent>()
            attachments != null && attachments.attachedIds.any { attachId ->
                state.getEntity(attachId)?.get<CardComponent>()?.typeLine?.isEquipment == true
            }
        }
        StatePredicate.HasGreatestPower -> hasGreatestPowerInProjection(state, entityId, container, projectedValues)
        StatePredicate.HasAnyCounter -> {
            val counters = container.get<CountersComponent>()
            counters != null && counters.counters.values.any { it > 0 }
        }
        is StatePredicate.HasCounter -> {
            val counters = container.get<CountersComponent>()
            if (counters != null) {
                try {
                    val counterType = CounterType.valueOf(
                        predicate.counterType.uppercase().replace(' ', '_')
                    )
                    (counters.getCount(counterType)) > 0
                } catch (_: IllegalArgumentException) {
                    false
                }
            } else false
        }
        is StatePredicate.Or -> predicate.predicates.any {
            matchesStatePredicateForProjection(state, entityId, it, container, isFaceDown, projectedValues)
        }
        is StatePredicate.And -> predicate.predicates.all {
            matchesStatePredicateForProjection(state, entityId, it, container, isFaceDown, projectedValues)
        }
        is StatePredicate.Not -> !matchesStatePredicateForProjection(
            state, entityId, predicate.predicate, container, isFaceDown, projectedValues
        )
    }

    private fun hasGreatestPowerInProjection(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): Boolean {
        val entityController = projectedController(state, entityId, projectedValues) ?: return false
        val entityPower = projectedValues[entityId]?.power
            ?: container.get<CardComponent>()?.baseStats?.basePower
            ?: return false
        val maxPower = state.getBattlefield()
            .filter { id ->
                projectedController(state, id, projectedValues) == entityController &&
                    isCreatureInProjection(state, id, projectedValues)
            }
            .maxOfOrNull {
                projectedValues[it]?.power
                    ?: state.getEntity(it)?.get<CardComponent>()?.baseStats?.basePower
                    ?: Int.MIN_VALUE
            }
            ?: return false
        return entityPower >= maxPower
    }

    private fun matchesCardPredicateForProjection(
        predicate: CardPredicate,
        card: CardComponent,
        container: ComponentContainer,
        projected: MutableProjectedValues?,
        types: Set<String>,
        subtypes: Set<String>,
        colors: Set<String>,
        keywords: Set<String>,
        isFaceDown: Boolean
    ): Boolean = when (predicate) {
        CardPredicate.IsCreature -> "CREATURE" in types || isFaceDown
        CardPredicate.IsLand -> "LAND" in types
        CardPredicate.IsArtifact -> "ARTIFACT" in types
        CardPredicate.IsEnchantment -> "ENCHANTMENT" in types
        CardPredicate.IsPlaneswalker -> "PLANESWALKER" in types
        CardPredicate.IsInstant -> "INSTANT" in types
        CardPredicate.IsSorcery -> "SORCERY" in types
        CardPredicate.IsPermanent -> types.any { it in setOf("CREATURE", "LAND", "ARTIFACT", "ENCHANTMENT", "PLANESWALKER") }
        CardPredicate.IsNonland -> "LAND" !in types
        CardPredicate.IsNoncreature -> "CREATURE" !in types
        CardPredicate.IsNonenchantment -> "ENCHANTMENT" !in types
        CardPredicate.IsBasicLand -> "LAND" in types && card.typeLine.supertypes.any { it.name == "BASIC" }
        CardPredicate.IsToken -> container.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()
        CardPredicate.IsNontoken -> !container.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()
        CardPredicate.IsLegendary -> "LEGENDARY" in types
        CardPredicate.IsNonlegendary -> "LEGENDARY" !in types
        is CardPredicate.HasSubtype -> if (isFaceDown) false else subtypes.any { it.equals(predicate.subtype.value, ignoreCase = true) }
        is CardPredicate.NotSubtype -> if (isFaceDown) true else subtypes.none { it.equals(predicate.subtype.value, ignoreCase = true) }
        is CardPredicate.HasAnyOfSubtypes -> if (isFaceDown) false else predicate.subtypes.any { sub -> subtypes.any { it.equals(sub.value, ignoreCase = true) } }
        is CardPredicate.HasColor -> predicate.color.name in colors
        is CardPredicate.NotColor -> predicate.color.name !in colors
        CardPredicate.IsColorless -> colors.isEmpty()
        CardPredicate.IsMulticolored -> colors.size > 1
        CardPredicate.IsMonocolored -> colors.size == 1
        is CardPredicate.HasKeyword -> predicate.keyword.name in keywords
        is CardPredicate.NotKeyword -> predicate.keyword.name !in keywords
        is CardPredicate.PowerAtMost -> (projected?.power ?: card.baseStats?.basePower ?: 0) <= predicate.max
        is CardPredicate.PowerAtLeast -> (projected?.power ?: card.baseStats?.basePower ?: 0) >= predicate.min
        is CardPredicate.PowerEquals -> (projected?.power ?: card.baseStats?.basePower) == predicate.value
        is CardPredicate.ToughnessAtMost -> (projected?.toughness ?: card.baseStats?.baseToughness ?: 0) <= predicate.max
        is CardPredicate.ToughnessAtLeast -> (projected?.toughness ?: card.baseStats?.baseToughness ?: 0) >= predicate.min
        is CardPredicate.ToughnessEquals -> (projected?.toughness ?: card.baseStats?.baseToughness) == predicate.value
        is CardPredicate.PowerOrToughnessAtLeast -> {
            val power = projected?.power ?: card.baseStats?.basePower ?: 0
            val toughness = projected?.toughness ?: card.baseStats?.baseToughness ?: 0
            power >= predicate.min || toughness >= predicate.min
        }
        is CardPredicate.ManaValueEquals -> card.manaValue == predicate.value
        is CardPredicate.ManaValueAtMost -> card.manaValue <= predicate.max
        is CardPredicate.ManaValueAtLeast -> card.manaValue >= predicate.min
        is CardPredicate.NameEquals -> card.name == predicate.name
        is CardPredicate.HasBasicLandType -> if (isFaceDown) false else subtypes.any { it.equals(predicate.landType, ignoreCase = true) }
        is CardPredicate.And -> predicate.predicates.all { matchesCardPredicateForProjection(it, card, container, projected, types, subtypes, colors, keywords, isFaceDown) }
        is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicateForProjection(it, card, container, projected, types, subtypes, colors, keywords, isFaceDown) }
        is CardPredicate.Not -> !matchesCardPredicateForProjection(predicate.predicate, card, container, projected, types, subtypes, colors, keywords, isFaceDown)
        else -> true
    }

    /**
     * Get the controller of an entity, preferring projected state over base state.
     * This ensures control-changing effects are respected during state projection.
     */
    private fun projectedController(
        state: GameState,
        entityId: EntityId,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): EntityId? {
        return projectedValues[entityId]?.controllerId
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
    }

    private fun parseCounterType(counterTypeString: String): CounterType? {
        return when (counterTypeString) {
            "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
            "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
            else -> try {
                CounterType.valueOf(counterTypeString.uppercase().replace(' ', '_'))
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
