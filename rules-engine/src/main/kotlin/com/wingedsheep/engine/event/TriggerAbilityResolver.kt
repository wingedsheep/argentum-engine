package com.wingedsheep.engine.event

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.GrantWardToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Resolves triggered abilities for entities by checking the AbilityRegistry,
 * CardRegistry, granted abilities, and static-granted abilities.
 */
class TriggerAbilityResolver(
    private val cardRegistry: CardRegistry,
    private val abilityRegistry: AbilityRegistry
) {

    /**
     * Get triggered abilities for a card, checking both the AbilityRegistry
     * and falling back to the CardRegistry for card definitions.
     *
     * If the entity has a TextReplacementComponent (from Artificial Evolution etc.),
     * creature type references in triggers and effects are transformed accordingly.
     */
    fun getTriggeredAbilities(entityId: EntityId, cardDefinitionId: String, state: GameState): List<TriggeredAbility> {
        // First check the AbilityRegistry (for manually registered abilities)
        val registryAbilities = abilityRegistry.getTriggeredAbilities(entityId, cardDefinitionId)
        val base = if (registryAbilities.isNotEmpty()) {
            registryAbilities
        } else {
            // Fall back to looking up from CardRegistry, including class-level-gated abilities
            val cardDef = cardRegistry.getCard(cardDefinitionId)
            val classLevel = state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel
            cardDef?.script?.effectiveTriggeredAbilities(classLevel) ?: emptyList()
        }

        // Merge in any temporarily granted triggered abilities (e.g., from Commando Raid)
        val grantedAbilities = state.grantedTriggeredAbilities
            .filter { it.entityId == entityId }
            .map { it.ability }

        // Merge in triggered abilities granted by static abilities on other permanents
        // (e.g., Hunter Sliver granting provoke to all Slivers)
        val staticGrantedAbilities = getStaticGrantedTriggeredAbilities(entityId, state)

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWardToGroup
        val wardAbilities = getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + wardAbilities
        val combined = if (allGranted.isNotEmpty()) base + allGranted else base

        // Apply text replacement if the entity has one
        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        return if (textReplacement != null) {
            combined.map { it.applyTextReplacement(textReplacement) }
        } else {
            combined
        }
    }

    /**
     * Get triggered abilities granted by static abilities on battlefield permanents.
     * E.g., Hunter Sliver grants provoke to all Sliver creatures via
     * GrantTriggeredAbilityToCreatureGroup.
     *
     * Scans all battlefield permanents for this static ability type, checks if the
     * target entity matches the filter using its projected card data.
     */
    private fun getStaticGrantedTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> {
        val registry = cardRegistry
        val targetContainer = state.getEntity(entityId) ?: return emptyList()
        val targetCard = targetContainer.get<CardComponent>() ?: return emptyList()
        val projected = state.projectedState
        val targetControllerId = projected.getController(entityId)

        val result = mutableListOf<TriggeredAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            // Skip face-down permanents — they have no abilities
            if (container.has<FaceDownComponent>()) continue

            val sourceControllerId = projected.getController(permanentId) ?: continue

            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability !is GrantTriggeredAbilityToCreatureGroup) continue

                // Check if the target entity matches the filter's card predicates
                val filter = ability.filter.baseFilter
                val matchesAll = filter.cardPredicates.all { predicate ->
                    when (predicate) {
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                            targetCard.typeLine.isCreature
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                            targetCard.typeLine.hasSubtype(predicate.subtype)
                        else -> true
                    }
                }
                if (!matchesAll) continue

                // Check controller predicate relative to the source permanent's controller
                val controllerMatch = when (filter.controllerPredicate) {
                    is ControllerPredicate.ControlledByYou -> targetControllerId == sourceControllerId
                    is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != sourceControllerId
                    null -> true
                    else -> true
                }
                if (controllerMatch) {
                    result.add(ability.ability)
                }
            }
        }

        return result
    }

    /**
     * Variant of getTriggeredAbilities that uses pre-computed grant providers
     * instead of scanning the battlefield for GrantTriggeredAbilityToCreatureGroup.
     * Reduces O(N^2) to O(N*P) where P = number of grant providers (typically 0-2).
     */
    fun getTriggeredAbilitiesWithProviders(
        entityId: EntityId,
        cardDefinitionId: String,
        state: GameState,
        grantProviders: List<TriggerIndex.GrantProviderEntry>
    ): List<TriggeredAbility> {
        // If the entity has lost all abilities (e.g., Deep Freeze), suppress its own triggered abilities
        val hasLostAbilities = state.projectedState.hasLostAllAbilities(entityId)

        val base = if (hasLostAbilities) {
            emptyList()
        } else {
            val registryAbilities = abilityRegistry.getTriggeredAbilities(entityId, cardDefinitionId)
            if (registryAbilities.isNotEmpty()) {
                registryAbilities
            } else {
                val cardDef = cardRegistry.getCard(cardDefinitionId)
                val classLevel = state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel
                cardDef?.script?.effectiveTriggeredAbilities(classLevel) ?: emptyList()
            }
        }

        val grantedAbilities = state.grantedTriggeredAbilities
            .filter { it.entityId == entityId }
            .map { it.ability }

        val staticGrantedAbilities = if (grantProviders.isNotEmpty()) {
            getStaticGrantedFromProviders(entityId, state, grantProviders)
        } else {
            emptyList()
        }

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWardToGroup
        val wardAbilities = if (hasLostAbilities) emptyList()
            else getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + wardAbilities
        val combined = if (allGranted.isNotEmpty()) base + allGranted else base

        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        return if (textReplacement != null) {
            combined.map { it.applyTextReplacement(textReplacement) }
        } else {
            combined
        }
    }

    /**
     * Fast lookup of static-granted triggered abilities using pre-computed providers.
     */
    private fun getStaticGrantedFromProviders(
        entityId: EntityId,
        state: GameState,
        grantProviders: List<TriggerIndex.GrantProviderEntry>
    ): List<TriggeredAbility> {
        val targetContainer = state.getEntity(entityId) ?: return emptyList()
        val targetCard = targetContainer.get<CardComponent>() ?: return emptyList()
        val projected = state.projectedState
        val targetControllerId = projected.getController(entityId)

        return buildList {
            for (entry in grantProviders) {
                val filter = entry.grant.filter.baseFilter
                val matchesAll = filter.cardPredicates.all { predicate ->
                    when (predicate) {
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                            targetCard.typeLine.isCreature
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                            targetCard.typeLine.hasSubtype(predicate.subtype)
                        else -> true
                    }
                }
                if (!matchesAll) continue

                // Check controller predicate relative to the source permanent's controller
                val controllerMatch = when (filter.controllerPredicate) {
                    is ControllerPredicate.ControlledByYou -> targetControllerId == entry.sourceControllerId
                    is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != entry.sourceControllerId
                    null -> true
                    else -> true
                }
                if (controllerMatch) add(entry.grant.ability)
            }
        }
    }

    /**
     * Generate ward triggered abilities for an entity.
     *
     * Checks two sources:
     * 1. Intrinsic ward from the card's keywordAbilities (e.g., KeywordAbility.WardMana)
     * 2. Ward granted by GrantWardToGroup static abilities on other permanents
     *
     * Each found ward produces a TriggeredAbility that fires on BecomesTargetEvent
     * by an opponent, with a WardCounterEffect for the appropriate cost.
     */
    private fun getWardTriggeredAbilities(
        entityId: EntityId,
        cardDefinitionId: String,
        state: GameState
    ): List<TriggeredAbility> {
        val result = mutableListOf<TriggeredAbility>()

        // 1. Intrinsic ward from card definition
        val cardDef = cardRegistry.getCard(cardDefinitionId)
        if (cardDef != null) {
            for (ka in cardDef.keywordAbilities) {
                val cost: WardCost? = when (ka) {
                    is KeywordAbility.WardMana -> WardCost.Mana(ka.cost.toString())
                    is KeywordAbility.WardLife -> WardCost.Life(ka.amount)
                    else -> null
                }
                if (cost != null) {
                    result.add(createWardTriggeredAbility(cost, "intrinsic"))
                }
            }
        }

        // 2. Ward granted by GrantWardToGroup static abilities on battlefield permanents
        val targetContainer = state.getEntity(entityId) ?: return result
        val targetCard = targetContainer.get<CardComponent>() ?: return result
        val projected = state.projectedState
        val targetControllerId = projected.getController(entityId)

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val sourceControllerId = projected.getController(permanentId) ?: continue
            val sourceDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Check all static abilities including class-level ones
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            val allStaticAbilities = sourceDef.script.effectiveStaticAbilities(classLevel)

            for (ability in allStaticAbilities) {
                if (ability !is GrantWardToGroup) continue

                // Use the generic filter resolver to check if entity matches
                val filter = ability.filter.baseFilter
                val matchesAll = filter.cardPredicates.all { predicate ->
                    when (predicate) {
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                            projected.isCreature(entityId)
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent -> true // On battlefield = permanent
                        is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                            targetCard.typeLine.hasSubtype(predicate.subtype)
                        else -> true
                    }
                }
                if (!matchesAll) continue

                // Check state predicates (e.g., HasAnyCounter)
                val matchesState = filter.statePredicates.all { predicate ->
                    when (predicate) {
                        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasAnyCounter -> {
                            val counters = targetContainer.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                            counters != null && counters.counters.values.any { it > 0 }
                        }
                        else -> true
                    }
                }
                if (!matchesState) continue

                // Check controller predicate
                val controllerMatch = when (filter.controllerPredicate) {
                    is ControllerPredicate.ControlledByYou -> targetControllerId == sourceControllerId
                    is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != sourceControllerId
                    null -> true
                    else -> true
                }
                if (!controllerMatch) continue

                // Check excludeSelf
                if (ability.filter.excludeSelf && entityId == permanentId) continue

                result.add(createWardTriggeredAbility(ability.cost, "granted_${permanentId.value}"))
            }
        }

        return result
    }

    private fun createWardTriggeredAbility(cost: WardCost, source: String): TriggeredAbility {
        return TriggeredAbility(
            id = AbilityId("ward_$source"),
            trigger = GameEvent.BecomesTargetEvent(byOpponent = true),
            binding = TriggerBinding.SELF,
            effect = WardCounterEffect(cost)
        )
    }
}
