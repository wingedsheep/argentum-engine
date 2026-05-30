package com.wingedsheep.engine.event

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SuppressesWardForGroupComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Resolves triggered abilities for entities by checking the AbilityRegistry,
 * CardRegistry, granted abilities, and static-granted abilities.
 */
class TriggerAbilityResolver(
    private val cardRegistry: CardRegistry,
    private val abilityRegistry: AbilityRegistry
) {
    private val predicateEvaluator = PredicateEvaluator()

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
            val topLevel = cardDef?.script?.effectiveTriggeredAbilities(classLevel) ?: emptyList()
            topLevel + getRoomFaceTriggeredAbilities(entityId, cardDef, state)
        }

        // Merge in any temporarily granted triggered abilities (e.g., from Commando Raid)
        val grantedAbilities = state.grantedTriggeredAbilities
            .filter { it.entityId == entityId }
            .map { it.ability }

        // Merge in triggered abilities granted by static abilities on other permanents
        // (e.g., Hunter Sliver granting provoke to all Slivers)
        val staticGrantedAbilities = getStaticGrantedTriggeredAbilities(entityId, state)
        val attachedGrantedAbilities = getAttachedGrantedTriggeredAbilities(entityId, state)

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWard
        val wardAbilities = getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        val ringBearerAbilities = getRingBearerAbilities(entityId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + attachedGrantedAbilities + wardAbilities + ringBearerAbilities
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
     * GrantTriggeredAbility.
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
                if (ability !is GrantTriggeredAbility) continue
                if (ability.filter.scope !is Scope.Battlefield) continue

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
     * instead of scanning the battlefield for GrantTriggeredAbility.
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
                val topLevel = cardDef?.script?.effectiveTriggeredAbilities(classLevel) ?: emptyList()
                topLevel + getRoomFaceTriggeredAbilities(entityId, cardDef, state)
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
        val attachedGrantedAbilities = getAttachedGrantedTriggeredAbilities(entityId, state)

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWard
        val wardAbilities = if (hasLostAbilities) emptyList()
            else getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        // The Ring emblem's abilities belong to the emblem (a player object), not the creature, so
        // they survive "loses all abilities" effects on the Ring-bearer (CR 701.52c).
        val ringBearerAbilities = getRingBearerAbilities(entityId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + attachedGrantedAbilities + wardAbilities + ringBearerAbilities
        val combined = if (allGranted.isNotEmpty()) base + allGranted else base

        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        return if (textReplacement != null) {
            combined.map { it.applyTextReplacement(textReplacement) }
        } else {
            combined
        }
    }

    /**
     * The Ring emblem's cumulative triggered abilities (CR 701.52c) for [entityId] when it is a
     * player's Ring-bearer. "Is your Ring-bearer" requires the creature to be under that owner's
     * control (CR 701.52e), so a Ring-bearer that changed controllers contributes nothing. The
     * subset of abilities is gated by the owner's tempt count (see [TheRingAbilities]).
     */
    private fun getRingBearerAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> {
        val bearer = state.getEntity(entityId)?.get<RingBearerComponent>() ?: return emptyList()
        // CR 701.52e: read the *projected* controller — a Ring-bearer stolen by a control-changing
        // effect is no longer "your Ring-bearer," so it stops contributing the emblem's abilities.
        if (state.projectedState.getController(entityId) != bearer.ownerId) return emptyList()
        val temptCount = state.getEntity(bearer.ownerId)?.get<TheRingComponent>()?.temptCount ?: return emptyList()
        return TheRingAbilities.abilitiesFor(temptCount)
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
     * Triggered abilities granted by Auras/Equipment attached to this entity.
     */
    private fun getAttachedGrantedTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> {
        val result = mutableListOf<TriggeredAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()?.targetId ?: continue
            if (attachedTo != entityId) continue

            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val sourceDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            val allStaticAbilities = sourceDef.script.effectiveStaticAbilities(classLevel)

            for (ability in allStaticAbilities) {
                when (ability) {
                    is GrantTriggeredAbility ->
                        if (ability.filter.scope is Scope.AttachedTo) result.add(ability.ability)

                    // "As long as enchanted permanent is X, it has '<triggered ability>'" —
                    // a conditional grant (e.g. Essence Leak). Only contribute the granted ability
                    // while the gating condition holds, evaluated with the Aura as the source so
                    // EnchantedPermanentMatches resolves the attached permanent.
                    is ConditionalStaticAbility -> {
                        val grant = ability.ability as? GrantTriggeredAbility ?: continue
                        if (grant.filter.scope !is Scope.AttachedTo) continue
                        val controllerId = state.projectedState.getController(permanentId) ?: continue
                        val context = EffectContext(
                            sourceId = permanentId,
                            controllerId = controllerId,
                            opponentId = null
                        )
                        if (ConditionEvaluator().evaluate(state, ability.condition, context)) {
                            result.add(grant.ability)
                        }
                    }

                    else -> {}
                }
            }
        }

        return result
    }

    /**
     * Generate ward triggered abilities for an entity.
     *
     * Checks two sources:
     * 1. Intrinsic ward from the card's keywordAbilities (KeywordAbility.Ward)
     * 2. Ward granted by GrantWard static abilities on other permanents
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

        val targetContainer = state.getEntity(entityId) ?: return result
        val targetCard = targetContainer.get<CardComponent>() ?: return result
        val projected = state.projectedState
        val targetControllerId = projected.getController(entityId)

        // Check suppression before generating any ward triggers — suppresses both intrinsic
        // and granted ward (Nowhere to Run: "Ward abilities of those creatures don't trigger.")
        if (isWardSuppressed(entityId, state, projected)) return result

        // 1. Intrinsic ward from card definition
        val cardDef = cardRegistry.getCard(cardDefinitionId)
        if (cardDef != null) {
            for (ka in cardDef.keywordAbilities) {
                if (ka is KeywordAbility.Ward) {
                    result.add(createWardTriggeredAbility(ka.cost, "intrinsic"))
                }
            }
        }

        // 2. Ward granted by battlefield-scoped GrantWard static abilities

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
                if (ability !is GrantWard) continue
                if (ability.filter.scope !is Scope.Battlefield) continue

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

                // Check state predicates
                val matchesState = filter.statePredicates.all { predicate ->
                    predicateEvaluator.matchesStatePredicate(state, entityId, predicate)
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

        // 3. Ward granted by attach-scoped GrantWard static abilities (Auras/Equipment)
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()?.targetId ?: continue
            if (attachedTo != entityId) continue

            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val sourceDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            val allStaticAbilities = sourceDef.script.effectiveStaticAbilities(classLevel)

            for (ability in allStaticAbilities) {
                val ward = when (ability) {
                    is GrantWard -> if (ability.filter.scope is Scope.AttachedTo) ability else continue
                    is ConditionalStaticAbility -> {
                        val conditionalWard = ability.ability as? GrantWard ?: continue
                        if (conditionalWard.filter.scope !is Scope.AttachedTo) continue
                        val controllerId = state.projectedState.getController(permanentId) ?: continue
                        val context = EffectContext(
                            sourceId = permanentId,
                            controllerId = controllerId,
                            opponentId = null
                        )
                        if (ConditionEvaluator().evaluate(state, ability.condition, context)) conditionalWard else continue
                    }
                    else -> continue
                }
                result.add(createWardTriggeredAbility(ward.cost, "attached_${permanentId.value}"))
            }
        }

        return result
    }

    /**
     * Triggered abilities contributed by *unlocked* faces of a Room permanent (CR 709.5).
     *
     * Locked halves are suppressed: their script's triggered abilities don't appear in the
     * permanent's effective ability set. As soon as a face is unlocked (via cast-time ETB
     * or the unlock special action), that face's abilities become active.
     *
     * Excludes "When you unlock this door" abilities ([GameEvent.DoorUnlockedEvent] triggers):
     * those are detected separately by [com.wingedsheep.engine.event.TriggerDetector.detectDoorUnlockedTriggers]
     * because the matcher is face-aware (only the unlocked face's "when you unlock this door"
     * fires, not other already-unlocked faces').
     */
    private fun getRoomFaceTriggeredAbilities(
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        state: GameState
    ): List<TriggeredAbility> {
        if (cardDef == null) return emptyList()
        val room = state.getEntity(entityId)?.get<RoomComponent>() ?: return emptyList()
        if (room.unlocked.isEmpty()) return emptyList()
        val result = mutableListOf<TriggeredAbility>()
        for (face in cardDef.cardFaces) {
            val faceId = com.wingedsheep.engine.state.components.identity.RoomFaceId(face.name)
            if (faceId !in room.unlocked) continue
            for (ability in face.script.effectiveTriggeredAbilities(null)) {
                if (ability.trigger is GameEvent.DoorUnlockedEvent) continue
                result.add(ability)
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

    /**
     * Returns true if any permanent on the battlefield suppresses ward triggers for [entityId]
     * (e.g. Nowhere to Run). The [GroupFilter] stored on [SuppressesWardForGroupComponent] is
     * evaluated with the suppressor's controller as the "you" context — for a filter like
     * `AllCreaturesOpponentsControl` that already restricts matches to creatures the
     * suppressor's opponents control, so no additional opponent check is needed here.
     */
    private fun isWardSuppressed(
        entityId: EntityId,
        state: GameState,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState
    ): Boolean {
        return state.getBattlefield().any { suppressorId ->
            val suppressorController = projected.getController(suppressorId) ?: return@any false
            val suppressComponent = state.getEntity(suppressorId)
                ?.get<SuppressesWardForGroupComponent>() ?: return@any false
            val ctx = PredicateContext(controllerId = suppressorController, sourceId = suppressorId)
            suppressComponent.filters.any { groupFilter ->
                when {
                    groupFilter.scope !is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> false
                    groupFilter.excludeSelf && entityId == suppressorId -> false
                    else -> predicateEvaluator.matches(
                        state, projected, entityId, groupFilter.baseFilter, ctx
                    )
                }
            }
        }
    }
}
