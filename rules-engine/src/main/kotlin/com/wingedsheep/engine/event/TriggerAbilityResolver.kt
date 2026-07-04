package com.wingedsheep.engine.event

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SuppressesWardForGroupComponent
import com.wingedsheep.engine.state.components.battlefield.ParadigmComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
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
import com.wingedsheep.sdk.scripting.EventPattern
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
import com.wingedsheep.sdk.scripting.predicates.evaluateWith

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
        // "This creature has '<triggered ability>' [as long as …]" — a Scope.Self GrantTriggeredAbility
        // on the permanent's own definition, optionally gated by a ConditionalStaticAbility.
        val selfGrantedAbilities =
            if (state.projectedState.hasLostAllAbilities(entityId)) emptyList()
            else getSelfGrantedTriggeredAbilities(entityId, state)

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWard
        val wardAbilities = getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        // Flanking (CR 702.25) is a keyword-derived triggered ability, synthesized for any
        // creature that has the keyword (intrinsic or granted).
        val flankingAbilities = getFlankingTriggeredAbilities(entityId, state)

        val ringBearerAbilities = getRingBearerAbilities(entityId, state)

        // A suspended card (CR 702.62) gains the owner's-upkeep countdown-and-cast ability
        // while it carries the marker. Component-driven, so it works for an arbitrary card
        // with no printed suspend (e.g. a spell exiled by Taigam, Master Opportunist).
        val suspendAbilities = getSuspendTriggeredAbilities(entityId, state)
        val paradigmAbilities = getParadigmTriggeredAbilities(entityId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + attachedGrantedAbilities +
            selfGrantedAbilities + wardAbilities + flankingAbilities + ringBearerAbilities +
            suspendAbilities + paradigmAbilities
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
     * Grant [com.wingedsheep.sdk.scripting.Suspend.countdownAbility] to any card carrying the
     * [SuspendedComponent] marker. The ability functions only in exile (`activeZone == EXILE`),
     * so it is inert anywhere else and harmless to return universally.
     */
    private fun getSuspendTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> =
        if (state.getEntity(entityId)?.has<SuspendedComponent>() == true) {
            listOf(com.wingedsheep.sdk.scripting.Suspend.countdownAbility)
        } else {
            emptyList()
        }

    /**
     * Grant [com.wingedsheep.sdk.scripting.Paradigm.recastAbility] to any card carrying the
     * [ParadigmComponent] marker. The ability functions only in exile (`activeZone == EXILE`), so
     * it is inert anywhere else and harmless to return universally.
     */
    private fun getParadigmTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> =
        if (state.getEntity(entityId)?.has<ParadigmComponent>() == true) {
            listOf(com.wingedsheep.sdk.scripting.Paradigm.recastAbility)
        } else {
            emptyList()
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
                val controllerMatch = filter.controllerPredicate?.evaluateWith { leaf ->
                    when (leaf) {
                        is ControllerPredicate.ControlledByYou -> targetControllerId == sourceControllerId
                        is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != sourceControllerId
                        else -> null // leaf kinds this fast path can't evaluate don't constrain
                    }
                } ?: true
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
        // "This creature has '<triggered ability>' [as long as …]" — a Scope.Self
        // GrantTriggeredAbility on the permanent's own definition (Fire Nation Cadets installs
        // firebendingAttackTrigger(2) this way while a Lesson is in the graveyard).
        val selfGrantedAbilities = if (hasLostAbilities) emptyList()
            else getSelfGrantedTriggeredAbilities(entityId, state)

        // Generate ward triggered abilities from intrinsic keyword abilities and GrantWard
        val wardAbilities = if (hasLostAbilities) emptyList()
            else getWardTriggeredAbilities(entityId, cardDefinitionId, state)

        // Flanking (CR 702.25) — keyword-derived triggered ability. Projected keyword check
        // already accounts for lost-all-abilities, but guard for symmetry with the other grants.
        val flankingAbilities = if (hasLostAbilities) emptyList()
            else getFlankingTriggeredAbilities(entityId, state)

        // The Ring emblem's abilities belong to the emblem (a player object), not the creature, so
        // they survive "loses all abilities" effects on the Ring-bearer (CR 701.54c).
        val ringBearerAbilities = getRingBearerAbilities(entityId, state)

        val suspendAbilities = getSuspendTriggeredAbilities(entityId, state)
        val paradigmAbilities = getParadigmTriggeredAbilities(entityId, state)

        val allGranted = grantedAbilities + staticGrantedAbilities + attachedGrantedAbilities +
            selfGrantedAbilities + wardAbilities + flankingAbilities + ringBearerAbilities +
            suspendAbilities + paradigmAbilities
        val combined = if (allGranted.isNotEmpty()) base + allGranted else base

        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        return if (textReplacement != null) {
            combined.map { it.applyTextReplacement(textReplacement) }
        } else {
            combined
        }
    }

    /**
     * The Ring emblem's cumulative triggered abilities (CR 701.54c) for [entityId] when it is a
     * player's Ring-bearer. "Is your Ring-bearer" requires the creature to be under that owner's
     * control (CR 701.54e), so a Ring-bearer that changed controllers contributes nothing. The
     * subset of abilities is gated by the owner's tempt count (see [TheRingAbilities]).
     */
    private fun getRingBearerAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> {
        val bearer = state.getEntity(entityId)?.get<RingBearerComponent>() ?: return emptyList()
        // CR 701.54e: read the *projected* controller — a Ring-bearer stolen by a control-changing
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
                // "Other creatures you control have …" — the granting permanent must not grant
                // the ability to itself. The slow path honors excludeSelf; the fast provider
                // path previously omitted it, so the source double-triggered (e.g. Bria,
                // Riptide Rogue got prowess twice).
                if (entry.grant.filter.excludeSelf && entityId == entry.sourceEntityId) continue
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
                val controllerMatch = filter.controllerPredicate?.evaluateWith { leaf ->
                    when (leaf) {
                        is ControllerPredicate.ControlledByYou -> targetControllerId == entry.sourceControllerId
                        is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != entry.sourceControllerId
                        else -> null // leaf kinds this fast path can't evaluate don't constrain
                    }
                } ?: true
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
     * Triggered abilities a permanent grants to *itself* through a [Scope.Self]
     * [GrantTriggeredAbility] static ability on its own definition — i.e. "this creature has
     * '<triggered ability>'". When the grant is wrapped in a [ConditionalStaticAbility]
     * ("… as long as <condition>"), the ability is contributed only while the gating condition
     * holds, evaluated with this permanent as the source. Fire Nation Cadets uses this to carry
     * [firebendingAttackTrigger] only while a Lesson card is in its controller's graveyard.
     *
     * Unlike [getStaticGrantedTriggeredAbilities] (battlefield-scoped lord/sliver grants) and
     * [getAttachedGrantedTriggeredAbilities] (Aura/Equipment grants), this reads the source's own
     * static abilities and toggles with the condition each time triggers are computed, so the grant
     * appears and disappears live as the condition changes.
     */
    private fun getSelfGrantedTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> {
        val container = state.getEntity(entityId) ?: return emptyList()
        if (container.has<FaceDownComponent>()) return emptyList()
        val card = container.get<CardComponent>() ?: return emptyList()
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return emptyList()
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val staticAbilities = cardDef.script.effectiveStaticAbilities(classLevel)
        if (staticAbilities.isEmpty()) return emptyList()

        val result = mutableListOf<TriggeredAbility>()
        for (ability in staticAbilities) {
            when (ability) {
                is GrantTriggeredAbility ->
                    if (ability.filter.scope is Scope.Self) result.add(ability.ability)

                is ConditionalStaticAbility -> {
                    val grant = ability.ability as? GrantTriggeredAbility ?: continue
                    if (grant.filter.scope !is Scope.Self) continue
                    val controllerId = state.projectedState.getController(entityId) ?: continue
                    val context = EffectContext(sourceId = entityId, controllerId = controllerId)
                    if (ConditionEvaluator().evaluate(state, ability.condition, context)) {
                        result.add(grant.ability)
                    }
                }

                // "This permanent has all activated and triggered abilities of the last chosen card
                // exiled with it" (Koh, the Face Stealer): the chosen card's triggered abilities fire
                // from this permanent, so it gains e.g. the chosen creature's attack/ETB/dies triggers.
                is com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard ->
                    if (ability.grantTriggered) {
                        result.addAll(
                            com.wingedsheep.engine.legalactions.utils.chosenLinkedExiledTriggeredAbilities(
                                state, entityId, cardRegistry
                            )
                        )
                    }

                else -> {}
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
                val controllerMatch = filter.controllerPredicate?.evaluateWith { leaf ->
                    when (leaf) {
                        is ControllerPredicate.ControlledByYou -> targetControllerId == sourceControllerId
                        is ControllerPredicate.ControlledByOpponent -> targetControllerId != null && targetControllerId != sourceControllerId
                        else -> null // leaf kinds this fast path can't evaluate don't constrain
                    }
                } ?: true
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
     * Excludes "When you unlock this door" abilities ([EventPattern.DoorUnlockedEvent] triggers):
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
                if (ability.trigger is EventPattern.DoorUnlockedEvent) continue
                result.add(ability)
            }
        }
        return result
    }

    /**
     * Flanking (CR 702.25b) as a keyword-derived triggered ability. Any creature that has
     * [Keyword.FLANKING] — intrinsically printed or granted — gets the synthesized
     * [com.wingedsheep.sdk.scripting.Flanking.blockedByNonFlankerTrigger], the same way ward and
     * suspend abilities are derived rather than authored per card. The projected keyword check
     * respects "loses all abilities" (the keyword is stripped in projection) and any effect that
     * grants flanking. The trigger's own "becomes blocked by a creature without flanking" filter
     * excludes flanking blockers (CR 702.25c).
     */
    private fun getFlankingTriggeredAbilities(entityId: EntityId, state: GameState): List<TriggeredAbility> =
        if (state.projectedState.hasKeyword(entityId, com.wingedsheep.sdk.core.Keyword.FLANKING)) {
            listOf(com.wingedsheep.sdk.scripting.Flanking.blockedByNonFlankerTrigger)
        } else {
            emptyList()
        }

    private fun createWardTriggeredAbility(cost: WardCost, source: String): TriggeredAbility {
        return TriggeredAbility(
            id = AbilityId("ward_$source"),
            trigger = EventPattern.BecomesTargetEvent(byOpponent = true),
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
