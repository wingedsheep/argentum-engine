package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ClassLevelChangedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Detects triggered abilities that should fire based on game events.
 *
 * Triggers are returned in APNAP (Active Player, Non-Active Player) order for
 * proper stack placement.
 *
 * Delegates to focused sub-detectors for specific trigger categories:
 * - [DeathAndLeaveTriggerDetector] — dies/leaves-battlefield triggers
 * - [DamageTriggerDetector] — damage source/received triggers
 * - [AttachmentTriggerDetector] — aura/equipment triggers
 */
class TriggerDetector(
    private val cardRegistry: CardRegistry,
    private val abilityRegistry: AbilityRegistry = AbilityRegistry(),
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator(),
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
) {

    private val matcher = TriggerMatcher(predicateEvaluator, conditionEvaluator)
    private val abilityResolver = TriggerAbilityResolver(cardRegistry, abilityRegistry)
    private val deathAndLeaveDetector = DeathAndLeaveTriggerDetector(abilityResolver, matcher)
    private val damageDetector = DamageTriggerDetector(abilityResolver, matcher)
    private val attachmentDetector = AttachmentTriggerDetector(matcher)

    /**
     * Build a trigger index for the current game state.
     *
     * Pre-scans all battlefield permanents once, categorizing each entity by the
     * engine event types its triggers respond to. This replaces O(N) battlefield
     * scans per event with O(1) lookups into pre-built buckets.
     *
     * Also pre-computes:
     * - Aura entities indexed by their attachment targets
     * - Grant providers (GrantTriggeredAbilityToCreatureGroup static abilities)
     * - Damage observer trigger lists for specialized detection methods
     */
    private fun buildTriggerIndex(state: GameState): TriggerIndex {
        val projected = state.projectedState

        // Phase 1: Collect grant providers (needed to compute abilities for each entity)
        val grantProviders = mutableListOf<TriggerIndex.GrantProviderEntry>()
        val registry = cardRegistry
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val sourceControllerId = projected.getController(permanentId) ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is GrantTriggeredAbilityToCreatureGroup) {
                    grantProviders.add(TriggerIndex.GrantProviderEntry(ability, sourceControllerId))
                }
            }
        }

        // Phase 2: Index each battlefield entity by trigger categories
        val categoryMap = HashMap<TriggerCategory, MutableList<TriggerIndex.IndexedEntity>>()
        val auraMap = HashMap<EntityId, MutableList<TriggerIndex.IndexedEntity>>()
        val damageToYou = mutableListOf<TriggerIndex.IndexedEntity>()
        val subtypeDmg = mutableListOf<TriggerIndex.IndexedEntity>()
        val damageObs = mutableListOf<TriggerIndex.IndexedEntity>()
        val deathTrackers = mutableListOf<TriggerIndex.IndexedEntity>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = projected.getController(entityId) ?: continue
            if (container.has<FaceDownComponent>()) continue

            val abilities = abilityResolver.getTriggeredAbilitiesWithProviders(
                entityId, cardComponent.cardDefinitionId, state, grantProviders
            )
            if (abilities.isEmpty() && container.get<AttachedToComponent>() == null) continue

            val entry = TriggerIndex.IndexedEntity(entityId, cardComponent, controllerId, abilities)

            // Categorize by event types this entity's triggers respond to
            val entityCategories = mutableSetOf<TriggerCategory>()
            for (ability in abilities) {
                if (ability.activeZone == Zone.BATTLEFIELD) {
                    entityCategories.addAll(TriggerIndex.triggerToCategories(ability.trigger, ability.binding))

                    // Index damage observer triggers
                    val trigger = ability.trigger
                    if (trigger is GameEvent.DealsDamageEvent && ability.binding == TriggerBinding.ANY) {
                        if (trigger.recipient == RecipientFilter.You && trigger.sourceFilter == null) {
                            damageToYou.add(entry)
                        } else if (trigger.damageType == DamageType.Combat &&
                            trigger.recipient == RecipientFilter.AnyPlayer &&
                            trigger.sourceFilter != null &&
                            trigger.sourceFilter is GameObjectFilter &&
                            (trigger.sourceFilter as GameObjectFilter).cardPredicates.any {
                                it is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype
                            }
                        ) {
                            subtypeDmg.add(entry)
                        } else {
                            // General damage observer (e.g., Kazarov, Gossip's Talent level 3)
                            damageObs.add(entry)
                        }
                    }

                    // Index creature-dealt-damage-dies triggers
                    if (trigger is GameEvent.CreatureDealtDamageBySourceDiesEvent) {
                        deathTrackers.add(entry)
                    }
                }
            }
            for (cat in entityCategories) {
                categoryMap.getOrPut(cat) { mutableListOf() }.add(entry)
            }

            // Index auras by attachment target
            val attachedTo = container.get<AttachedToComponent>()?.targetId
            if (attachedTo != null) {
                auraMap.getOrPut(attachedTo) { mutableListOf() }.add(entry)
            }
        }

        return TriggerIndex(
            byCategory = categoryMap,
            aurasByTarget = auraMap,
            grantProviders = grantProviders,
            damageToYouObservers = damageToYou,
            subtypeDamageObservers = subtypeDmg,
            damageObservers = damageObs,
            creatureDamageDeathTrackers = deathTrackers,
        )
    }

    /**
     * Detect all triggers that should fire based on the given events.
     *
     * @param state The current game state
     * @param events The events that occurred
     * @return List of pending triggers in APNAP order
     */
    fun detectTriggers(
        state: GameState,
        events: List<EngineGameEvent>
    ): List<PendingTrigger> {
        val index = buildTriggerIndex(state)
        val triggers = mutableListOf<PendingTrigger>()

        for (event in events) {
            triggers.addAll(detectTriggersForEvent(state, event, index))
        }

        // Rule 603.10: "Look back in time" for simultaneous deaths.
        // When multiple creatures die at the same time (e.g., from Infest),
        // each creature's death triggers should still see the others dying.
        // The main loop in detectTriggersForEvent only checks battlefield creatures,
        // so dead creatures miss each other's death events. Fix that here.
        deathAndLeaveDetector.detectSimultaneousDeathTriggers(state, events, triggers)

        // Detect "whenever one or more cards are put into your graveyard from your library"
        // batching triggers (e.g., Sidisi, Brood Tyrant). Groups library→graveyard zone changes
        // and fires the trigger at most once per controller.
        detectLibraryToGraveyardBatchTriggers(state, events, triggers, index)

        // Detect "whenever you sacrifice one or more [permanents]" batching triggers
        // (e.g., Camellia, the Seedmiser). Groups sacrifice events per controller
        // and fires the trigger at most once per controller.
        detectSacrificeBatchTriggers(state, events, triggers, index)

        // Detect "whenever one or more [creatures] you control deal combat damage to a player"
        // batching triggers (e.g., Kastral, the Windcrested). Groups combat damage events
        // and fires the trigger at most once per observer.
        detectCombatDamageBatchTriggers(state, events, triggers, state.projectedState, index)

        // Detect "whenever one or more creatures you control leave the battlefield without dying"
        // batching triggers (e.g., Dour Port-Mage). Groups zone changes from battlefield to
        // non-graveyard zones and fires the trigger at most once per observer.
        detectLeaveBattlefieldWithoutDyingBatchTriggers(state, events, triggers, index)

        // Detect "whenever one or more [filtered] permanents you control enter the battlefield"
        // batching triggers (e.g., Builder's Talent). Groups zone changes to battlefield
        // and fires the trigger at most once per observer.
        detectPermanentsEnteredBatchTriggers(state, events, triggers, index)

        // Detect "When this Class becomes level N" triggers.
        // Class level-up events fire ETB triggers from the newly gained class level.
        detectClassLevelUpTriggers(state, events, triggers)

        // Detect Saga chapter triggers from lore counter additions
        detectSagaChapterTriggers(state, events, triggers)

        // Duplicate triggers for "additional time" static abilities (e.g., Naban, Panharmonicon).
        // When a creature matching the filter ETBs, triggered abilities on the controller's
        // permanents that fired from that event trigger an additional time per copy.
        duplicateETBTriggers(state, events, triggers)

        // Detect event-based delayed triggers (e.g. Long River Lurker's
        // "whenever that creature deals combat damage this turn, you may exile it").
        detectEventBasedDelayedTriggers(state, events, triggers)

        // Detect evoke sacrifice triggers: when a permanent enters with EvokedComponent,
        // create a "sacrifice self" delayed trigger (CR 702.74)
        detectEvokeSacrificeTriggers(state, events, triggers)

        // Filter out once-per-turn triggers that have already fired this turn
        val filteredTriggers = triggers.filter { trigger ->
            if (!trigger.ability.oncePerTurn) return@filter true
            val entity = state.getEntity(trigger.sourceId)
            val tracker = entity?.get<TriggeredAbilityFiredThisTurnComponent>()
            tracker == null || !tracker.hasFired(trigger.ability.id)
        }

        // Rule 603.4: Filter out triggers with unmet intervening-if conditions
        return matcher.sortByApnapOrder(state, matcher.filterByTriggerCondition(state, filteredTriggers))
    }

    /**
     * Detect delayed triggers that should fire at the given step.
     * Returns the pending triggers and the IDs of consumed delayed triggers.
     */
    fun detectDelayedTriggers(state: GameState, step: Step): Pair<List<PendingTrigger>, Set<String>> {
        val activePlayer = state.activePlayerId
        val matching = state.delayedTriggers.filter { delayed ->
            delayed.trigger == null &&
                delayed.fireAtStep == step &&
                (!delayed.fireOnlyOnControllersTurn || delayed.controllerId == activePlayer) &&
                (delayed.notBeforeTurn == null || state.turnNumber >= delayed.notBeforeTurn)
        }
        if (matching.isEmpty()) return emptyList<PendingTrigger>() to emptySet()

        val triggers = matching.map { delayed ->
            PendingTrigger(
                ability = TriggeredAbility.create(
                    trigger = GameEvent.StepEvent(Step.END, Player.Each),
                    binding = TriggerBinding.ANY,
                    effect = delayed.effect
                ),
                sourceId = delayed.sourceId,
                sourceName = delayed.sourceName,
                controllerId = delayed.controllerId,
                triggerContext = TriggerContext(step = step)
            )
        }
        val consumedIds = matching.map { it.id }.toSet()
        return matcher.sortByApnapOrder(state, triggers) to consumedIds
    }

    /**
     * Detect triggers for phase/step changes.
     * Uses trigger index to only check entities with step-based triggers.
     */
    fun detectPhaseStepTriggers(
        state: GameState,
        step: Step,
        activePlayerId: EntityId
    ): List<PendingTrigger> {
        val index = buildTriggerIndex(state)
        val triggers = mutableListOf<PendingTrigger>()
        val projected = state.projectedState

        // Check battlefield entities with StepEvent triggers
        for (entry in index.getEntitiesForCategory(TriggerCategory.STEP)) {
            for (ability in entry.abilities) {
                if (ability.activeZone != Zone.BATTLEFIELD) continue
                if (matcher.matchesStepTrigger(ability.trigger, step, entry.controllerId, activePlayerId)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext(step = step, triggeringEntityId = activePlayerId)
                        )
                    )
                }
            }
        }

        // Check ATTACHED step triggers on auras (e.g., Custody Battle, Lingering Death)
        // For ATTACHED + StepEvent(Player.You), "you" = the attached creature's controller
        for ((targetId, attachments) in index.aurasByTarget) {
            val enchantedController = projected.getController(targetId) ?: continue
            for (entry in attachments) {
                for (ability in entry.abilities) {
                    if (ability.binding != TriggerBinding.ATTACHED) continue
                    if (ability.activeZone != Zone.BATTLEFIELD) continue
                    val trigger = ability.trigger as? GameEvent.StepEvent ?: continue
                    if (step != trigger.step) continue
                    if (matcher.matchesPlayerForStep(trigger.player, enchantedController, activePlayerId)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entry.entityId,
                                sourceName = entry.cardComponent.name,
                                controllerId = enchantedController,
                                triggerContext = TriggerContext(step = step, triggeringEntityId = activePlayerId)
                            )
                        )
                    }
                }
            }
        }

        // Check graveyard cards for step-based triggers with activeZone == GRAVEYARD
        for (playerId in state.turnOrder) {
            for (entityId in state.getGraveyard(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

                for (ability in abilities) {
                    if (ability.activeZone != Zone.GRAVEYARD) continue
                    // Use the card's owner as controller (graveyard cards are owned, not controlled)
                    val ownerId = cardComponent.ownerId
                        ?: container.get<OwnerComponent>()?.playerId
                        ?: continue
                    if (matcher.matchesStepTrigger(ability.trigger, step, ownerId, activePlayerId)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = ownerId,
                                triggerContext = TriggerContext(step = step, triggeringEntityId = activePlayerId)
                            )
                        )
                    }
                }
            }
        }

        // Check exiled cards for step-based triggers with activeZone == EXILE
        for (playerId in state.turnOrder) {
            for (entityId in state.getExile(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

                for (ability in abilities) {
                    if (ability.activeZone != Zone.EXILE) continue
                    val ownerId = cardComponent.ownerId
                        ?: container.get<OwnerComponent>()?.playerId
                        ?: continue
                    if (matcher.matchesStepTrigger(ability.trigger, step, ownerId, activePlayerId)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = ownerId,
                                triggerContext = TriggerContext(step = step, triggeringEntityId = activePlayerId)
                            )
                        )
                    }
                }
            }
        }

        // Check global granted triggered abilities for step-based triggers
        // (e.g., Dimensional Breach creates a permanent global upkeep trigger)
        for (global in state.globalGrantedTriggeredAbilities) {
            val ability = global.ability
            if (matcher.matchesStepTrigger(ability.trigger, step, global.controllerId, activePlayerId)) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = global.sourceId,
                        sourceName = global.sourceName,
                        controllerId = global.controllerId,
                        triggerContext = TriggerContext(step = step, triggeringEntityId = activePlayerId)
                    )
                )
            }
        }

        // Filter out once-per-turn triggers that have already fired this turn
        val filteredTriggers = triggers.filter { trigger ->
            if (!trigger.ability.oncePerTurn) return@filter true
            val entity = state.getEntity(trigger.sourceId)
            val tracker = entity?.get<TriggeredAbilityFiredThisTurnComponent>()
            tracker == null || !tracker.hasFired(trigger.ability.id)
        }

        // Rule 603.4: Filter out triggers with unmet intervening-if conditions
        return matcher.sortByApnapOrder(state, matcher.filterByTriggerCondition(state, filteredTriggers))
    }

    /**
     * Detect event-based delayed triggered abilities scoped to a watched entity.
     *
     * Unlike step-based delayed triggers, event-based ones are NOT consumed on fire —
     * they may fire multiple times before expiry (e.g., double strike combat damage).
     * They are removed only by their [DelayedTriggerExpiry] (e.g., end of turn cleanup).
     */
    /**
     * When a permanent enters the battlefield with EvokedComponent, create a "sacrifice self"
     * trigger that goes on the stack as a separate triggered ability (CR 702.74).
     * The ETB triggers and the sacrifice trigger are siblings on the stack — the player
     * can respond between them.
     */
    private fun detectEvokeSacrificeTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val etbEvents = events.filterIsInstance<ZoneChangeEvent>().filter { it.toZone == Zone.BATTLEFIELD }
        for (event in etbEvents) {
            val entity = state.getEntity(event.entityId) ?: continue
            if (!entity.has<com.wingedsheep.engine.state.components.battlefield.EvokedComponent>()) continue
            val cardComponent = entity.get<CardComponent>() ?: continue
            val controllerId = entity.get<ControllerComponent>()?.playerId ?: event.ownerId

            // Create a sacrifice-self trigger
            val sacrificeAbility = TriggeredAbility.create(
                trigger = com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                binding = TriggerBinding.SELF,
                effect = com.wingedsheep.sdk.dsl.Effects.SacrificeTarget(
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.Self
                ),
                descriptionOverride = "Evoke — Sacrifice ${cardComponent.name}"
            )
            triggers.add(
                PendingTrigger(
                    ability = sacrificeAbility,
                    sourceId = event.entityId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(triggeringEntityId = event.entityId)
                )
            )
        }
    }

    private fun detectEventBasedDelayedTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val eventBased = state.delayedTriggers.filter { it.trigger != null }
        if (eventBased.isEmpty()) return

        for (event in events) {
            for (delayed in eventBased) {
                val spec = delayed.trigger ?: continue
                if (!matchesEventForWatchedEntity(spec, event, delayed.watchedEntityId, delayed.controllerId, state)) continue
                triggers.add(
                    PendingTrigger(
                        ability = TriggeredAbility.create(
                            trigger = spec.event,
                            binding = spec.binding,
                            effect = delayed.effect
                        ),
                        sourceId = delayed.sourceId,
                        sourceName = delayed.sourceName,
                        controllerId = delayed.controllerId,
                        triggerContext = TriggerContext.fromEvent(event).copy(
                            triggeringEntityId = delayed.watchedEntityId
                                ?: TriggerContext.fromEvent(event).triggeringEntityId
                        )
                    )
                )
            }
        }
    }

    /**
     * Match an event against a delayed-trigger TriggerSpec, scoped to a watched entity.
     * Currently supports DealsDamageEvent (the only category that needs this today).
     */
    private fun matchesEventForWatchedEntity(
        spec: com.wingedsheep.sdk.scripting.TriggerSpec,
        event: EngineGameEvent,
        watchedEntityId: EntityId?,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        val specEvent = spec.event
        return when (specEvent) {
            is com.wingedsheep.sdk.scripting.GameEvent.DealsDamageEvent -> {
                if (event !is com.wingedsheep.engine.core.DamageDealtEvent) return false
                if (watchedEntityId != null && event.sourceId != watchedEntityId) return false
                matcher.matchesDealsDamageTrigger(specEvent, event, state, controllerId)
            }
            else -> false
        }
    }

    private fun detectTriggersForEvent(
        state: GameState,
        event: EngineGameEvent,
        index: TriggerIndex
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val projected = state.projectedState

        // Check only battlefield permanents with triggers relevant to this event type
        for (entry in index.getEntitiesForEvent(event)) {
            val entityId = entry.entityId
            val cardComponent = entry.cardComponent
            val controllerId = entry.controllerId

            for (ability in entry.abilities) {
                if (ability.activeZone != Zone.BATTLEFIELD) continue
                if (matcher.matchesTrigger(ability.trigger, ability.binding, event, entityId, controllerId, state)) {
                    // For "whenever a creature attacks" (AttackEvent with ANY binding),
                    // create one trigger per attacking creature (Rule 603.2c)
                    if (ability.trigger is GameEvent.AttackEvent && ability.binding == TriggerBinding.ANY &&
                        event is AttackersDeclaredEvent) {
                        val attackFilter = (ability.trigger as GameEvent.AttackEvent).filter
                        for (attackerId in event.attackers) {
                            if (attackFilter != null) {
                                // Filtered trigger: match creature against filter (includes controller predicate)
                                if (predicateEvaluator.matchesWithProjection(
                                        state, projected, attackerId, attackFilter,
                                        PredicateContext(controllerId = controllerId, sourceId = entityId)
                                    )) {
                                    triggers.add(
                                        PendingTrigger(
                                            ability = ability,
                                            sourceId = entityId,
                                            sourceName = cardComponent.name,
                                            controllerId = controllerId,
                                            triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                        )
                                    )
                                }
                            } else {
                                // Unfiltered trigger: any creature attacking
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                    )
                                )
                            }
                        }
                    }
                    // For "whenever a creature you control becomes blocked" (BecomesBlockedEvent with ANY binding),
                    // create one trigger per blocked creature controlled by the ability's controller.
                    // If a filter is set (e.g., "whenever a Beast becomes blocked"), match any blocked
                    // creature matching the filter regardless of controller.
                    else if (ability.trigger is GameEvent.BecomesBlockedEvent && ability.binding == TriggerBinding.ANY &&
                        event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val trigger = ability.trigger as GameEvent.BecomesBlockedEvent
                        val creatureFilter = trigger.filter
                        val blockedAttackers = event.blockers.values.flatten().distinct()
                        for (attackerId in blockedAttackers) {
                            if (creatureFilter != null) {
                                // Filtered trigger: match any creature matching the filter (any controller)
                                if (predicateEvaluator.matchesWithProjection(state, projected, attackerId, creatureFilter, PredicateContext(controllerId = controllerId, sourceId = entityId))) {
                                    triggers.add(
                                        PendingTrigger(
                                            ability = ability,
                                            sourceId = entityId,
                                            sourceName = cardComponent.name,
                                            controllerId = controllerId,
                                            triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                        )
                                    )
                                }
                            } else {
                                // Unfiltered trigger: only creatures controlled by the ability's controller
                                val attackerController = projected.getController(attackerId)
                                if (attackerController == controllerId) {
                                    triggers.add(
                                        PendingTrigger(
                                            ability = ability,
                                            sourceId = entityId,
                                            sourceName = cardComponent.name,
                                            controllerId = controllerId,
                                            triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    // For "blocks or becomes blocked by [filter]" (BlocksOrBecomesBlockedByEvent with SELF binding),
                    // check both blocking and being-blocked relationships and create one trigger per matching partner.
                    else if (ability.trigger is GameEvent.BlocksOrBecomesBlockedByEvent &&
                        ability.binding == TriggerBinding.SELF && event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val trigger = ability.trigger as GameEvent.BlocksOrBecomesBlockedByEvent
                        val partners = mutableListOf<EntityId>()

                        // Case 1: Source creature is a blocker — its combat partners are the attackers it blocks
                        val blockedAttackerIds = event.blockers[entityId]
                        if (blockedAttackerIds != null) {
                            partners.addAll(blockedAttackerIds)
                        }

                        // Case 2: Source creature is an attacker — its combat partners are blockers blocking it
                        for ((blockerId, attackerIds) in event.blockers) {
                            if (attackerIds.contains(entityId)) {
                                partners.add(blockerId)
                            }
                        }

                        for (partnerId in partners.distinct()) {
                            val partnerFilter = trigger.partnerFilter
                            val matchesFilter = if (partnerFilter != null) {
                                predicateEvaluator.matchesWithProjection(
                                    state, projected, partnerId, partnerFilter,
                                    PredicateContext(controllerId = controllerId, sourceId = entityId)
                                )
                            } else {
                                true
                            }
                            if (matchesFilter) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = partnerId)
                                    )
                                )
                            }
                        }
                    } else {
                        // For abilities like Death Match where the triggered ability should be
                        // controlled by the triggering entity's controller, not the source's controller
                        val effectiveControllerId = if (ability.controlledByTriggeringEntityController) {
                            val triggeringEntityId = TriggerContext.fromEvent(event).triggeringEntityId
                            if (triggeringEntityId != null) {
                                projected.getController(triggeringEntityId) ?: controllerId
                            } else {
                                controllerId
                            }
                        } else {
                            controllerId
                        }

                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = effectiveControllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
            }
        }

        // Check graveyard cards for non-step triggers with activeZone == GRAVEYARD
        // (e.g., Dragon Shadow: "When a creature with MV 6+ enters, return this from graveyard")
        for (playerId in state.turnOrder) {
            for (entityId in state.getGraveyard(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

                for (ability in abilities) {
                    if (ability.activeZone != Zone.GRAVEYARD) continue
                    val ownerId = cardComponent.ownerId
                        ?: container.get<OwnerComponent>()?.playerId
                        ?: continue
                    if (matcher.matchesTrigger(ability.trigger, ability.binding, event, entityId, ownerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = ownerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
            }
        }

        // Check global granted triggered abilities (e.g., False Cure)
        detectGlobalGrantedTriggers(state, event, triggers)

        // Handle death triggers (source might not be on battlefield anymore)
        if (event is ZoneChangeEvent && event.toZone == Zone.GRAVEYARD &&
            event.fromZone == Zone.BATTLEFIELD) {
            deathAndLeaveDetector.detectDeathTriggers(state, event, triggers)
            // Handle "whenever a creature dealt damage by this creature this turn dies" triggers
            deathAndLeaveDetector.detectCreatureDealtDamageBySourceDiesTriggers(state, event, triggers, projected, index)
            // Handle ATTACHED zone-change triggers on auras that went to graveyard with their creature
            // (detected on the AURA's zone change event using lastKnownAttachedTo)
            deathAndLeaveDetector.detectDeadAuraAttachmentTriggers(state, event, triggers)
            // Handle persist (CR 702.79) — nontoken creature with persist dies with no -1/-1 counter
            deathAndLeaveDetector.detectPersistTriggers(state, event, triggers)
        }

        // Handle leaves-the-battlefield triggers (source is no longer on battlefield)
        if (event is ZoneChangeEvent && event.fromZone == Zone.BATTLEFIELD) {
            deathAndLeaveDetector.detectLeavesBattlefieldTriggers(state, event, triggers)
        }

        // Handle cycling triggers on the cycled card itself (e.g., Renewed Faith)
        if (event is CardCycledEvent) {
            detectCyclingCardTriggers(state, event, triggers)
        }

        // Handle damage-received triggers for creatures no longer on the battlefield
        // (e.g., Broodhatch Nantuko dies from combat damage but trigger still fires)
        if (event is DamageDealtEvent && event.targetId !in state.getBattlefield()) {
            damageDetector.detectDamageReceivedTriggers(state, event, triggers)
        }

        // Handle damage-source triggers
        if (event is DamageDealtEvent && event.sourceId != null) {
            damageDetector.detectDamageSourceTriggers(state, event, triggers, projected)
        }

        // Handle "whenever a creature/spell deals damage to this" triggers (e.g., Tephraderm)
        if (event is DamageDealtEvent && event.sourceId != null) {
            damageDetector.detectDamagedBySourceTriggers(state, event, triggers)
        }

        // Handle "whenever a creature deals damage to you" triggers (e.g., Aurification)
        if (event is DamageDealtEvent && event.sourceId != null && event.targetId in state.turnOrder) {
            damageDetector.detectDamageToControllerTriggers(state, event, triggers, projected, index)
        }

        // Handle general damage observer triggers (e.g., Kazarov: "whenever a creature an opponent controls is dealt damage")
        if (event is DamageDealtEvent) {
            damageDetector.detectDamageObserverTriggers(state, event, triggers, index)
        }

        // Handle "whenever a [subtype] deals combat damage to a player" triggers (e.g., Cabal Slaver)
        if (event is DamageDealtEvent && event.sourceId != null && event.isCombatDamage && event.targetId in state.turnOrder) {
            damageDetector.detectSubtypeDamageToPlayerTriggers(state, event, triggers, projected, index)
        }

        // Handle all ATTACHED-bound triggers on auras/equipment (e.g., Frozen Solid, One with Nature,
        // Guilty Conscience, Extra Arms, Heart-Piercer Bow, Fatal Mutation, Uncontrolled Infestation)
        attachmentDetector.detectAttachmentTriggers(state, event, triggers, index)

        // Handle "when you gain control of this from another player" triggers (e.g., Risky Move)
        if (event is ControlChangedEvent) {
            detectControlChangeTriggers(state, event, triggers)
        }

        // Handle NthSpellCast triggers on the spell currently being cast (e.g. Hearthborn
        // Battler cast as the second spell of the turn). The spell is on the stack, not the
        // battlefield, so the main index scan above skips it.
        if (event is SpellCastEvent) {
            detectSelfCastNthSpellTriggers(state, event, triggers)
        }

        return triggers
    }

    /**
     * Detect NthSpellCast triggers on the spell currently being cast.
     *
     * When a card like Hearthborn Battler is itself the Nth spell cast this turn, its
     * trigger ("whenever a player casts their second spell each turn") should fire even
     * though the card is on the stack rather than the battlefield. The trigger event is
     * the cast itself, and the ability travels with the spell onto the stack.
     */
    private fun detectSelfCastNthSpellTriggers(
        state: GameState,
        event: SpellCastEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.spellEntityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        if (container.has<FaceDownComponent>()) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.casterId

        for (ability in abilities) {
            if (ability.trigger !is GameEvent.NthSpellCastEvent) continue
            if (matcher.matchesTrigger(ability.trigger, ability.binding, event, entityId, controllerId, state)) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
                    )
                )
            }
        }
    }

    /**
     * Detect triggers from global granted triggered abilities (e.g., False Cure).
     * These are triggered abilities not attached to any permanent, created by
     * spell effects and stored in GameState.globalGrantedTriggeredAbilities.
     */
    private fun detectGlobalGrantedTriggers(
        state: GameState,
        event: EngineGameEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        if (state.globalGrantedTriggeredAbilities.isEmpty()) return

        for (global in state.globalGrantedTriggeredAbilities) {
            val ability = global.ability
            // Use a dummy sourceId for matchesTrigger (global abilities aren't attached to entities)
            if (matcher.matchesTrigger(ability.trigger, ability.binding, event, global.sourceId, global.controllerId, state)) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = global.sourceId,
                        sourceName = global.sourceName,
                        controllerId = global.controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
                    )
                )
            }
        }
    }

    /**
     * Detect cycling triggers on the card that was cycled.
     * Cards like Renewed Faith have "When you cycle this card, you may gain 2 life."
     * The card is now in the graveyard, but its cycling trigger still fires.
     */
    private fun detectCyclingCardTriggers(
        state: GameState,
        event: CardCycledEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.cardId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger is GameEvent.CycleEvent) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = event.playerId,
                        triggerContext = TriggerContext(triggeringPlayerId = event.playerId)
                    )
                )
            }
        }
    }

    /**
     * Detect "whenever one or more [filter] cards are put into your graveyard from your library"
     * batching triggers. Groups all library→graveyard zone changes by owner and fires matching
     * triggers at most once per controller, regardless of how many cards were moved.
     */
    private fun detectLibraryToGraveyardBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all library→graveyard zone change events, grouped by owner
        val libToGravByOwner = mutableMapOf<EntityId, MutableList<ZoneChangeEvent>>()
        for (event in events) {
            if (event is ZoneChangeEvent && event.fromZone == Zone.LIBRARY && event.toZone == Zone.GRAVEYARD) {
                libToGravByOwner.getOrPut(event.ownerId) { mutableListOf() }.add(event)
            }
        }
        if (libToGravByOwner.isEmpty()) return

        // Check battlefield permanents with library-to-graveyard batch triggers
        for (entry in index.getEntitiesForCategory(TriggerCategory.LIBRARY_TO_GRAVEYARD)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is GameEvent.CardsPutIntoGraveyardFromLibraryEvent) continue

                // This trigger watches the controller's own graveyard
                val controllerId = entry.controllerId
                val ownerEvents = libToGravByOwner[controllerId] ?: continue

                // Check if any of the milled cards match the filter
                val hasMatch = ownerEvents.any { event ->
                    if (trigger.filter == GameObjectFilter.Any) return@any true
                    val entity = state.getEntity(event.entityId)
                    val cardComponent = entity?.get<CardComponent>()
                    if (cardComponent != null) {
                        trigger.filter.cardPredicates.all { predicate ->
                            when (predicate) {
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                    cardComponent.typeLine.isCreature
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                    cardComponent.typeLine.hasSubtype(predicate.subtype)
                                else -> true
                            }
                        }
                    } else false
                }

                if (hasMatch) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever you sacrifice one or more [permanents]" batching triggers.
     *
     * Groups PermanentsSacrificedEvent by controller and fires the trigger at most once
     * per controller, regardless of how many sacrifice events occurred.
     */
    private fun detectSacrificeBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all sacrifice events, grouped by controller
        val sacrificeByController = mutableMapOf<EntityId, MutableList<PermanentsSacrificedEvent>>()
        for (event in events) {
            if (event is PermanentsSacrificedEvent) {
                sacrificeByController.getOrPut(event.playerId) { mutableListOf() }.add(event)
            }
        }
        if (sacrificeByController.isEmpty()) return

        // Check battlefield permanents with sacrifice batch triggers
        for (entry in index.getEntitiesForCategory(TriggerCategory.SACRIFICE)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is GameEvent.PermanentsSacrificedEvent) continue

                // This trigger watches the controller's own sacrifices
                val controllerId = entry.controllerId
                val controllerEvents = sacrificeByController[controllerId] ?: continue

                // Check if any of the sacrificed permanents match the filter
                val hasMatch = controllerEvents.any { event ->
                    event.permanentIds.any { permanentId ->
                        if (trigger.filter == GameObjectFilter.Any) return@any true
                        // Look up the entity (should be in graveyard or still accessible)
                        val entity = state.getEntity(permanentId)
                        val cardComponent = entity?.get<CardComponent>()
                        if (cardComponent != null) {
                            trigger.filter.cardPredicates.all { predicate ->
                                when (predicate) {
                                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                        cardComponent.typeLine.isCreature
                                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact ->
                                        cardComponent.typeLine.isArtifact
                                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                        cardComponent.typeLine.hasSubtype(predicate.subtype)
                                    else -> true
                                }
                            }
                        } else false
                    }
                }

                if (hasMatch) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever one or more [creatures] you control deal combat damage to a player"
     * batching triggers. Groups all combat damage-to-player events and fires matching
     * triggers at most once per observer, regardless of how many creatures connected.
     */
    private fun detectCombatDamageBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        // Collect all combat damage-to-player events, grouped by the controller of the damage source
        data class CombatDamageInfo(val sourceId: EntityId, val targetPlayerId: EntityId)
        val combatDamageByController = mutableMapOf<EntityId, MutableList<CombatDamageInfo>>()
        for (event in events) {
            if (event is DamageDealtEvent && event.isCombatDamage && event.sourceId != null &&
                event.targetId in state.turnOrder) {
                val sourceContainer = state.getEntity(event.sourceId) ?: continue
                val controller = sourceContainer.get<ControllerComponent>()?.playerId ?: continue
                combatDamageByController.getOrPut(controller) { mutableListOf() }
                    .add(CombatDamageInfo(event.sourceId, event.targetId))
            }
        }
        if (combatDamageByController.isEmpty()) return

        for (entry in index.getEntitiesForCategory(TriggerCategory.COMBAT_DAMAGE_BATCH)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent) continue

                val controllerId = entry.controllerId
                val damageEvents = combatDamageByController[controllerId] ?: continue

                // Check if any damage source matches the sourceFilter (using projected state for subtypes)
                val hasMatch = damageEvents.any { info ->
                    val sourceContainer = state.getEntity(info.sourceId) ?: return@any false
                    val sourceCard = sourceContainer.get<CardComponent>() ?: return@any false
                    if (!sourceCard.typeLine.isCreature) return@any false
                    if (sourceContainer.has<FaceDownComponent>()) return@any false

                    // Check card predicates from the sourceFilter
                    trigger.sourceFilter.cardPredicates.all { predicate ->
                        when (predicate) {
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> true
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                projected.hasSubtype(info.sourceId, predicate.subtype.value)
                            else -> true
                        }
                    }
                }

                if (hasMatch) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever one or more creatures you control leave the battlefield without dying"
     * batching triggers. Groups zone changes from battlefield to non-graveyard zones and fires
     * matching triggers at most once per observer, regardless of how many creatures left.
     */
    private fun detectLeaveBattlefieldWithoutDyingBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all zone changes from battlefield to non-graveyard zones, grouped by owner
        // (owner approximates controller; the entity has already left the battlefield)
        data class LeaveInfo(val entityId: EntityId, val cardComponent: CardComponent)
        val leavesByController = mutableMapOf<EntityId, MutableList<LeaveInfo>>()
        for (event in events) {
            if (event is ZoneChangeEvent && event.fromZone == Zone.BATTLEFIELD &&
                event.toZone != Zone.GRAVEYARD) {
                val entity = state.getEntity(event.entityId) ?: continue
                val card = entity.get<CardComponent>() ?: continue
                if (!card.typeLine.isCreature) continue
                leavesByController.getOrPut(event.ownerId) { mutableListOf() }
                    .add(LeaveInfo(event.entityId, card))
            }
        }
        if (leavesByController.isEmpty()) return

        for (entry in index.getEntitiesForCategory(TriggerCategory.LEAVE_WITHOUT_DYING)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is GameEvent.LeaveBattlefieldWithoutDyingEvent) continue

                val controllerId = entry.controllerId
                val controllerLeaves = leavesByController[controllerId] ?: continue

                // Filter out self if excludeSelf is set
                val relevantLeaves = if (trigger.excludeSelf) {
                    controllerLeaves.filter { it.entityId != entry.entityId }
                } else {
                    controllerLeaves
                }
                if (relevantLeaves.isEmpty()) continue

                // Check if any leaving creature matches the filter
                val hasMatch = relevantLeaves.any { info ->
                    trigger.filter.cardPredicates.all { predicate ->
                        when (predicate) {
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                info.cardComponent.typeLine.isCreature
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                info.cardComponent.typeLine.hasSubtype(predicate.subtype)
                            else -> true
                        }
                    }
                }

                if (hasMatch) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever one or more [filtered] permanents you control enter the battlefield"
     * batching triggers.
     *
     * Groups ZoneChangeEvent with toZone == BATTLEFIELD by controller and fires the trigger
     * at most once per observer, regardless of how many permanents entered.
     */
    private fun detectPermanentsEnteredBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all zone changes to battlefield, grouped by controller
        data class EnterInfo(val entityId: EntityId, val cardComponent: CardComponent, val isToken: Boolean)
        val entersByController = mutableMapOf<EntityId, MutableList<EnterInfo>>()
        for (event in events) {
            if (event is ZoneChangeEvent && event.toZone == Zone.BATTLEFIELD) {
                val entity = state.getEntity(event.entityId) ?: continue
                val card = entity.get<CardComponent>() ?: continue
                val isToken = entity.has<TokenComponent>()
                val controllerId = entity.get<ControllerComponent>()?.playerId ?: event.ownerId
                entersByController.getOrPut(controllerId) { mutableListOf() }
                    .add(EnterInfo(event.entityId, card, isToken))
            }
        }
        if (entersByController.isEmpty()) return

        for (entry in index.getEntitiesForCategory(TriggerCategory.PERMANENTS_ENTERED_BATCH)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is GameEvent.PermanentsEnteredEvent) continue

                val controllerId = entry.controllerId
                val controllerEnters = entersByController[controllerId] ?: continue

                // Check if any entering permanent matches the filter
                val hasMatch = controllerEnters.any { info ->
                    trigger.filter.cardPredicates.all { predicate ->
                        when (predicate) {
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> info.cardComponent.typeLine.isCreature
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature -> !info.cardComponent.typeLine.isCreature
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !info.cardComponent.typeLine.isLand
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent -> info.cardComponent.typeLine.isPermanent
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype -> info.cardComponent.typeLine.hasSubtype(predicate.subtype)
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> info.cardComponent.typeLine.isArtifact
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment -> info.cardComponent.typeLine.isEnchantment
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken -> info.isToken
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken -> !info.isToken
                            else -> true
                        }
                    }
                }

                if (hasMatch) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "When this Class becomes level N" triggers.
     *
     * When a ClassLevelChangedEvent is emitted, checks if the newly gained class level
     * has any ETB triggers (trigger = ZoneChangeEvent(to = BATTLEFIELD) with SELF binding)
     * and fires them.
     */
    private fun detectClassLevelUpTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        for (event in events) {
            if (event !is ClassLevelChangedEvent) continue

            val entity = state.getEntity(event.entityId) ?: continue
            val card = entity.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Find the class level that was just gained
            val levelAbility = cardDef.classLevels.find { it.level == event.newLevel } ?: continue

            // Check for ETB triggers in the newly gained level
            for (ability in levelAbility.triggeredAbilities) {
                val trigger = ability.trigger
                // "When this Class becomes level N" is modeled as an ETB trigger
                if (trigger is GameEvent.ZoneChangeEvent &&
                    trigger.to == Zone.BATTLEFIELD &&
                    ability.binding == TriggerBinding.SELF
                ) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = event.entityId,
                            sourceName = card.name,
                            controllerId = event.controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect Saga chapter triggers from lore counter additions.
     *
     * When lore counters are added to a Saga, checks which chapter abilities should trigger.
     * The SagaComponent in the state has already been updated with the newly triggered chapters
     * (by TurnManager.addLoreCountersToSagas or StackResolver.enterPermanentOnBattlefield),
     * so we use the current lore count and the triggered chapters set to determine which
     * chapters to fire.
     */
    private fun detectSagaChapterTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val registry = cardRegistry

        // Find all LORE counter addition events
        val loreEvents = events.filterIsInstance<CountersAddedEvent>().filter { it.counterType == "LORE" }
        if (loreEvents.isEmpty()) return

        for (event in loreEvents) {
            val entityId = event.entityId
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val sagaComponent = container.get<SagaComponent>() ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val cardDef = registry.getCard(cardComponent.cardDefinitionId) ?: continue
            val sagaChapters = cardDef.sagaChapters
            if (sagaChapters.isEmpty()) continue

            val loreCount = counters.getCount(CounterType.LORE)
            // The previous lore count is current minus what was just added
            val previousLoreCount = loreCount - event.amount
            val controllerId = container.get<ControllerComponent>()?.playerId
                ?: cardComponent.ownerId ?: continue

            // Fire chapters that are newly reached by this counter addition
            for (chapter in sagaChapters.sortedBy { it.chapter }) {
                // Chapter triggers if lore count now >= chapter number
                // AND it wasn't already triggered before this counter addition
                if (loreCount >= chapter.chapter && previousLoreCount < chapter.chapter) {
                    // Create a triggered ability for this chapter
                    val chapterAbility = TriggeredAbility.create(
                        trigger = GameEvent.StepEvent(Step.PRECOMBAT_MAIN, Player.You),
                        binding = TriggerBinding.SELF,
                        effect = chapter.effect,
                        targetRequirement = chapter.targetRequirement,
                        additionalTargetRequirements = chapter.additionalTargetRequirements
                    )

                    triggers.add(
                        PendingTrigger(
                            ability = chapterAbility,
                            sourceId = entityId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    )
                }
            }
        }
    }

    /**
     * Duplicate ETB triggers for "additional time" static abilities (Naban, Panharmonicon).
     *
     * For each ZoneChangeEvent(to=BATTLEFIELD) in the events, checks if any permanent on
     * the battlefield has AdditionalETBTriggers whose creatureFilter matches the entering entity.
     * If so, duplicates all triggers that fired from that ETB event for the controller's permanents.
     *
     * Multiple copies are additive: N copies add N extra copies of each trigger.
     */
    private fun duplicateETBTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val registry = cardRegistry
        val projected = state.projectedState

        // Find ETB events (zone changes to battlefield)
        val etbEvents = events.filterIsInstance<ZoneChangeEvent>().filter { it.toZone == Zone.BATTLEFIELD }
        if (etbEvents.isEmpty()) return

        // Collect all AdditionalETBTriggers static abilities from battlefield permanents
        data class ETBDoubler(val controllerId: EntityId, val filter: GameObjectFilter, val sourceId: EntityId)
        val doublers = mutableListOf<ETBDoubler>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue

            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is AdditionalETBTriggers) {
                    doublers.add(ETBDoubler(controllerId, ability.creatureFilter, permanentId))
                }
            }
        }

        if (doublers.isEmpty()) return

        // For each ETB event, check if the entering creature matches any doubler
        val duplicates = mutableListOf<PendingTrigger>()

        for (etbEvent in etbEvents) {
            val enteringEntityId = etbEvent.entityId

            for (doubler in doublers) {
                // The entering creature must be controlled by the doubler's controller
                val enteringController = projected.getController(enteringEntityId) ?: etbEvent.ownerId
                if (enteringController != doubler.controllerId) continue

                // Check if the entering creature matches the filter
                if (doubler.filter != GameObjectFilter.Any) {
                    if (!predicateEvaluator.matchesWithProjection(
                            state, projected, enteringEntityId, doubler.filter,
                            PredicateContext(controllerId = doubler.controllerId, sourceId = doubler.sourceId)
                        )) continue
                }

                // Find all existing triggers that fired from this ETB event
                // and belong to permanents controlled by the doubler's controller
                for (trigger in triggers) {
                    if (trigger.triggerContext.triggeringEntityId != enteringEntityId) continue
                    if (trigger.controllerId != doubler.controllerId) continue

                    // Only duplicate triggers that are ETB-related (ZoneChangeEvent triggers)
                    val triggerEvent = trigger.ability.trigger
                    if (triggerEvent !is GameEvent.ZoneChangeEvent) continue
                    if (triggerEvent.to != Zone.BATTLEFIELD) continue

                    duplicates.add(trigger)
                }
            }
        }

        triggers.addAll(duplicates)
    }

    /**
     * Detect "when you gain control of this from another player" triggers.
     * Fires on the permanent whose control just changed, when the new controller
     * is different from the old controller.
     */
    private fun detectControlChangeTriggers(
        state: GameState,
        event: ControlChangedEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.permanentId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        // Only fire if control actually changed
        if (event.oldControllerId == event.newControllerId) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger is GameEvent.ControlChangeEvent && ability.binding == TriggerBinding.SELF) {
                // The new controller controls this triggered ability
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = event.newControllerId,
                        triggerContext = TriggerContext(
                            triggeringEntityId = entityId,
                            triggeringPlayerId = event.newControllerId
                        )
                    )
                )
            }
        }
    }
}
