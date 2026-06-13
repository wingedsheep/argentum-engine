package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ClassLevelChangedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardPlottedEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DoorUnlockedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
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
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.decayed

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
     * - Grant providers (GrantTriggeredAbility static abilities)
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
                if (ability is GrantTriggeredAbility &&
                    ability.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield
                ) {
                    grantProviders.add(TriggerIndex.GrantProviderEntry(ability, sourceControllerId, permanentId))
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
                    if (trigger is EventPattern.DealsDamageEvent && ability.binding == TriggerBinding.ANY) {
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
                    if (trigger is EventPattern.CreatureDealtDamageBySourceDiesEvent) {
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

        for ((eventIndex, event) in events.withIndex()) {
            // DrawEvent firing counts are derived from CardsDrawnThisTurnComponent, which `state`
            // holds at its POST-batch value. When one execution emits several CardsDrawnEvents for
            // the same player, each event needs to know how many of those draws happened after it
            // so the matcher can reconstruct the count as of that event (exceptFirstInDrawStep
            // exemption boundary).
            val samePlayerDrawsLaterInBatch = if (event is CardsDrawnEvent) {
                events.subList(eventIndex + 1, events.size)
                    .filterIsInstance<CardsDrawnEvent>()
                    .filter { it.playerId == event.playerId }
                    .sumOf { it.count }
            } else 0
            triggers.addAll(detectTriggersForEvent(state, event, index, samePlayerDrawsLaterInBatch))
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

        // Detect "whenever one or more [filtered] cards are put into your graveyard from anywhere"
        // batching triggers (e.g., Moonshadow). Groups all to-graveyard zone changes by owner
        // and fires the trigger at most once per controller.
        detectAnyToGraveyardBatchTriggers(state, events, triggers, index)

        // Detect "whenever one or more cards leave your graveyard" batching triggers
        // (e.g., Attuned Hunter, Kishla Skimmer). Groups from-graveyard zone changes by the
        // owner of that graveyard and fires the trigger at most once per controller. The
        // "during your turn" restriction is a triggerCondition (Conditions.IsYourTurn) on the card.
        detectCardsLeftGraveyardBatchTriggers(state, events, triggers, index)

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

        // Detect "whenever one or more [other] creatures you control die" batching triggers
        // (e.g., Vengeful Townsfolk). Groups battlefield→graveyard zone changes by each dying
        // creature's last-known controller and fires the trigger at most once per controller —
        // so a board wipe that kills several of a player's creatures fires the trigger once,
        // not once per creature (the over-counting a per-creature death trigger would suffer).
        detectCreaturesDiedBatchTriggers(state, events, triggers, index)

        // Detect "whenever one or more [filtered] permanents you control enter the battlefield"
        // batching triggers (e.g., Builder's Talent). Groups zone changes to battlefield
        // and fires the trigger at most once per observer.
        detectPermanentsEnteredBatchTriggers(state, events, triggers, index)

        // Detect "When you unlock this door" triggers (CR 709.5h, DSK Rooms).
        // Face-aware: only the unlocked face's "When you unlock this door" abilities fire.
        detectDoorUnlockedTriggers(state, events, triggers)

        // Detect "When this Class becomes level N" triggers.
        // Class level-up events fire ETB triggers from the newly gained class level.
        detectClassLevelUpTriggers(state, events, triggers)

        // Detect Saga chapter triggers from lore counter additions
        detectSagaChapterTriggers(state, events, triggers)

        // Duplicate triggers for "additional time" static abilities (e.g., Naban, Panharmonicon,
        // Gandalf the White). When a permanent matching the filter enters or leaves the
        // battlefield (per the static's directions), triggered abilities on the controller's
        // permanents that fired from that event trigger an additional time per copy.
        duplicateETBOrLTBTriggers(state, events, triggers)

        // Duplicate triggers caused by a creature being declared as an attacker (Windcrag Siege's
        // Mardu mode). The attack-cause analogue of duplicateETBTriggers.
        duplicateAttackTriggers(state, events, triggers)

        // Duplicate triggers for "all triggers from a filtered source trigger again" static
        // abilities (e.g., Twinflame Travelers). For each pending trigger whose source matches
        // a doubler's filter, add a copy.
        duplicateSourceTriggers(state, triggers)

        // Detect event-based delayed triggers (e.g. Long River Lurker's
        // "whenever that creature deals combat damage this turn, you may exile it").
        detectEventBasedDelayedTriggers(state, events, triggers)

        // Detect evoke sacrifice triggers: when a permanent enters with EvokedComponent,
        // create a "sacrifice self" delayed trigger (CR 702.74)
        detectEvokeSacrificeTriggers(state, events, triggers)

        // Detect the Decayed counter's attack trigger (CR 702.147a): when a creature with a
        // decayed counter is declared as an attacker, it must be sacrificed at end of combat.
        detectDecayedCounterAttackTriggers(state, events, triggers)

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
                (delayed.fireOnPlayerId == null || delayed.fireOnPlayerId == activePlayer) &&
                (delayed.notBeforeTurn == null || state.turnNumber >= delayed.notBeforeTurn)
        }
        if (matching.isEmpty()) return emptyList<PendingTrigger>() to emptySet()

        val triggers = matching.map { delayed ->
            PendingTrigger(
                ability = TriggeredAbility.create(
                    trigger = EventPattern.StepEvent(Step.END, Player.Each),
                    binding = TriggerBinding.ANY,
                    effect = delayed.effect,
                    targetRequirement = delayed.targetRequirement
                ),
                sourceId = delayed.sourceId,
                sourceName = delayed.sourceName,
                controllerId = delayed.controllerId,
                triggerContext = TriggerContext(
                    step = step,
                    triggeringEntityId = delayed.fireOnPlayerId,
                    triggeringPlayerId = delayed.fireOnPlayerId
                )
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
                    val trigger = ability.trigger as? EventPattern.StepEvent ?: continue
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

        // Duplicate triggers for "all triggers from a filtered source trigger again" static
        // abilities (e.g., Twinflame Travelers) — also applies to phase/step triggers.
        duplicateSourceTriggers(state, triggers)

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
                trigger = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(to = Zone.BATTLEFIELD),
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

    /**
     * CR 702.147a — Decayed's triggered half, driven by a decayed counter rather than a printed
     * ability. When a creature with one or more decayed counters is declared as an attacker, fire
     * an attack-triggered ability that schedules a "sacrifice it at end of combat" delayed trigger
     * (via [CreateDelayedTriggerEffect], step [Step.END_COMBAT]) — the exact composition the
     * printed `card { decayed() }` helper uses. The "can't block" static half is handled in
     * projection ([StateProjector]).
     */
    private fun detectDecayedCounterAttackTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val declaredAttackers = events.filterIsInstance<AttackersDeclaredEvent>()
            .flatMap { it.attackers }
            .distinct()
        for (attackerId in declaredAttackers) {
            val entity = state.getEntity(attackerId) ?: continue
            val counters = entity.get<CountersComponent>() ?: continue
            if (counters.getCount(com.wingedsheep.sdk.core.CounterType.DECAYED) <= 0) continue
            val cardComponent = entity.get<CardComponent>() ?: continue
            val controllerId = entity.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                ?.playerId ?: continue

            val sacrificeAtEndOfCombat = TriggeredAbility.create(
                trigger = com.wingedsheep.sdk.scripting.EventPattern.AttackEvent(),
                binding = TriggerBinding.SELF,
                effect = com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect(
                    step = Step.END_COMBAT,
                    effect = com.wingedsheep.sdk.dsl.Effects.SacrificeTarget(
                        com.wingedsheep.sdk.scripting.targets.EffectTarget.Self
                    )
                ),
                descriptionOverride = "Decayed — When ${cardComponent.name} attacks, " +
                    "sacrifice it at end of combat."
            )
            triggers.add(
                PendingTrigger(
                    ability = sacrificeAtEndOfCombat,
                    sourceId = attackerId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(triggeringEntityId = attackerId)
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

        // A one-shot delayed trigger ([DelayedTriggeredAbility.fireOnce]) must fire at most
        // once even if several events in this batch match it — track which ids have already
        // produced a pending trigger this pass.
        val firedOnceIds = mutableSetOf<String>()
        for (event in events) {
            for (delayed in eventBased) {
                if (delayed.fireOnce && delayed.id in firedOnceIds) continue
                val spec = delayed.trigger ?: continue
                if (!matchesEventForWatchedEntity(spec, event, delayed.watchedEntityId, delayed.id, delayed.sourceId, delayed.controllerId, state)) continue
                if (delayed.fireOnce) firedOnceIds.add(delayed.id)
                triggers.add(
                    PendingTrigger(
                        ability = TriggeredAbility.create(
                            trigger = spec.event,
                            binding = spec.binding,
                            effect = delayed.effect,
                            targetRequirement = delayed.targetRequirement
                        ),
                        sourceId = delayed.sourceId,
                        sourceName = delayed.sourceName,
                        controllerId = delayed.controllerId,
                        triggerContext = TriggerContext.fromEvent(event).copy(
                            triggeringEntityId = delayed.watchedEntityId
                                ?: TriggerContext.fromEvent(event).triggeringEntityId
                        ),
                        consumesDelayedTriggerId = if (delayed.fireOnce) delayed.id else null
                    )
                )
            }
        }
    }

    /**
     * Match an event against a delayed-trigger TriggerSpec.
     *
     * Two scoping modes:
     *  - **Entity-scoped** (`watchedEntityId != null`): the trigger watches one concrete entity —
     *    DealsDamageEvent (scoped on damage source) and ZoneChangeEvent ("when *that* creature dies
     *    this turn"). The spec's GameObjectFilter is not applied; the watched entity is the scope.
     *  - **Filter-scoped** (`watchedEntityId == null`): the trigger watches a *group* described by
     *    the spec's GameObjectFilter + TriggerBinding — the attack-declaration events ("when you next
     *    attack this turn") and ZoneChangeEvent ("whenever a creature you control enters this turn").
     *    These delegate to the shared [TriggerMatcher] so they behave identically to battlefield
     *    triggers, honoring the filter (including its controller predicate) and binding.
     */
    private fun matchesEventForWatchedEntity(
        spec: com.wingedsheep.sdk.scripting.TriggerSpec,
        event: EngineGameEvent,
        watchedEntityId: EntityId?,
        delayedId: String,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        val specEvent = spec.event
        return when (specEvent) {
            // Attack-declaration triggers are player-/filter-scoped, not entity-scoped, so they
            // reuse the canonical matcher rather than the watched-entity narrowing above.
            // Powers "when you next attack this turn, …" (YouAttackEvent) and the general
            // "when a [filtered] creature next attacks" (AttackEvent).
            is com.wingedsheep.sdk.scripting.EventPattern.YouAttackEvent,
            is com.wingedsheep.sdk.scripting.EventPattern.AttackEvent ->
                matcher.matchesTrigger(specEvent, spec.binding, event, sourceId, controllerId, state)
            // Spell-cast delayed triggers ("whenever you cast a [filtered] spell this turn, …",
            // Rediscover the Way chapter III) are filter-scoped: delegate to the canonical
            // spell-cast matcher so the spell filter and casting-player predicate are honored.
            is com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent ->
                matcher.matchesTrigger(specEvent, spec.binding, event, sourceId, controllerId, state)
            is com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent -> {
                if (event !is com.wingedsheep.engine.core.DamageDealtEvent) return false
                if (watchedEntityId != null && event.sourceId != watchedEntityId) return false
                matcher.matchesDealsDamageTrigger(specEvent, event, state, controllerId)
            }
            // "When damage is prevented this way": fires only for this delayed trigger's own
            // shield, matched by the linkId echoed back on the DamagePreventedEvent.
            is com.wingedsheep.sdk.scripting.EventPattern.DamagePreventedEvent -> {
                event is com.wingedsheep.engine.core.DamagePreventedEvent && event.linkId == delayedId
            }
            is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent -> {
                if (event !is ZoneChangeEvent) return false
                if (watchedEntityId != null) {
                    // Entity-scoped: "when *that* creature dies/leaves this turn". The watched
                    // entity already narrows the trigger, so we only check the zone transition —
                    // the spec's GameObjectFilter does not apply.
                    if (event.entityId != watchedEntityId) return false
                    if (specEvent.from != null && event.fromZone != specEvent.from) return false
                    if (specEvent.to != null && event.toZone != specEvent.to) return false
                    if (specEvent.excludeTo != null && event.toZone == specEvent.excludeTo) return false
                    true
                } else {
                    // Filter-scoped: "whenever a creature you control enters this turn". There is no
                    // single watched entity, so the spec's GameObjectFilter and TriggerBinding are
                    // what scope the trigger. Delegate to the canonical zone-change matcher so it
                    // behaves identically to a battlefield-resident "whenever a [filtered] permanent
                    // enters/leaves" trigger (Thunder of Unity chapters II/III).
                    matcher.matchesZoneChangeTrigger(specEvent, spec.binding, event, sourceId, controllerId, state)
                }
            }
            else -> false
        }
    }

    private fun detectTriggersForEvent(
        state: GameState,
        event: EngineGameEvent,
        index: TriggerIndex,
        samePlayerDrawsLaterInBatch: Int = 0
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
                    if (ability.trigger is EventPattern.AttackEvent && ability.binding == TriggerBinding.ANY &&
                        event is AttackersDeclaredEvent) {
                        val attackFilter = (ability.trigger as EventPattern.AttackEvent).filter
                        for (attackerId in event.attackers) {
                            if (attackFilter != null) {
                                // Filtered trigger: match creature against filter (includes controller predicate)
                                if (predicateEvaluator.matches(
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
                    else if (ability.trigger is EventPattern.BecomesBlockedEvent && ability.binding == TriggerBinding.ANY &&
                        event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val trigger = ability.trigger as EventPattern.BecomesBlockedEvent
                        val creatureFilter = trigger.filter
                        val blockedAttackers = event.blockers.values.flatten().distinct()
                        for (attackerId in blockedAttackers) {
                            if (creatureFilter != null) {
                                // Filtered trigger: match any creature matching the filter (any controller)
                                if (predicateEvaluator.matches(state, projected, attackerId, creatureFilter, PredicateContext(controllerId = controllerId, sourceId = entityId))) {
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
                    // For "whenever a creature [you control] blocks" (BlockEvent with ANY binding),
                    // create one trigger per matching blocker.
                    else if (ability.trigger is EventPattern.BlockEvent && ability.binding == TriggerBinding.ANY &&
                        event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val blockFilter = (ability.trigger as EventPattern.BlockEvent).filter
                        for (blockerId in event.blockers.keys) {
                            if (blockFilter != null) {
                                if (predicateEvaluator.matches(
                                        state, projected, blockerId, blockFilter,
                                        PredicateContext(controllerId = controllerId, sourceId = entityId)
                                    )) {
                                    triggers.add(
                                        PendingTrigger(
                                            ability = ability,
                                            sourceId = entityId,
                                            sourceName = cardComponent.name,
                                            controllerId = controllerId,
                                            triggerContext = TriggerContext(triggeringEntityId = blockerId)
                                        )
                                    )
                                }
                            } else {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = blockerId)
                                    )
                                )
                            }
                        }
                    }
                    // For "whenever this creature blocks a [filter]" (BlockEvent with SELF binding +
                    // attackerFilter), create one trigger per blocked attacker matching the filter.
                    // triggeringEntityId = the blocked attacker. Skystinger pattern.
                    else if (ability.trigger is EventPattern.BlockEvent && ability.binding == TriggerBinding.SELF &&
                        (ability.trigger as EventPattern.BlockEvent).attackerFilter != null &&
                        event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val attackerFilter = (ability.trigger as EventPattern.BlockEvent).attackerFilter!!
                        val blockedAttackerIds = event.blockers[entityId] ?: emptyList()
                        for (attackerId in blockedAttackerIds) {
                            if (predicateEvaluator.matches(
                                    state, projected, attackerId, attackerFilter,
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
                        }
                    }
                    // "Whenever this creature becomes blocked[ by ...]" (BecomesBlockedEvent, SELF).
                    // Two shapes that differ in firing count:
                    //   - Unfiltered ("becomes blocked"): becoming blocked is a single event no
                    //     matter how many creatures block, so fire exactly ONCE with the source as
                    //     the triggering entity. This keeps blocker-count payoffs correct (Rampage:
                    //     +N/+N for each creature blocking it beyond the first) and avoids prompting
                    //     "may" abilities once per blocker (Gustcloak).
                    //   - Filtered ("becomes blocked by a creature matching [filter]"): fire once per
                    //     matching blocker, with triggeringEntityId = the blocker, so effects targeting
                    //     the triggering entity resolve to that blocker (Flanking gives each -1/-1).
                    else if (ability.trigger is EventPattern.BecomesBlockedEvent && ability.binding == TriggerBinding.SELF &&
                        event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val blockerFilter = (ability.trigger as EventPattern.BecomesBlockedEvent).filter
                        if (blockerFilter == null) {
                            val isBlocked = event.blockers.values.any { it.contains(entityId) }
                            if (isBlocked) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = entityId)
                                    )
                                )
                            }
                        } else {
                            for ((blockerId, attackerIds) in event.blockers) {
                                if (!attackerIds.contains(entityId)) continue
                                if (!predicateEvaluator.matches(
                                        state, projected, blockerId, blockerFilter,
                                        PredicateContext(controllerId = controllerId, sourceId = entityId)
                                    )
                                ) continue
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = blockerId)
                                    )
                                )
                            }
                        }
                    }
                    // For "blocks or becomes blocked by [filter]" (BlocksOrBecomesBlockedByEvent with SELF binding),
                    // check both blocking and being-blocked relationships and create one trigger per matching partner.
                    else if (ability.trigger is EventPattern.BlocksOrBecomesBlockedByEvent &&
                        ability.binding == TriggerBinding.SELF && event is com.wingedsheep.engine.core.BlockersDeclaredEvent) {
                        val trigger = ability.trigger as EventPattern.BlocksOrBecomesBlockedByEvent
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
                                predicateEvaluator.matches(
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
                    }
                    // For "whenever [a player] draws a card" (DrawEvent), drawing N cards via a
                    // single effect creates N separate trigger firings — one per card drawn
                    // (CR 121.2 + 603.2). The engine emits a single aggregate CardsDrawnEvent, so
                    // expand it here. `exceptFirstInDrawStep` (Orcish Bowmasters) subtracts the one
                    // card exempted by CR 504.1 — the matcher computes the final firing count.
                    else if (ability.trigger is EventPattern.DrawEvent && event is CardsDrawnEvent) {
                        val firings = matcher.drawTriggerFiringCount(
                            ability.trigger as EventPattern.DrawEvent, event, controllerId, state,
                            samePlayerDrawsLaterInBatch
                        )
                        repeat(firings) {
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
                    // Same shape for "whenever an opponent discards a card" — discarding N cards
                    // through one effect creates N separate trigger firings. A card filter narrows
                    // that to the matching cards, so defer to the matcher for the count.
                    else if (ability.trigger is EventPattern.DiscardEvent &&
                        event is CardsDiscardedEvent) {
                        val firings = matcher.matchingDiscardCount(
                            ability.trigger as EventPattern.DiscardEvent,
                            event,
                            entityId,
                            controllerId,
                            state
                        )
                        repeat(firings) {
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

                        // Lock in the attached creature's power for Aura/Equipment abilities that
                        // read "enchanted/equipped creature ... its power" (e.g. Pain for All). The
                        // creature — and the aura — may leave before the ability resolves (removed in
                        // response to the aura's ETB trigger), in which case the resolver falls back to
                        // this last-known value (CR 608.2h).
                        val enchantedPower = state.getEntity(entityId)
                            ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                            ?.targetId
                            ?.let { projected.getPower(it) }

                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = effectiveControllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                                    .copy(enchantedCreatureLastKnownPower = enchantedPower)
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

        // Handle "when this card becomes plotted" triggers on the plotted card itself, which
        // now sits face up in exile rather than on the battlefield (e.g., Aloe Alchemist).
        if (event is CardPlottedEvent) {
            detectPlottedCardTriggers(state, event, triggers)
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

        // Handle self-cast triggers on the spell currently being cast — both NthSpellCast
        // (e.g. Hearthborn Battler cast as the second spell of the turn) and "when you cast
        // this spell" cast triggers (e.g. Sage of the Skies). The spell is on the stack, not
        // the battlefield, so the main index scan above skips it.
        if (event is SpellCastEvent) {
            detectSelfCastTriggers(state, event, triggers)
        }

        return triggers
    }

    /**
     * Detect self-cast triggers on the spell currently being cast — triggers whose event keys off
     * the spell's own casting and travels with it onto the stack.
     *
     * Two kinds qualify:
     *  - [EventPattern.NthSpellCastEvent] — when a card like Hearthborn Battler is itself the Nth
     *    spell cast this turn ("whenever a player casts their second spell each turn").
     *  - [EventPattern.CastThisSpellEvent] — a "when you cast this spell" cast trigger (Sage of the
     *    Skies). These are never indexed against battlefield permanents, so this is the only path
     *    that fires them.
     *
     * The spell is on the stack rather than the battlefield, so the main index scan skips it; this
     * pass reads the cast spell's own triggered abilities and matches them against the cast event.
     */
    private fun detectSelfCastTriggers(
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
            if (ability.trigger !is EventPattern.NthSpellCastEvent &&
                ability.trigger !is EventPattern.CastThisSpellEvent
            ) continue
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
            if (ability.trigger is EventPattern.CycleEvent) {
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
     * Detect "when this card becomes plotted" triggers on the plotted card itself (e.g.,
     * Aloe Alchemist). After the plot special action resolves the card sits face up in exile,
     * not on the battlefield, so the main index scan never sees it. This mirrors
     * [detectCyclingCardTriggers]: read the plotted card's own triggered abilities and fire any
     * that key off [EventPattern.BecomesPlottedEvent].
     */
    private fun detectPlottedCardTriggers(
        state: GameState,
        event: CardPlottedEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.cardId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger is EventPattern.BecomesPlottedEvent) {
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
                if (trigger !is EventPattern.CardsPutIntoGraveyardFromLibraryEvent) continue

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
     * Detect "whenever one or more [filter] cards are put into your graveyard from anywhere"
     * batching triggers. Groups all to-graveyard zone changes by the owner of the moved card
     * and fires matching triggers at most once per controller, regardless of how many cards
     * were moved or from which source zones.
     */
    private fun detectAnyToGraveyardBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all zone change events targeting graveyard, grouped by owner
        val toGravByOwner = mutableMapOf<EntityId, MutableList<ZoneChangeEvent>>()
        for (event in events) {
            if (event is ZoneChangeEvent && event.toZone == Zone.GRAVEYARD && event.fromZone != Zone.GRAVEYARD) {
                toGravByOwner.getOrPut(event.ownerId) { mutableListOf() }.add(event)
            }
        }
        if (toGravByOwner.isEmpty()) return

        for (entry in index.getEntitiesForCategory(TriggerCategory.ANY_TO_GRAVEYARD)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is EventPattern.CardsPutIntoYourGraveyardEvent) continue

                val controllerId = entry.controllerId
                val ownerEvents = toGravByOwner[controllerId] ?: continue

                // Check if any of the moved cards match the filter
                val hasMatch = ownerEvents.any { event ->
                    cardMatchesGraveyardBatchFilter(state, event.entityId, trigger.filter)
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
     * Detect "whenever one or more cards leave your graveyard" batching triggers
     * (e.g., Attuned Hunter, Kishla Skimmer, Kheru Goldkeeper).
     *
     * Groups all from-graveyard zone changes by the owner of that graveyard ("your graveyard")
     * and fires the trigger at most once per controller, regardless of how many matching cards
     * left or where they went (cast/exiled/reanimated/returned to hand). The "during your turn"
     * restriction is expressed on the card as `triggerCondition = Conditions.IsYourTurn`, and
     * "only once each turn" via `oncePerTurn = true`, both applied later in detectTriggers.
     */
    private fun detectCardsLeftGraveyardBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect all zone change events leaving a graveyard, grouped by the graveyard's owner
        val fromGravByOwner = mutableMapOf<EntityId, MutableList<ZoneChangeEvent>>()
        for (event in events) {
            if (event is ZoneChangeEvent && event.fromZone == Zone.GRAVEYARD && event.toZone != Zone.GRAVEYARD) {
                fromGravByOwner.getOrPut(event.ownerId) { mutableListOf() }.add(event)
            }
        }
        if (fromGravByOwner.isEmpty()) return

        for (entry in index.getEntitiesForCategory(TriggerCategory.CARDS_LEFT_GRAVEYARD)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is EventPattern.CardsLeftYourGraveyardEvent) continue

                val controllerId = entry.controllerId
                val ownerEvents = fromGravByOwner[controllerId] ?: continue

                // Check if any of the departed cards match the filter
                val hasMatch = ownerEvents.any { event ->
                    cardMatchesGraveyardBatchFilter(state, event.entityId, trigger.filter)
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
     * Whether the card now identified by [entityId] (which may have moved to a new zone)
     * matches [filter] for graveyard batching triggers. Card-characteristic predicates are
     * evaluated against the card's base [CardComponent]; [GameObjectFilter.Any] always matches.
     */
    private fun cardMatchesGraveyardBatchFilter(
        state: GameState,
        entityId: EntityId,
        filter: GameObjectFilter
    ): Boolean {
        if (filter == GameObjectFilter.Any) return true
        val cardComponent = state.getEntity(entityId)?.get<CardComponent>() ?: return false
        return filter.cardPredicates.all { predicate ->
            when (predicate) {
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                    cardComponent.typeLine.isCreature
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact ->
                    cardComponent.typeLine.isArtifact
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment ->
                    cardComponent.typeLine.isEnchantment
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand ->
                    cardComponent.typeLine.isLand
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPlaneswalker ->
                    com.wingedsheep.sdk.core.CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent ->
                    cardComponent.typeLine.isPermanent
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland ->
                    !cardComponent.typeLine.isLand
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature ->
                    !cardComponent.typeLine.isCreature
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonartifact ->
                    !cardComponent.typeLine.isArtifact
                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                    cardComponent.typeLine.hasSubtype(predicate.subtype)
                else -> true
            }
        }
    }

    /**
     * Detect "whenever you sacrifice one or more [permanents]" batching triggers
     * (ANY binding) and "when you sacrifice this" self-targeted triggers (SELF binding).
     *
     * For ANY binding: groups PermanentsSacrificedEvent by controller and fires the
     * trigger at most once per controller, regardless of how many sacrifice events
     * occurred.
     *
     * For SELF binding: fires once per sacrificed permanent that has a matching trigger.
     * The source has already moved to the graveyard by the time detection runs, so it
     * is not in the trigger index — abilities are looked up directly by card definition.
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

        // ANY binding: check battlefield permanents with sacrifice batch triggers
        for (entry in index.getEntitiesForCategory(TriggerCategory.SACRIFICE)) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is EventPattern.PermanentsSacrificedEvent) continue
                if (ability.binding != TriggerBinding.ANY) continue

                // This trigger watches the controller's own sacrifices
                val controllerId = entry.controllerId
                val controllerEvents = sacrificeByController[controllerId] ?: continue

                // Check if any of the sacrificed permanents match the filter
                val hasMatch = controllerEvents.any { event ->
                    event.permanentIds.any { permanentId ->
                        sacrificedPermanentMatchesFilter(state, permanentId, trigger.filter)
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

        // SELF binding: source has left the battlefield, so it isn't in the index.
        // Look up triggered abilities directly from each sacrificed permanent.
        for ((controllerId, controllerEvents) in sacrificeByController) {
            for (event in controllerEvents) {
                for (permanentId in event.permanentIds) {
                    val container = state.getEntity(permanentId) ?: continue
                    if (container.has<FaceDownComponent>()) continue
                    val cardComponent = container.get<CardComponent>() ?: continue

                    val abilities = abilityResolver.getTriggeredAbilities(
                        permanentId, cardComponent.cardDefinitionId, state
                    )
                    for (ability in abilities) {
                        if (ability.binding != TriggerBinding.SELF) continue
                        val trigger = ability.trigger
                        if (trigger !is EventPattern.PermanentsSacrificedEvent) continue
                        if (ability.activeZone != Zone.BATTLEFIELD) continue
                        if (!sacrificedPermanentMatchesFilter(state, permanentId, trigger.filter)) continue

                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = permanentId,
                                sourceName = cardComponent.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun sacrificedPermanentMatchesFilter(
        state: GameState,
        permanentId: EntityId,
        filter: GameObjectFilter
    ): Boolean {
        if (filter == GameObjectFilter.Any) return true
        val entity = state.getEntity(permanentId) ?: return false
        val cardComponent = entity.get<CardComponent>() ?: return false
        return filter.cardPredicates.all { predicate ->
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
                if (trigger !is EventPattern.OneOrMoreDealCombatDamageToPlayerEvent) continue

                val controllerId = entry.controllerId
                val damageEvents = combatDamageByController[controllerId] ?: continue

                // Check if any damage source matches the sourceFilter. Damage events are already
                // grouped by source controller and matched to observers with the same controller,
                // so "you control" is satisfied; we still run the full filter via the canonical
                // evaluator so state predicates (e.g. +1/+1 counters) and any other card/controller
                // predicates are honored — not just the handful of card predicates handled inline.
                val firstMatchingInfo = damageEvents.firstOrNull { info ->
                    val sourceContainer = state.getEntity(info.sourceId) ?: return@firstOrNull false
                    sourceContainer.get<CardComponent>() ?: return@firstOrNull false
                    if (!projected.isCreature(info.sourceId)) return@firstOrNull false
                    if (sourceContainer.has<FaceDownComponent>()) return@firstOrNull false

                    predicateEvaluator.matches(
                        state,
                        projected,
                        info.sourceId,
                        trigger.sourceFilter,
                        PredicateContext(controllerId = controllerId, sourceId = entry.entityId)
                    )
                }

                if (firstMatchingInfo != null) {
                    // Batch trigger fires once regardless of how many sources dealt damage.
                    // triggeringEntityId is an arbitrary matching source (the first one we found);
                    // cards that need per-source dispatch must use a singular trigger event instead.
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext(triggeringEntityId = firstMatchingInfo.sourceId)
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
                if (trigger !is EventPattern.LeaveBattlefieldWithoutDyingEvent) continue

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

    private data class CreatureDeathInfo(
        val entityId: EntityId,
        val typeLine: com.wingedsheep.sdk.core.TypeLine?
    )

    /**
     * Detect "whenever one or more [other] creatures you control die" batching triggers
     * (e.g., Vengeful Townsfolk).
     *
     * Groups battlefield→graveyard zone changes by each dying creature's last-known controller
     * and fires the trigger at most once per qualifying controller, regardless of how many
     * creatures died simultaneously. This is the once-per-batch shape a per-creature death
     * trigger (one [ZoneChangeEvent] each) cannot express — a board wipe killing five of a
     * player's creatures fires this once, not five times.
     *
     * Controller is read from [ZoneChangeEvent.lastKnownController] (falling back to owner),
     * since the creature has already left the battlefield. The creature's type line comes from
     * [ZoneChangeEvent.lastKnownTypeLine] so it survives the 704.5s token cleanup that can remove
     * a dead token from the graveyard in the same pass.
     *
     * Two source populations are considered: permanents that survived the batch (taken from the
     * post-batch battlefield index) and — for Rule 603.10 "look back in time" — a source that
     * itself died in this same batch, recovered from its last-known card definition since it is no
     * longer on the battlefield (mirrors [DeathAndLeaveTriggerDetector.detectSimultaneousDeathTriggers]).
     */
    private fun detectCreaturesDiedBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        // Collect creature deaths (battlefield → graveyard), grouped by last-known controller.
        val deathsByController = mutableMapOf<EntityId, MutableList<CreatureDeathInfo>>()
        for (event in events) {
            if (event !is ZoneChangeEvent) continue
            if (event.fromZone != Zone.BATTLEFIELD || event.toZone != Zone.GRAVEYARD) continue
            val typeLine = event.lastKnownTypeLine
                ?: state.getEntity(event.entityId)?.get<CardComponent>()?.typeLine
            if (typeLine?.isCreature != true) continue
            val controllerId = event.lastKnownController ?: event.ownerId
            deathsByController.getOrPut(controllerId) { mutableListOf() }
                .add(CreatureDeathInfo(event.entityId, typeLine))
        }
        if (deathsByController.isEmpty()) return

        // (1) Sources that survived the batch — taken from the (post-batch) battlefield index.
        for (entry in index.getEntitiesForCategory(TriggerCategory.CREATURES_DIED_BATCH)) {
            for (ability in entry.abilities) {
                fireCreaturesDiedBatchTrigger(
                    ability = ability,
                    sourceId = entry.entityId,
                    sourceName = entry.cardComponent.name,
                    controllerId = entry.controllerId,
                    deathsByController = deathsByController,
                    triggers = triggers
                )
            }
        }

        // (2) Rule 603.10 "look back in time": a source that itself died in this same batch must
        // still see the *other* creatures that died alongside it. The index is built from the
        // current (post-batch) battlefield, so such a source is absent from it — recover its
        // abilities from its last-known card definition, exactly as detectSimultaneousDeathTriggers
        // does for the per-creature path. (For Vengeful Townsfolk the on-self +1/+1 is a harmless
        // no-op once it is in the graveyard, but a non-self payoff — draw a card, make a token,
        // deal damage — would otherwise be silently dropped on a board wipe that also kills it.)
        for (event in events) {
            if (event !is ZoneChangeEvent) continue
            if (event.fromZone != Zone.BATTLEFIELD || event.toZone != Zone.GRAVEYARD) continue
            // Still on the battlefield → already covered by the index path above.
            if (event.entityId in state.getBattlefield()) continue
            val container = state.getEntity(event.entityId)
            // Face-down permanents have no abilities (Rule 708.2).
            if (container?.has<FaceDownComponent>() == true) continue
            val cardDefId = container?.get<CardComponent>()?.cardDefinitionId
                ?: event.lastKnownCardDefinitionId ?: continue
            val controllerId = event.lastKnownController ?: event.ownerId
            val sourceName = container?.get<CardComponent>()?.name ?: event.entityName
            for (ability in abilityResolver.getTriggeredAbilities(event.entityId, cardDefId, state)) {
                if (ability.trigger !is EventPattern.CreaturesYouControlDiedEvent) continue
                fireCreaturesDiedBatchTrigger(
                    ability = ability,
                    sourceId = event.entityId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    deathsByController = deathsByController,
                    triggers = triggers
                )
            }
        }
    }

    /**
     * Evaluate a single "whenever one or more [filtered] creatures you control die" ability
     * against the batch of deaths grouped by controller, firing it at most once. Shared by the
     * surviving-source (battlefield index) and dead-source (Rule 603.10 look-back) paths.
     */
    private fun fireCreaturesDiedBatchTrigger(
        ability: TriggeredAbility,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId,
        deathsByController: Map<EntityId, List<CreatureDeathInfo>>,
        triggers: MutableList<PendingTrigger>
    ) {
        val trigger = ability.trigger
        if (trigger !is EventPattern.CreaturesYouControlDiedEvent) return

        val controllerDeaths = deathsByController[controllerId] ?: return

        // "one or more *other* creatures you control die" excludes the source's own death.
        val relevantDeaths = if (trigger.excludeSelf) {
            controllerDeaths.filter { it.entityId != sourceId }
        } else {
            controllerDeaths
        }
        if (relevantDeaths.isEmpty()) return

        val hasMatch = relevantDeaths.any { info ->
            trigger.filter.cardPredicates.all { predicate ->
                when (predicate) {
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                        info.typeLine?.isCreature == true
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                        info.typeLine?.hasSubtype(predicate.subtype) == true
                    else -> true
                }
            }
        }
        if (!hasMatch) return

        triggers.add(
            PendingTrigger(
                ability = ability,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                triggerContext = TriggerContext()
            )
        )
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
                if (trigger !is EventPattern.PermanentsEnteredEvent) continue

                val controllerId = entry.controllerId
                val controllerEnters = entersByController[controllerId] ?: continue

                // Check if any entering permanent matches the filter
                val hasMatch = controllerEnters.any { info ->
                    trigger.filter.cardPredicates.all { predicate ->
                        when (predicate) {
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> info.cardComponent.typeLine.isCreature
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature -> !info.cardComponent.typeLine.isCreature
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !info.cardComponent.typeLine.isLand
                            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonartifact -> !info.cardComponent.typeLine.isArtifact
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
                if (trigger is EventPattern.ZoneChangeEvent &&
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
                        trigger = EventPattern.StepEvent(Step.PRECOMBAT_MAIN, Player.You),
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
     * Duplicate ETB and LTB triggers for [AdditionalETBOrLTBTriggers] static abilities
     * (Naban, Panharmonicon, Starfield Vocalist, Gandalf the White).
     *
     * For each ZoneChangeEvent whose `toZone == BATTLEFIELD` (entering) or whose `fromZone
     * == BATTLEFIELD` (leaving), checks every battlefield permanent whose static matches the
     * filter and the direction. For each match, duplicates triggers belonging to the
     * static's controller whose triggering entity is the cause and whose trigger pattern
     * matches the direction.
     *
     * Multiple copies are additive: N copies add N extra copies of each trigger.
     */
    private fun duplicateETBOrLTBTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val registry = cardRegistry
        val projected = state.projectedState

        // Battlefield-boundary-crossing events: pure entering, or pure leaving. A same-batch
        // zone change that goes battlefield → battlefield is not a thing the engine emits, so
        // we don't have to disambiguate.
        val zoneEvents = events.filterIsInstance<ZoneChangeEvent>().filter {
            it.toZone == Zone.BATTLEFIELD ||
                (it.fromZone == Zone.BATTLEFIELD && it.toZone != Zone.BATTLEFIELD)
        }
        if (zoneEvents.isEmpty()) return

        data class Doubler(
            val controllerId: EntityId,
            val filter: GameObjectFilter,
            val sourceId: EntityId,
            val mustBeYouControl: Boolean,
            val directions: Set<BattlefieldDirection>,
        )
        val doublers = mutableListOf<Doubler>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue

            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is AdditionalETBOrLTBTriggers) {
                    doublers.add(
                        Doubler(
                            controllerId = controllerId,
                            filter = ability.filter,
                            sourceId = permanentId,
                            mustBeYouControl = ability.mustBeYouControl,
                            directions = ability.directions,
                        )
                    )
                }
            }
        }

        if (doublers.isEmpty()) return

        val duplicates = mutableListOf<PendingTrigger>()

        for (event in zoneEvents) {
            val direction = if (event.toZone == Zone.BATTLEFIELD) BattlefieldDirection.ENTERING
                else BattlefieldDirection.LEAVING
            val causeEntityId = event.entityId

            for (doubler in doublers) {
                if (direction !in doubler.directions) continue

                if (doubler.mustBeYouControl) {
                    // Entering → consult projected controller (entity is on the battlefield now).
                    // Leaving → projected state has no entry; use the last-known controller
                    // snapshotted on the ZoneChangeEvent, falling back to owner.
                    val causeController = when (direction) {
                        BattlefieldDirection.ENTERING ->
                            projected.getController(causeEntityId) ?: event.ownerId
                        BattlefieldDirection.LEAVING ->
                            event.lastKnownController ?: event.ownerId
                    }
                    if (causeController != doubler.controllerId) continue
                }

                if (doubler.filter != GameObjectFilter.Any) {
                    if (!causeMatchesDoublerFilter(
                            state, projected, event, direction,
                            doubler.filter, doubler.controllerId, doubler.sourceId
                        )) continue
                }

                for (trigger in triggers) {
                    if (trigger.triggerContext.triggeringEntityId != causeEntityId) continue
                    if (trigger.controllerId != doubler.controllerId) continue

                    val triggerEvent = trigger.ability.trigger
                    if (triggerEvent !is EventPattern.ZoneChangeEvent) continue
                    when (direction) {
                        BattlefieldDirection.ENTERING ->
                            if (triggerEvent.to != Zone.BATTLEFIELD) continue
                        BattlefieldDirection.LEAVING ->
                            if (triggerEvent.from != Zone.BATTLEFIELD) continue
                    }

                    duplicates.add(trigger)
                }
            }
        }

        triggers.addAll(duplicates)
    }

    /**
     * Does the entity that crossed the battlefield boundary in [event] match the doubler's [filter]?
     *
     * Entering: the cause is live on the battlefield, so projected state is authoritative — match
     * directly.
     *
     * Leaving: the cause is no longer on the battlefield. Its base entity (now in the graveyard)
     * carries only its printed type line — the projected types it had on the battlefield are gone —
     * and a token cause may even have been swept by 704.5d. Per CR 603.10a leaves-the-battlefield
     * checks look back in time, so we evaluate the filter against the entity's last-known type line
     * (snapshotted on the [ZoneChangeEvent]), mirroring `TriggerMatcher.matchesZoneChangeTrigger`.
     * We overlay that type line onto the cause entity (synthesizing a minimal one if it was swept)
     * and reuse the full filter engine, so `or`/color/subtype predicates all resolve correctly.
     * Without this, a creature that was an artifact/legendary only via a continuous effect (e.g.
     * a creature turned into an artifact, then destroyed) would silently fail Gandalf the White's
     * `Artifact ∨ Legendary` filter and the trigger it caused wouldn't be doubled.
     */
    private fun causeMatchesDoublerFilter(
        state: GameState,
        projected: ProjectedState,
        event: ZoneChangeEvent,
        direction: BattlefieldDirection,
        filter: GameObjectFilter,
        controllerId: EntityId,
        sourceId: EntityId
    ): Boolean {
        val context = PredicateContext(controllerId = controllerId, sourceId = sourceId)

        if (direction == BattlefieldDirection.ENTERING) {
            return predicateEvaluator.matches(state, projected, event.entityId, filter, context)
        }

        val lastKnownTypeLine = event.lastKnownTypeLine
        val existing = state.getEntity(event.entityId)
        val existingCard = existing?.get<CardComponent>()
        val overlayCard = when {
            existingCard != null ->
                existingCard.copy(typeLine = lastKnownTypeLine ?: existingCard.typeLine)
            lastKnownTypeLine != null -> CardComponent(
                cardDefinitionId = event.lastKnownCardDefinitionId ?: event.entityName,
                name = event.entityName,
                manaCost = ManaCost.ZERO,
                typeLine = lastKnownTypeLine
            )
            else -> return false
        }
        // Off-battlefield, so the original `projected` has no entry for this id and matches() falls
        // back to the overlaid card's type line. No projection recompute needed.
        val overlayState = state.withEntity(
            event.entityId,
            (existing ?: ComponentContainer.EMPTY).withComponent(overlayCard)
        )
        return predicateEvaluator.matches(overlayState, projected, event.entityId, filter, context)
    }

    /**
     * Duplicate triggers caused by a creature being declared as an attacker for
     * [AdditionalAttackTriggers] static abilities (Windcrag Siege's Mardu mode).
     *
     * The attack-cause analogue of [duplicateETBTriggers]: for each [AttackersDeclaredEvent], if a
     * permanent the doubler controls has an attack-related triggered ability ("whenever a creature
     * attacks" / "whenever you attack") that fired from that event, that trigger fires an additional
     * time per copy. Only the declared attackers matching [AdditionalAttackTriggers.attackerFilter]
     * count as the cause; if the trigger names a specific attacker (per-attacker AttackEvent), that
     * attacker must match.
     *
     * Multiple copies are additive: N copies add N extra firings of each affected trigger.
     */
    private fun duplicateAttackTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val attackEvents = events.filterIsInstance<AttackersDeclaredEvent>()
        if (attackEvents.isEmpty() || triggers.isEmpty()) return

        val registry = cardRegistry
        val projected = state.projectedState

        data class AttackDoubler(
            val controllerId: EntityId,
            val filter: GameObjectFilter,
            val sourceId: EntityId,
        )
        val doublers = mutableListOf<AttackDoubler>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                // Unwrap mode/condition-gated abilities (e.g. Windcrag Siege's Mardu mode) and
                // honor the gate against this source permanent.
                val unwrapped: AdditionalAttackTriggers? = when (ability) {
                    is AdditionalAttackTriggers -> ability
                    is ConditionalStaticAbility ->
                        (ability.ability as? AdditionalAttackTriggers)?.takeIf {
                            conditionEvaluator.evaluate(
                                state, ability.condition,
                                EffectContext(sourceId = permanentId, controllerId = controllerId)
                            )
                        }
                    else -> null
                }
                if (unwrapped != null) {
                    doublers.add(AttackDoubler(controllerId, unwrapped.attackerFilter, permanentId))
                }
            }
        }

        if (doublers.isEmpty()) return

        val duplicates = mutableListOf<PendingTrigger>()

        for (attackEvent in attackEvents) {
            for (doubler in doublers) {
                // Which declared attackers satisfy this doubler's filter (the "cause").
                val matchingAttackers = attackEvent.attackers.filter { attackerId ->
                    doubler.filter == GameObjectFilter.Any ||
                        predicateEvaluator.matches(
                            state, projected, attackerId, doubler.filter,
                            PredicateContext(controllerId = doubler.controllerId, sourceId = doubler.sourceId)
                        )
                }
                if (matchingAttackers.isEmpty()) continue

                for (trigger in triggers) {
                    if (trigger.controllerId != doubler.controllerId) continue

                    // Only attack-caused triggers: "whenever a creature attacks" / "whenever you attack".
                    val triggerEvent = trigger.ability.trigger
                    val isAttackTrigger = triggerEvent is EventPattern.AttackEvent ||
                        triggerEvent is EventPattern.YouAttackEvent
                    if (!isAttackTrigger) continue

                    // If the trigger names a specific attacker (per-attacker AttackEvent), that
                    // attacker must be one of the cause attackers. Triggers without a specific
                    // attacker (e.g. "whenever you attack") are caused by the attack as a whole.
                    val triggeringAttacker = trigger.triggerContext.triggeringEntityId
                    if (triggeringAttacker != null && triggeringAttacker !in matchingAttackers) continue

                    duplicates.add(trigger)
                }
            }
        }

        triggers.addAll(duplicates)
    }

    /**
     * Duplicate triggers for [AdditionalSourceTriggers] static abilities (e.g., Twinflame Travelers).
     *
     * For each pending trigger whose source permanent matches a doubler's filter and is
     * controlled by the doubler's controller, add an extra copy. Self-exclusion (the doubler's
     * own source) is honoured per [AdditionalSourceTriggers.excludeSelf].
     *
     * Multiple copies are additive: N doublers each cause one extra firing per matching trigger,
     * yielding N+1 total firings.
     */
    private fun duplicateSourceTriggers(
        state: GameState,
        triggers: MutableList<PendingTrigger>
    ) {
        if (triggers.isEmpty()) return
        val registry = cardRegistry
        val projected = state.projectedState

        data class SourceDoubler(
            val doublerSourceId: EntityId,
            val controllerId: EntityId,
            val filter: GameObjectFilter,
            val excludeSelf: Boolean
        )
        val doublers = mutableListOf<SourceDoubler>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is AdditionalSourceTriggers) {
                    doublers.add(
                        SourceDoubler(
                            doublerSourceId = permanentId,
                            controllerId = controllerId,
                            filter = ability.sourceFilter,
                            excludeSelf = ability.excludeSelf
                        )
                    )
                }
            }
        }

        if (doublers.isEmpty()) return

        val duplicates = mutableListOf<PendingTrigger>()
        // Iterate over a snapshot so duplicates added by earlier doublers don't re-multiply.
        val originals = triggers.toList()
        for (doubler in doublers) {
            for (trigger in originals) {
                if (trigger.controllerId != doubler.controllerId) continue
                val triggerSourceId = trigger.sourceId
                if (doubler.excludeSelf && triggerSourceId == doubler.doublerSourceId) continue
                if (!predicateEvaluator.matches(
                        state, projected, triggerSourceId, doubler.filter,
                        PredicateContext(controllerId = doubler.controllerId, sourceId = doubler.doublerSourceId)
                    )
                ) continue
                duplicates.add(trigger)
            }
        }

        triggers.addAll(duplicates)
    }

    /**
     * Detect "When you unlock this door" triggers (CR 709.5h, DSK Rooms).
     *
     * Face-aware: for each [DoorUnlockedEvent], we look up the Room's `cardFaces[i]` whose
     * id matches the unlocked face id and add SELF-bound `OnDoorUnlocked` triggers from
     * *that* face's script only. This ensures that already-unlocked faces' "when you
     * unlock this door" abilities don't re-fire when a *different* door is unlocked.
     *
     * Both ETB-time unlocks (from the cast face) and unlock-action transitions emit
     * [DoorUnlockedEvent], so both paths feed through here.
     */
    private fun detectDoorUnlockedTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        for (event in events) {
            if (event !is DoorUnlockedEvent) continue
            val container = state.getEntity(event.roomId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val roomComp = container.get<RoomComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val face = cardDef.cardFaces.firstOrNull {
                RoomFaceId(it.name) == event.faceId
            } ?: continue

            // Sanity-check: only fire if the face is currently unlocked (the engine emits
            // the event right after applying the unlock, so this should always be true).
            if (event.faceId !in roomComp.unlocked) continue

            for (ability in face.script.effectiveTriggeredAbilities(null)) {
                if (ability.trigger !is EventPattern.DoorUnlockedEvent) continue
                if (ability.binding != TriggerBinding.SELF) continue
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = event.roomId,
                        sourceName = cardComponent.name,
                        controllerId = event.controllerId,
                        triggerContext = TriggerContext(triggeringEntityId = event.roomId)
                    )
                )
            }
        }
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

        // Face-down creatures have no abilities (Rule 708.2)
        if (container.has<FaceDownComponent>()) return

        // Only fire if control actually changed
        if (event.oldControllerId == event.newControllerId) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger is EventPattern.ControlChangeEvent && ability.binding == TriggerBinding.SELF) {
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
