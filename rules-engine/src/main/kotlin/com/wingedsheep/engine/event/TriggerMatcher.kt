package com.wingedsheep.engine.event
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.handlers.effects.permanent.counters.counterTypeToString

import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GiftGivenEvent
import com.wingedsheep.engine.core.RoomFullyUnlockedEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CommitCrimeEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.SagaChapterResolvedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.core.LandTappedForManaEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.PhasedInEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.scripting.events.AbilityTargetMatch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ExploreReveal
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.evaluateWith

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Contains shared trigger matching and predicate logic used by all detectors.
 */
class TriggerMatcher(
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator
) {

    fun matchesTrigger(
        trigger: EventPattern,
        binding: TriggerBinding,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        // ATTACHED triggers are generally handled by AttachmentTriggerDetector, not the main loop.
        // Exception: BlocksOrBecomesBlockedByEvent ATTACHED is handled in the main loop, since it
        // needs the full BlockersDeclaredEvent block map to find the equipped creature's combat
        // partner (Barrow-Blade).
        if (binding == TriggerBinding.ATTACHED &&
            trigger !is EventPattern.BlocksOrBecomesBlockedByEvent
        ) return false

        return when (trigger) {
            is EventPattern.ZoneChangeEvent -> matchesZoneChangeTrigger(trigger, binding, event, sourceId, controllerId, state)
            is EventPattern.DrawEvent -> {
                event is CardsDrawnEvent && matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            // MillEvent is a replacement-only pattern (ModifyMillAmount); it never matches a
            // triggered ability. Applied at the mill announcement by MillAmountModifier.
            is EventPattern.MillEvent -> false
            is EventPattern.NthCardDrawnEvent -> {
                // Fires on CardsDrawnEvent when the drawing player's per-turn draw count
                // crosses the threshold inside this batch. The component is incremented
                // per individual draw in DrawCardPrimitive, so after the aggregate event
                // we have countAfter = CardsDrawnThisTurnComponent.count and
                // countBefore = countAfter - event.count.
                if (event !is CardsDrawnEvent) return false
                if (!matchesPlayer(trigger.player, event.playerId, controllerId)) return false
                val countAfter = state.getEntity(event.playerId)
                    ?.get<CardsDrawnThisTurnComponent>()
                    ?.count ?: 0
                val countBefore = countAfter - event.count
                countBefore < trigger.nthCard && trigger.nthCard <= countAfter
            }
            is EventPattern.CardRevealedFromDrawEvent -> {
                if (event !is CardRevealedFromDrawEvent) return false
                if (event.playerId != controllerId) return false
                val filter = trigger.cardFilter
                if (filter != null) {
                    val requiresCreature = filter.cardPredicates.any {
                        it is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature
                    }
                    if (requiresCreature && !event.isCreature) return false
                }
                true
            }
            is EventPattern.AttackEvent -> {
                event is AttackersDeclaredEvent &&
                    checkBinding(binding, sourceId, event.attackers) &&
                    trigger.requires.all { matchesAttackPredicate(it, event, sourceId, state) }
            }
            is EventPattern.YouAttackEvent -> {
                if (event !is AttackersDeclaredEvent) return false
                if (state.activePlayerId != controllerId) return false
                val filter = trigger.attackerFilter
                if (filter != null) {
                    // Count attackers matching the filter
                    val predicateEvaluator = PredicateEvaluator()
                    val projected = state.projectedState
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    val matchingCount = event.attackers.count { attackerId ->
                        predicateEvaluator.matches(state, projected, attackerId, filter, predicateContext)
                    }
                    matchingCount >= trigger.minAttackers
                } else {
                    event.attackers.size >= trigger.minAttackers
                }
            }
            is EventPattern.CreaturesAttackYouEvent -> {
                if (event !is AttackersDeclaredEvent) return false
                // Only count attackers declared against the player themself, not against
                // a planeswalker they control (per CR 509.1b / Orim's Prayer ruling).
                val attackingThisPlayer = event.attackers.count { attackerId ->
                    val attackingComponent = state.getEntity(attackerId)
                        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                    attackingComponent?.defenderId == controllerId
                }
                attackingThisPlayer >= trigger.minAttackers
            }
            is EventPattern.CreaturesAttackYourOpponentEvent -> {
                if (event !is AttackersDeclaredEvent) return false
                // Count attackers declared against an opponent *player* of the controller (not the
                // controller, and not a planeswalker — opponent ids are players). Party Dude L3.
                val opponents = state.getOpponents(controllerId).toSet()
                val attackingAnOpponent = event.attackers.count { attackerId ->
                    val defenderId = state.getEntity(attackerId)
                        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                        ?.defenderId
                    defenderId != null && defenderId in opponents
                }
                attackingAnOpponent >= trigger.minAttackers
            }
            is EventPattern.BlockEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.keys.contains(sourceId))
            }
            is EventPattern.BecomesBlockedEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.values.any { it.contains(sourceId) })
            }
            is EventPattern.BecomesUnblockedEvent -> {
                // CR 509.3g: fires for an attacker with no creatures declared as blockers.
                // We piggyback on BlockersDeclaredEvent (emitted once at end of
                // declare-blockers): SELF matches when sourceId is an attacker this combat
                // AND is absent from every blocker's blocked-attackers list.
                if (event !is BlockersDeclaredEvent) return false
                if (binding != TriggerBinding.SELF) return false
                val isAttacking = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>() != null
                isAttacking && event.blockers.values.none { it.contains(sourceId) }
            }
            is EventPattern.StateConditionMetEvent -> {
                // Synthetic — never matched against real events. State triggers fire via
                // StateTriggerPoller which produces PendingTriggers directly.
                false
            }
            is EventPattern.BlocksOrBecomesBlockedByEvent -> {
                // Basic match: it's a BlockersDeclaredEvent and the relevant creature is involved
                // in combat. Per-partner trigger creation happens in detectTriggersForEvent.
                // SELF: the source creature itself; ATTACHED: the source's equipped/enchanted
                // creature (Barrow-Blade). Other bindings don't apply.
                if (event !is BlockersDeclaredEvent) return false
                val combatCreatureId = when (binding) {
                    TriggerBinding.SELF -> sourceId
                    TriggerBinding.ATTACHED -> state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        ?.targetId ?: return false
                    else -> return false
                }
                // The combat creature is a blocker or an attacker that's being blocked.
                event.blockers.keys.contains(combatCreatureId) ||
                    event.blockers.values.any { it.contains(combatCreatureId) }
            }
            is EventPattern.DealsDamageEvent -> {
                // SELF-bound DealsDamageEvent handled separately in detectDamageSourceTriggers
                // Non-self (observer triggers like "whenever a creature deals damage to you") handled in
                // detectDamageToControllerTriggers and detectSubtypeDamageToPlayerTriggers
                false
            }
            is EventPattern.DamageReceivedEvent -> {
                // Generic (source=Any) DamageReceivedEvent can match in the main loop
                // Specific source-filtered ones are handled in detectDamagedBySourceTriggers
                if (trigger.source != SourceFilter.Any) return false
                event is DamageDealtEvent && (binding != TriggerBinding.SELF || event.targetId == sourceId)
            }
            is EventPattern.SpellCastEvent -> {
                event is SpellCastEvent &&
                    matchesPlayer(trigger.player, event.casterId, controllerId) &&
                    matchesSpellFilter(trigger.spellFilter, event, state, sourceId) &&
                    trigger.requires.all { matchesSpellCastPredicate(it, event, state, sourceId, controllerId) }
            }
            is EventPattern.NthSpellCastEvent -> {
                // Fires on SpellCastEvent when the casting player's per-turn spell count
                // reaches exactly the specified threshold (e.g., 2 for "second spell").
                if (event !is SpellCastEvent) return false
                if (!matchesPlayer(trigger.player, event.casterId, controllerId)) return false
                val currentCount = state.playerSpellsCastThisTurn[event.casterId] ?: 0
                currentCount == trigger.nthSpell
            }
            is EventPattern.CastThisSpellEvent -> {
                // "When you cast this spell" — fires on the spell's own cast while on the stack.
                // detectSelfCastTriggers only invokes the matcher for the spell being cast, so
                // this just confirms the event is that very spell's cast (intervening-if, if any,
                // is enforced separately by filterByTriggerCondition per CR 603.4).
                event is SpellCastEvent && event.spellEntityId == sourceId
            }
            is EventPattern.ExpendEvent -> {
                // Expend triggers when cumulative mana spent on spells this turn
                // crosses the threshold. Fires on SpellCastEvent only.
                // Detects the "crossing": previous total < threshold <= new total.
                if (event !is SpellCastEvent) return false
                if (!matchesPlayer(trigger.player, event.casterId, controllerId)) return false

                val playerEntity = state.getEntity(event.casterId) ?: return false
                val manaComponent = playerEntity.get<ManaSpentOnSpellsThisTurnComponent>()
                    ?: return false
                val currentTotal = manaComponent.totalSpent
                val spentThisCast = event.totalManaSpent
                val previousTotal = currentTotal - spentThisCast
                // Trigger if we just crossed the threshold with this cast
                previousTotal < trigger.threshold && currentTotal >= trigger.threshold
            }
            is EventPattern.SpellOrAbilityOnStackEvent -> {
                // Intervening-if: only trigger if the spell/ability has a single target
                val stackEntityId = when (event) {
                    is SpellCastEvent -> event.spellEntityId
                    is AbilityActivatedEvent -> event.abilityEntityId
                    is AbilityTriggeredEvent -> event.abilityEntityId
                    else -> null
                }
                if (stackEntityId == null) return false
                val targets = state.getEntity(stackEntityId)
                    ?.get<TargetsComponent>()?.targets
                targets != null && targets.size == 1
            }
            is EventPattern.AbilityActivatedEvent -> {
                if (event !is AbilityActivatedEvent) return false
                if (!matchesPlayer(trigger.player, event.controllerId, controllerId)) return false
                if (trigger.requireNoTapInCost) {
                    // Antiquities "activates an ability without {T} in its activation cost"
                    // (Haunting Wind / Powerleech / Artifact Possession). Match any activated
                    // ability whose cost lacks {T} — mana abilities without {T} count too.
                    if (event.costsTap) return false
                } else {
                    // Default "activates an ability that isn't a mana ability" (Flamescroll
                    // Celebrant): the engine emits AbilityActivatedEvent for non-mana abilities
                    // (which use the stack) and, for the {T}-cost template above, for non-{T} mana
                    // abilities — so explicitly reject the mana-ability ones here. Loyalty
                    // abilities qualify (they aren't mana abilities).
                    if (event.isManaAbility) return false
                }
                // SELF/ATTACHED binding: the ability's source must be this permanent (Artifact
                // Possession — "enchanted artifact"); the source is exposed via TriggerContext as
                // the triggering entity for the ATTACHED check upstream, but the source-id match
                // here keys directly off event.sourceId.
                if (binding == TriggerBinding.SELF && event.sourceId != sourceId) return false
                // sourceFilter: the permanent whose ability was activated must match (e.g. an
                // artifact, or an artifact an opponent controls).
                val sourceFilter = trigger.sourceFilter
                if (sourceFilter != null) {
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    if (!PredicateEvaluator().matches(
                            state, state.projectedState, event.sourceId, sourceFilter, predicateContext
                        )
                    ) return false
                }
                val targetMatch = trigger.targetMatch
                if (targetMatch != null) {
                    // "...that targets a creature or player": the activated ability on the stack
                    // must have at least one chosen target satisfying the constraint. A
                    // non-targeting ability has no TargetsComponent and therefore never matches.
                    matchesAbilityTargetConstraint(event.abilityEntityId, targetMatch, sourceId, controllerId, state)
                } else true
            }
            is EventPattern.AbilityTriggeredEvent -> {
                if (event !is AbilityTriggeredEvent) return false
                if (!matchesPlayer(trigger.player, event.controllerId, controllerId)) return false
                // "attacking causes a triggered ability of that creature to trigger": only fire for
                // abilities the engine stamped as attack-caused (SELF-bound attacks triggers).
                if (trigger.requireAttackCause && !event.causedByAttack) return false
                // sourceFilter: the permanent whose ability triggered must match (null = any).
                val sourceFilter = trigger.sourceFilter
                if (sourceFilter != null) {
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    if (!PredicateEvaluator().matches(
                            state, state.projectedState, event.sourceId, sourceFilter, predicateContext
                        )
                    ) return false
                }
                true
            }
            is EventPattern.CycleEvent -> {
                event is CardCycledEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId) &&
                    (binding != TriggerBinding.SELF || event.cardId == sourceId)
            }
            is EventPattern.GiftGivenEvent -> {
                event is GiftGivenEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.CommitCrimeEvent -> {
                event is CommitCrimeEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.BecomesPlottedEvent -> {
                // "When this card becomes plotted" triggers fire on a card that is now in exile,
                // not on the battlefield, so the index-driven main loop never reaches them.
                // TriggerDetector.detectPlottedCardTriggers handles them directly; returning false
                // here keeps the regular loop from double-firing or mis-binding them.
                false
            }
            is EventPattern.BecameSaddledEvent -> {
                // Saddled permanents stay on the battlefield (CR 702.171b), so this matches in the
                // regular battlefield trigger loop. SELF binding must match the saddled permanent.
                if (event !is com.wingedsheep.engine.core.BecameSaddledEvent) return false
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                // "for the first time each turn" intervening-if — only the first saddle this turn.
                if (trigger.firstTimeEachTurn && !event.firstThisTurn) return false
                if (trigger.filter != GameObjectFilter.Any) {
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    PredicateEvaluator().matches(
                        state, state.projectedState, event.entityId, trigger.filter, predicateContext
                    )
                } else true
            }
            is EventPattern.BecomesAttachedEvent -> {
                if (event !is com.wingedsheep.engine.core.PermanentAttachedEvent) return false
                // SELF binding = "whenever THIS Equipment/Aura becomes attached" (the attachment
                // is the source, e.g. Assimilation Aegis).
                if (binding == TriggerBinding.SELF && event.attachmentId != sourceId) return false
                // The controller of the attachment (CR — "an Aura you control").
                if (!matchesPlayer(trigger.attachmentController, event.controllerId, controllerId)) return false
                // The attachment must match the attachment filter (e.g. Aura, Equipment).
                val attachmentCtx = com.wingedsheep.engine.handlers.PredicateContext(
                    controllerId = controllerId,
                    sourceId = sourceId,
                )
                if (trigger.attachmentFilter != GameObjectFilter.Any &&
                    !PredicateEvaluator().matches(
                        state, state.projectedState, event.attachmentId, trigger.attachmentFilter, attachmentCtx
                    )
                ) return false
                // The attached-to permanent must match the attached-to filter, with the attachment
                // exposed as EntityReference.Triggering so relative predicates (mana value at most
                // the Aura's mana value — Eriette) resolve against it.
                if (trigger.attachedToFilter != GameObjectFilter.Any) {
                    val attachedToCtx = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId,
                        triggeringEntityId = event.attachmentId,
                    )
                    PredicateEvaluator().matches(
                        state, state.projectedState, event.attachedToId, trigger.attachedToFilter, attachedToCtx
                    )
                } else true
            }
            is EventPattern.TargetsChosenEvent -> {
                event is com.wingedsheep.engine.core.TargetsChosenEvent &&
                    matchesPlayer(trigger.player, event.chooserId, controllerId)
            }
            is EventPattern.RoomFullyUnlockedEvent -> {
                event is RoomFullyUnlockedEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.DoorUnlockedEvent -> {
                // Face-scoped "When you unlock this door" triggers (CR 709.5h) are handled
                // by TriggerDetector.detectDoorUnlockedTriggers, which knows the source face
                // of each face-script ability. Returning false here keeps the regular index
                // loop from firing them on the wrong face.
                false
            }
            is EventPattern.TapEvent -> {
                if (event !is TappedEvent) return false
                // Batch ("one or more become tapped") triggers fire once per tap batch, handled by
                // detectTapBatchTriggers — never once per event here.
                if (trigger.batch) return false
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                val filter = trigger.filter
                if (filter != null) {
                    val predicateEvaluator = PredicateEvaluator()
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.entityId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.UntapEvent -> {
                if (event !is UntappedEvent) return false
                // Batch ("one or more become untapped") triggers fire once per untap batch, handled
                // by detectUntapBatchTriggers — never once per event here.
                if (trigger.batch) return false
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                val filter = trigger.filter
                if (filter != null) {
                    val predicateEvaluator = PredicateEvaluator()
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.entityId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.PhasesInEvent -> {
                if (event !is PhasedInEvent) return false
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                if (binding == TriggerBinding.OTHER && event.entityId == sourceId) return false
                val filter = trigger.filter
                if (filter != null) {
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.entityId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.LandTappedForMana -> {
                if (event !is LandTappedForManaEvent) return false
                if (binding == TriggerBinding.SELF && event.landId != sourceId) return false
                if (!matchesPlayer(trigger.player, event.tapperId, controllerId)) return false
                val filter = trigger.landFilter
                if (filter != null) {
                    val predicateEvaluator = PredicateEvaluator()
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.landId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.LifeGainEvent -> {
                event is LifeChangedEvent &&
                    event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    matchesPlayer(trigger.player, event.playerId, controllerId) &&
                    (!trigger.firstTimeEachTurn || event.firstThisTurn)
            }
            is EventPattern.RingTemptedEvent -> {
                event is com.wingedsheep.engine.core.RingTemptedEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId) &&
                    (!trigger.requireBearerChosen || event.bearerId != null)
            }
            is EventPattern.ScriedEvent -> {
                event is com.wingedsheep.engine.core.ScriedEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.SurveiledEvent -> {
                event is com.wingedsheep.engine.core.SurveiledEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.ScriedOrSurveiledEvent -> when (event) {
                is com.wingedsheep.engine.core.ScriedEvent ->
                    matchesPlayer(trigger.player, event.playerId, controllerId)
                is com.wingedsheep.engine.core.SurveiledEvent ->
                    matchesPlayer(trigger.player, event.playerId, controllerId)
                else -> false
            }
            is EventPattern.DiscoveredEvent -> {
                event is com.wingedsheep.engine.core.DiscoveredEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.ExploredEvent -> {
                if (event !is com.wingedsheep.engine.core.PermanentExploredEvent) return false
                // Reveal-type gate (CR 701.44a): ANY always matches; LAND/NONLAND require the
                // matching reveal. Empty-library explore (wasLand == null) matches only ANY.
                when (trigger.revealedType) {
                    ExploreReveal.ANY -> {}
                    ExploreReveal.LAND -> if (event.revealedCardWasLand != true) return false
                    ExploreReveal.NONLAND -> if (event.revealedCardWasLand != false) return false
                }
                // Subject gate: the exploring permanent must match the filter, resolving "you" to
                // the observing ability's controller. A null filter matches any permanent.
                val filter = trigger.filter ?: return true
                predicateEvaluator.matches(
                    state,
                    state.projectedState,
                    event.exploringPermanentId,
                    filter,
                    PredicateContext(controllerId = controllerId, sourceId = sourceId)
                )
            }
            is EventPattern.ExploitedEvent -> {
                if (event !is com.wingedsheep.engine.core.ExploitedEvent) return false
                // SELF: the exploiter is this permanent (a creature's own "when it exploits" payoff —
                // though the DSL bakes those into the reflexive, so watchers are the common case).
                // OTHER: any exploiter but this permanent. ANY: no restriction.
                if (binding == TriggerBinding.SELF && event.exploiterId != sourceId) return false
                if (binding == TriggerBinding.OTHER && event.exploiterId == sourceId) return false
                // "a creature you control exploits ..." scopes on the EXPLOITER's controller.
                if (!matchesPlayer(trigger.player, event.exploiterControllerId, controllerId)) return false
                // "exploits a NONTOKEN creature" (Skull Skaab): reject when the sacrificed
                // permanent was a token (last-known info captured before the zone change).
                if (trigger.requireNontokenExploited && event.sacrificedWasToken) return false
                true
            }
            is EventPattern.TrainedEvent -> {
                if (event !is com.wingedsheep.engine.core.TrainedEvent) return false
                // "When THIS creature trains" (Savior of Ollenbock): SELF binding restricts to the
                // ability's own source. OTHER would be "another creature you control trains"; ANY has
                // no restriction. The emit already gates on a counter actually landing (CR 702.149c),
                // so any TrainedEvent that reaches here is a genuine train.
                if (binding == TriggerBinding.SELF && event.trainedId != sourceId) return false
                if (binding == TriggerBinding.OTHER && event.trainedId == sourceId) return false
                true
            }
            is EventPattern.BendPerformedEvent -> {
                event is com.wingedsheep.engine.core.BendPerformedEvent &&
                    event.bendType in trigger.types &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.ManifestedDreadEvent -> {
                event is com.wingedsheep.engine.core.ManifestedDreadEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.BecomesTargetEvent -> {
                event is BecomesTargetEvent && matchesBecomesTargetTrigger(trigger, binding, event, sourceId, controllerId, state)
            }
            is EventPattern.TurnFaceUpEvent -> {
                event is TurnFaceUpEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is EventPattern.CreatureTurnedFaceUpEvent -> {
                if (event !is TurnFaceUpEvent) return false
                matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.TransformEvent -> {
                if (event !is TransformedEvent) return false
                // SELF binding must match the transforming permanent
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                // intoBackFace==null matches any transform; true/false filters by direction
                val directionMatches = trigger.intoBackFace == null ||
                    trigger.intoBackFace == event.intoBackFace
                directionMatches
            }
            // These are handled separately in their own detect* methods
            is EventPattern.ControlChangeEvent -> false
            // Phase/step triggers are handled separately
            is EventPattern.StepEvent -> false
            // Creature-dealt-damage-by-source-dies triggers are handled separately
            is EventPattern.CreatureDealtDamageBySourceDiesEvent -> false
            // "When damage is prevented this way" fires only via its linked delayed trigger
            // (detectEventBasedDelayedTriggers), never as a battlefield trigger.
            is EventPattern.DamagePreventedEvent -> false
            is EventPattern.CardPlayedFromPermissionEvent -> false
            // Replacement-effect-only events never match as triggers
            is EventPattern.DamageEvent -> false
            is EventPattern.CounterPlacementEvent -> false
            // "Whenever you create a token" (Mirkwood Bats) — fires per token created (CR's
            // singular templating), matched against each token-creation ZoneChangeEvent.
            is EventPattern.TokenCreationEvent -> matchesTokenCreationTrigger(trigger, event, controllerId, state)
            is EventPattern.LifeLossEvent -> {
                event is LifeChangedEvent &&
                    event.reason != com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    event.oldLife > event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.LifeGainOrLossEvent -> {
                event is LifeChangedEvent &&
                    event.oldLife != event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.PlayerLostGameEvent -> {
                event is com.wingedsheep.engine.core.PlayerLostEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.DiscardEvent -> {
                event is CardsDiscardedEvent &&
                    matchingDiscardCount(trigger, event, sourceId, controllerId, state) > 0
            }
            is EventPattern.SearchLibraryEvent -> {
                event is com.wingedsheep.engine.core.LibrarySearchedEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            // ExtraTurnEvent is only used as a replacement effect filter, not a trigger
            is EventPattern.ExtraTurnEvent -> false
            // Batching trigger — handled in detectLibraryToGraveyardBatchTriggers
            is EventPattern.CardsPutIntoGraveyardFromLibraryEvent -> false
            // Batching trigger handled separately by detectAnyToGraveyardBatchTriggers
            is EventPattern.CardsPutIntoYourGraveyardEvent -> false
            // Leave-graveyard batch triggers are handled by detectCardsLeftGraveyardBatchTriggers
            is EventPattern.CardsLeftYourGraveyardEvent -> false
            // Sacrifice batch triggers are handled by detectSacrificeBatchTriggers
            is EventPattern.PermanentsSacrificedEvent -> false
            // Combat damage batch triggers are handled by detectCombatDamageBatchTriggers
            is EventPattern.OneOrMoreDealCombatDamageToPlayerEvent -> false
            is EventPattern.OneOrMoreDealCombatDamageToYouEvent -> false
            // Leave battlefield without dying batch triggers are handled by detectLeaveBattlefieldWithoutDyingBatchTriggers
            is EventPattern.LeaveBattlefieldWithoutDyingEvent -> false
            // Creatures-you-control-die batch triggers are handled by detectCreaturesDiedBatchTriggers
            is EventPattern.CreaturesYouControlDiedEvent -> false
            // Enter battlefield batch triggers are handled by detectPermanentsEnteredBatchTriggers
            is EventPattern.PermanentsEnteredEvent -> false
            is EventPattern.CountersPlacedEvent -> {
                if (event !is CountersAddedEvent) return false
                // SELF binding: only counters landing on this permanent ("counters on Aragorn"
                // / "whenever you put counters on ~"). OTHER restricts to any *other* permanent.
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                if (binding == TriggerBinding.OTHER && event.entityId == sourceId) return false
                // Counters.ANY is the wildcard "counters of any type" sentinel.
                if (trigger.counterType != com.wingedsheep.sdk.core.Counters.ANY &&
                    !counterTypesMatch(trigger.counterType, event.counterType)) return false
                // "First time counters this turn" intervening-if (Stalwart Successor).
                if (trigger.firstTimeEachTurn && !event.firstThisTurn) return false
                // Placer restriction (CR 122.6a): "Whenever YOU put counters ...". A placement the
                // engine didn't attribute to a placer (null) never satisfies a non-null selector.
                trigger.placedBy?.let { placer ->
                    val placedBy = event.placedBy ?: return false
                    if (!matchesPlayer(placer, placedBy, controllerId)) return false
                }
                // Check filter: the permanent receiving counters must match
                if (trigger.filter != GameObjectFilter.Any) {
                    val projected = state.projectedState
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    val predicateEvaluator = PredicateEvaluator()
                    if (!predicateEvaluator.matches(state, projected, event.entityId, trigger.filter, predicateContext)) {
                        return false
                    }
                }
                true
            }
            is EventPattern.SagaChapterResolvedEvent -> {
                if (event !is SagaChapterResolvedEvent) return false
                // "of a Saga you control" — the resolving Saga's controller must match the
                // observer (Tom's controller). Player.You is the only meaningful selector here.
                if (!matchesPlayer(trigger.player, event.controllerId, controllerId)) return false
                if (trigger.finalChapterOnly && !event.isFinalChapter) return false
                true
            }
        }
    }

    /**
     * How many of the discarded cards satisfy this discard trigger — i.e. how many times the
     * ability fires. Discarding N cards in one resolution fires "whenever ... discards a card"
     * N times (one per card); a [EventPattern.DiscardEvent.cardFilter] narrows that to the matching
     * cards only. Returns 0 when the discarding player doesn't match the trigger's selector, so
     * the boolean "does it trigger?" question is just `matchingDiscardCount(...) > 0`.
     *
     * The filter is evaluated against post-discard state — the cards are already in the graveyard.
     * Safe for zone-independent predicates (type/subtype/color); a filter depending on hand-specific
     * state would read the wrong zone.
     */
    fun matchingDiscardCount(
        trigger: EventPattern.DiscardEvent,
        event: CardsDiscardedEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Int {
        if (!matchesPlayer(trigger.player, event.playerId, controllerId)) return 0
        val filter = trigger.cardFilter ?: return event.cardIds.size
        val projected = state.projectedState
        val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
            controllerId = controllerId,
            sourceId = sourceId
        )
        return event.cardIds.count { cardId ->
            predicateEvaluator.matches(state, projected, cardId, filter, predicateContext)
        }
    }

    /**
     * How many times a [EventPattern.DrawEvent] trigger fires for one aggregate
     * [CardsDrawnEvent] (CR 121.2 — each drawn card is an individual draw, so the trigger fires
     * once per card). Returns 0 when the drawing player doesn't match the trigger's player scope.
     *
     * When [EventPattern.DrawEvent.exceptFirstInDrawStep] is set, the first card the drawing
     * player draws in their *own* draw step (CR 504.1) is exempt and subtracted from the count.
     * That exempt card is the one drawn when the player's cards-drawn-this-turn count equals the
     * draw-step-start snapshot ([GameState.drawStepStartDrawCountByPlayer]); it's contained in this
     * batch iff `countBefore <= snapshot < countAfter`. Used by Orcish Bowmasters.
     *
     * [state] is the POST-execution state, so [CardsDrawnThisTurnComponent] already includes every
     * draw of the whole event batch. When one execution emitted several [CardsDrawnEvent]s for the
     * same player ("Draw a card" twice in one resolution), [samePlayerDrawsLaterInBatch] backs the
     * component count off to this event's own boundary so the exemption lands on the right card.
     */
    fun drawTriggerFiringCount(
        trigger: EventPattern.DrawEvent,
        event: CardsDrawnEvent,
        controllerId: EntityId,
        state: GameState,
        samePlayerDrawsLaterInBatch: Int = 0
    ): Int {
        if (!matchesPlayer(trigger.player, event.playerId, controllerId)) return 0
        val total = event.count
        if (!trigger.exceptFirstInDrawStep || total == 0) return total

        val drawer = event.playerId
        // Exemption only applies in the drawing player's own draw step.
        val inOwnDrawStep = state.activePlayerId == drawer && state.step == Step.DRAW
        if (!inOwnDrawStep) return total

        val countAfter = (state.getEntity(drawer)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0) -
            samePlayerDrawsLaterInBatch
        val countBefore = countAfter - total
        val snapshot = state.drawStepStartDrawCountByPlayer[drawer] ?: 0
        val exemptInBatch = if (countBefore <= snapshot && snapshot < countAfter) 1 else 0
        return total - exemptInBatch
    }

    /**
     * "Whenever you create a token" (Mirkwood Bats). A token is *created* when it enters the
     * battlefield from nowhere — its [ZoneChangeEvent] has `fromZone == null`. A token that's a
     * copy of a permanent spell enters from [Zone.STACK] instead and is explicitly **not** created
     * (CR 608.3f / 111.13), so the `fromZone == null` gate excludes it. One event is emitted per
     * token, so this fires once per token created (the singular "a token" templating), e.g. each
     * Soldier from Horn of Gondor's `{3},{T}` drains separately.
     */
    private fun matchesTokenCreationTrigger(
        trigger: EventPattern.TokenCreationEvent,
        event: EngineGameEvent,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        if (event !is ZoneChangeEvent) return false
        if (event.fromZone != null || event.toZone != Zone.BATTLEFIELD) return false
        val entity = state.getEntity(event.entityId) ?: return false
        if (!entity.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()) return false

        // Controller of the freshly-created token (owner == controller at creation; prefer projected).
        val tokenController = state.projectedState.getController(event.entityId) ?: event.ownerId
        val controllerMatches = when (trigger.controller) {
            is ControllerFilter.You -> tokenController == controllerId
            is ControllerFilter.Opponent -> tokenController != controllerId
            is ControllerFilter.Any -> true
        }
        if (!controllerMatches) return false

        val filter = trigger.tokenFilter
        if (filter != null && !predicateEvaluator.matches(
                state, state.projectedState, event.entityId, filter,
                PredicateContext(controllerId = controllerId)
            )
        ) {
            return false
        }
        return true
    }

    fun matchesZoneChangeTrigger(
        trigger: EventPattern.ZoneChangeEvent,
        binding: TriggerBinding,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        if (event !is ZoneChangeEvent) return false

        // Match zones
        if (trigger.from != null && event.fromZone != trigger.from) return false
        if (trigger.to != null && event.toZone != trigger.to) return false
        if (trigger.excludeTo != null && event.toZone == trigger.excludeTo) return false
        // "if it wasn't sacrificed" (Urza's Miter, CR 701.21) — reject sacrifice deaths.
        if (trigger.excludeSacrifice && event.wasSacrificed) return false
        // "while you're activating a craft ability" (Market Gnome, CR 702.167) — fire only when
        // this exile was a chosen craft material, not on any other exile.
        if (trigger.requireCraftMaterial && !event.craftMaterial) return false

        // Check binding
        when (binding) {
            TriggerBinding.SELF -> if (event.entityId != sourceId) return false
            TriggerBinding.OTHER -> if (event.entityId == sourceId) return false
            TriggerBinding.ANY -> { /* no entity restriction */ }
            TriggerBinding.ATTACHED -> return false // handled by AttachmentTriggerDetector
        }

        // Check filter
        if (trigger.filter != GameObjectFilter.Any) {
            // A heterogeneous OR union (`Artifact or Creature.tapped()`) carries its branches in
            // anyOf, each a complete filter — match if any branch matches. (A homogeneous OR
            // instead collapses to a single CardPredicate.Or handled below.)
            if (trigger.filter.anyOf.isNotEmpty()) {
                return trigger.filter.anyOf.any { branch ->
                    matchesZoneChangeTrigger(trigger.copy(filter = branch), binding, event, sourceId, controllerId, state)
                }
            }
            val projected = state.projectedState
            // Check card predicates (creature type, subtype, etc.)
            // Note: entity may not exist in state if it was a token cleaned up by SBAs.
            // In that case, fall back to lastKnownTypeLine from the event.
            val entity = state.getEntity(event.entityId)
            val cardComponent = entity?.get<CardComponent>()
            // For permanents leaving the battlefield, prefer lastKnownTypeLine since it
            // captures the projected types/subtypes at the moment of leaving (e.g., a creature
            // that was a Food artifact only because of Ygra's continuous effect). The base
            // cardComponent.typeLine in the new zone has only the printed types.
            val typeLine = if (event.fromZone == Zone.BATTLEFIELD && event.lastKnown?.typeLine != null) {
                event.lastKnown?.typeLine
            } else {
                cardComponent?.typeLine ?: event.lastKnown?.typeLine
            }
            val isFaceDown = entity?.has<FaceDownComponent>() == true

            // LKI-aware predicate evaluation. Composites (Or/And/Not — e.g. the collapsed
            // `Artifact or Creature` union on Tarrian's Soulcleaver) must recurse through THIS
            // function, not fall through to the live-entity path: a token is swept by 704.5d
            // before the matcher runs, so the generic cardComponent-based path returns false for
            // every predicate inside the composite and the trigger silently misses token deaths.
            fun matchesLkiPredicate(predicate: com.wingedsheep.sdk.scripting.predicates.CardPredicate): Boolean {
                return when (predicate) {
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> {
                        // For dying creatures: use base state (they're already in graveyard)
                        // Face-down permanents are 2/2 creatures (Rule 708.2) and count.
                        isFaceDown || typeLine?.isCreature == true
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent -> {
                        // LKI-safe permanent check. Anything that left the battlefield was, by
                        // definition, a permanent — but a *token* is swept from the game by 704.5d
                        // before this matcher runs, so its live CardComponent is gone and the generic
                        // `else` branch below would return false. Read the last-known type line
                        // (captured on the event, like IsCreature/IsLand above) instead. Without this,
                        // "whenever another permanent you control leaves the battlefield" (Suki,
                        // Courageous Rescuer) silently misses a leaving token.
                        isFaceDown || typeLine?.isPermanent == true
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand ->
                        typeLine?.isLand == true
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact ->
                        typeLine?.isArtifact == true
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment ->
                        typeLine?.isEnchantment == true
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype -> {
                        // For entering creatures: use projected state (they're on battlefield)
                        // For dying creatures: use base state (they're in graveyard, no projected subtypes)
                        if (event.toZone == Zone.BATTLEFIELD) {
                            projected.hasSubtype(event.entityId, predicate.subtype.value)
                        } else {
                            !isFaceDown && typeLine?.hasSubtype(predicate.subtype) == true
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasAnyOfSubtypes -> {
                        // Same LKI handling as HasSubtype, for the "any one of these subtypes"
                        // (OR) form used by the Outlaw subtype group. For dying/leaving creatures
                        // the entity (and its CardComponent) may already be gone, so read the
                        // last-known type line rather than the generic cardComponent path.
                        if (event.toZone == Zone.BATTLEFIELD) {
                            predicate.subtypes.any { projected.hasSubtype(event.entityId, it.value) }
                        } else {
                            !isFaceDown && typeLine != null && predicate.subtypes.any { typeLine.hasSubtype(it) }
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKeyword -> {
                        // For entering creatures: use projected state (they're on battlefield)
                        // For dying/leaving creatures: use lastKnownKeywords from the event since
                        // the creature is no longer on the battlefield and projected state won't
                        // have its keywords (e.g., Jackdaw Savior: "whenever a creature you control
                        // with flying dies").
                        if (event.fromZone == Zone.BATTLEFIELD && event.lastKnown?.keywords?.isNotEmpty() == true) {
                            predicate.keyword.name in (event.lastKnown?.keywords ?: emptySet())
                        } else {
                            projected.hasKeyword(event.entityId, predicate.keyword)
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasChosenSubtype -> {
                        // Resolve the chosen subtype from the trigger source's CastChoicesComponent
                        // and check that the entering/leaving entity has that subtype.
                        val chosenType = state.getEntity(sourceId)
                            ?.chosenCreatureType()
                            ?: return false
                        val hasSubtype = if (event.toZone == Zone.BATTLEFIELD) {
                            projected.hasSubtype(event.entityId, chosenType)
                        } else {
                            typeLine?.subtypes?.any { it.value.equals(chosenType, ignoreCase = true) } == true
                        }
                        val isChangelingCreatureType = cardComponent != null &&
                            Keyword.CHANGELING in cardComponent.baseKeywords &&
                            chosenType in Subtype.ALL_CREATURE_TYPES
                        hasSubtype || isChangelingCreatureType
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken -> {
                        // Token-ness is intrinsic; LKI is required because 704.5d sweeps the
                        // token entity before the matcher runs on death triggers, and the
                        // entity may also be unreachable after a bounce/exile by the time
                        // we check. For enter-battlefield events the entity is still in state
                        // and lastKnownWasToken is not populated — read TokenComponent live.
                        val isToken = if (event.fromZone == Zone.BATTLEFIELD) {
                            event.lastKnown?.wasToken == true
                        } else {
                            entity?.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() == true
                        }
                        !isToken
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken -> {
                        if (event.fromZone == Zone.BATTLEFIELD) {
                            event.lastKnown?.wasToken == true
                        } else {
                            entity?.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() == true
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Or ->
                        predicate.predicates.any { matchesLkiPredicate(it) }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.And ->
                        predicate.predicates.all { matchesLkiPredicate(it) }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Not ->
                        !matchesLkiPredicate(predicate.predicate)
                    else -> {
                        // For other predicates, check the entity's type
                        if (cardComponent == null) return false
                        matchesCardPredicate(
                            predicate, cardComponent, projected, event.entityId, isFaceDown,
                            lastKnownPower = event.lastKnown?.power,
                            lastKnownToughness = event.lastKnown?.toughness,
                            lastKnownWasToken = event.lastKnown?.wasToken == true
                        )
                    }
                }
            }

            for (predicate in trigger.filter.cardPredicates) {
                if (!matchesLkiPredicate(predicate)) return false
            }
            // Check state predicates (face-down, tapped, etc.)
            for (predicate in trigger.filter.statePredicates) {
                if (!matchesStatePredicateForZoneChangeTrigger(predicate, state, event)) return false
            }
            // Check controller predicate. Control predicates (youControl) prefer the dying
            // creature's last-known controller over its owner so stolen creatures (Threaten /
            // Mind Control) and tokens whose creator owns them but doesn't control them group
            // correctly. Owner predicates (ownedByYou) instead read the card's owner: "put into
            // YOUR graveyard from the battlefield" cares about ownership, since a permanent
            // always goes to its owner's graveyard regardless of who last controlled it
            // (CR 400.3, e.g. Soulcatchers' Aerie).
            trigger.filter.controllerPredicate?.let { pred ->
                val effectiveController = event.lastKnown?.controllerId ?: event.ownerId
                val controllerMatches = pred.evaluateWith { leaf ->
                    when (leaf) {
                        is ControllerPredicate.ControlledByYou -> effectiveController == controllerId
                        is ControllerPredicate.ControlledByOpponent -> effectiveController != controllerId
                        is ControllerPredicate.OwnedByYou -> event.ownerId == controllerId
                        is ControllerPredicate.OwnedByOpponent -> event.ownerId != controllerId
                        else -> null // leaf kinds this LKI site can't evaluate don't constrain
                    }
                }
                if (!controllerMatches) return false
            }
        }

        return true
    }

    fun matchesCardPredicate(
        predicate: com.wingedsheep.sdk.scripting.predicates.CardPredicate,
        cardComponent: CardComponent,
        projected: ProjectedState,
        entityId: EntityId,
        isFaceDown: Boolean = false,
        lastKnownPower: Int? = null,
        lastKnownToughness: Int? = null,
        /**
         * Token-ness for zone-change triggers where the entity may already be gone
         * (CR 704.5s sweeps tokens out of non-battlefield zones). Pass [ZoneChangeEvent.lastKnown?.wasToken == true]
         * so `IsNontoken` / `IsToken` can still resolve. When `null`, the token bit is read
         * from the base state via the projected snapshot (entity must still exist).
         */
        lastKnownWasToken: Boolean? = null
    ): Boolean {
        fun isToken(): Boolean {
            if (lastKnownWasToken != null) return lastKnownWasToken
            val entity = projected.getBaseState().getEntity(entityId) ?: return false
            return entity.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()
        }
        return when (predicate) {
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> cardComponent.typeLine.isCreature
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand -> cardComponent.typeLine.isLand
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> cardComponent.typeLine.isArtifact
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment -> cardComponent.typeLine.isEnchantment
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPlaneswalker -> com.wingedsheep.sdk.core.CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsInstant -> cardComponent.typeLine.isInstant
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsSorcery -> cardComponent.typeLine.isSorcery
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsBasicLand -> cardComponent.typeLine.isLand && cardComponent.typeLine.supertypes.contains(com.wingedsheep.sdk.core.Supertype.BASIC)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent -> cardComponent.typeLine.isPermanent
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !cardComponent.typeLine.isLand
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature -> !cardComponent.typeLine.isCreature
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonenchantment -> !cardComponent.typeLine.isEnchantment
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonartifact -> !cardComponent.typeLine.isArtifact
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLegendary -> cardComponent.typeLine.isLegendary
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonlegendary -> !cardComponent.typeLine.isLegendary
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                cardComponent.typeLine.hasSubtype(predicate.subtype)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasAnyOfSubtypes ->
                predicate.subtypes.any { cardComponent.typeLine.hasSubtype(it) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.NotSubtype ->
                !cardComponent.typeLine.hasSubtype(predicate.subtype)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtLeast -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMost -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc <= predicate.max
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostX -> false
            // Resolution-time chosen-number predicate; TriggerMatcher has no chosen-number context.
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEqualsX -> false
            // Entity-relative — TriggerMatcher has no entity context; predicate doesn't apply here.
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostEntityManaSpent -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerGreaterThanEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtMostEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerLessThanEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.SharesColorWithPermanentYouControl -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEquals -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc == predicate.value
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueIsEven -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc % 2 == 0
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueIsOdd -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc % 2 != 0
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasXInManaCost ->
                // Printed cost's {X} symbol, not the computed CMC. Face-down has no mana cost.
                if (isFaceDown) false else cardComponent.manaCost.hasX
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtLeast -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtMost -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerEquals -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power == predicate.value
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessAtLeast -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessAtMost -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness <= predicate.max
            }
            // Resolution-time only — TriggerMatcher has no X context, so the predicate never matches here.
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessAtMostX -> false
            // Resolution-time chosen-number predicate; TriggerMatcher has no chosen-number context.
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerEqualsX -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessEquals -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness == predicate.value
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerOrToughnessAtLeast -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                power >= predicate.min || toughness >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerOrToughnessAtMost -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                power <= predicate.max || toughness <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.TotalPowerAndToughnessAtMost -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                (power + toughness) <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessGreaterThanPower -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness > power
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerGreaterThanBase -> {
                // Self-relative: last-known/projected power at event time vs the object's own
                // printed base power. Face-down (base 2/2, power 2) never exceeds its base.
                if (isFaceDown) false
                else {
                    val basePower = cardComponent.baseStats?.basePower
                    val power = lastKnownPower ?: projected.getPower(entityId) ?: basePower
                    basePower != null && power != null && power > basePower
                }
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKeyword ->
                projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.NotKeyword ->
                !projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken -> isToken()
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken -> !isToken()
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Or ->
                predicate.predicates.any { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.And ->
                predicate.predicates.all { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Not ->
                !matchesCardPredicate(predicate.predicate, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken)
            else -> error(
                "TriggerMatcher.matchesCardPredicate has no branch for ${predicate::class.simpleName}. " +
                "Add an explicit branch — silent pass-through hid the IsNontoken bug for months."
            )
        }
    }

    /**
     * Check if a trigger event matches a death trigger pattern (ZoneChangeEvent from battlefield to graveyard).
     */
    fun isDeathTrigger(trigger: EventPattern): Boolean {
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == Zone.GRAVEYARD
    }

    /**
     * Check if a trigger event matches a leaves-battlefield pattern (ZoneChangeEvent from battlefield, to=null).
     */
    fun isLeavesBattlefieldTrigger(trigger: EventPattern): Boolean {
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == null
    }

    /**
     * Check if a trigger fires for a specific battlefield-exit destination
     * (e.g., "When this is exiled from the battlefield" → trigger.to = EXILE).
     *
     * Excludes [Zone.GRAVEYARD] — death triggers are handled by [isDeathTrigger]
     * and [DeathAndLeaveTriggerDetector.detectDeathTriggers], so matching here
     * would double-fire. Generic "leaves the battlefield" triggers (to = null)
     * are matched by [isLeavesBattlefieldTrigger] instead.
     */
    fun isLeavesBattlefieldToZoneTrigger(trigger: EventPattern, eventToZone: Zone): Boolean {
        if (eventToZone == Zone.GRAVEYARD) return false
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == eventToZone
    }

    /**
     * Check binding for AttackEvent: SELF means sourceId must be in attackers.
     */
    fun checkBinding(binding: TriggerBinding, sourceId: EntityId, entityIds: List<EntityId>): Boolean {
        return when (binding) {
            TriggerBinding.SELF -> sourceId in entityIds
            TriggerBinding.OTHER -> true  // "whenever another creature attacks" (not currently used, but correct)
            TriggerBinding.ANY -> true
            TriggerBinding.ATTACHED -> false // handled by AttachmentTriggerDetector
        }
    }

    /**
     * Check if a Player filter matches the event's player.
     */
    fun matchesPlayer(player: Player, eventPlayerId: EntityId, controllerId: EntityId): Boolean {
        return when (player) {
            Player.You -> eventPlayerId == controllerId
            Player.Each -> true
            Player.EachOpponent -> eventPlayerId != controllerId
            else -> true
        }
    }

    /**
     * Extract a subtype value from a GameObjectFilter (used for DealsDamageEvent.sourceFilter).
     */
    fun extractSubtypeFromFilter(filter: GameObjectFilter): String? {
        for (predicate in filter.cardPredicates) {
            if (predicate is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype) {
                return predicate.subtype.value
            }
        }
        return null
    }

    /**
     * Check if a BecomesTargetEvent matches a BecomesTargetEvent trigger.
     * Validates binding (SELF = targeted entity is this permanent) and
     * checks the targetFilter against the targeted entity.
     */
    fun matchesBecomesTargetTrigger(
        trigger: EventPattern.BecomesTargetEvent,
        binding: TriggerBinding,
        event: BecomesTargetEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        // Check binding: SELF means this permanent was targeted
        when (binding) {
            TriggerBinding.SELF -> if (event.targetEntityId != sourceId) return false
            TriggerBinding.OTHER -> if (event.targetEntityId == sourceId) return false
            TriggerBinding.ANY -> { /* no entity restriction */ }
            TriggerBinding.ATTACHED -> return false // handled by AttachmentTriggerDetector
        }

        // Spell targets fire only for triggers that opted in ("... or a creature spell you control",
        // e.g. Surrak). Permanent-only wording ("a creature you control" — Pawpatch Recruit, Daru
        // Spiritualist) must not react to a creature spell on the stack being targeted.
        if (event.targetIsSpell && !trigger.includeSpellTargets) return false

        // "Becomes the target of a spell" (King of the Oathbreakers) ignores abilities.
        if (trigger.spellsOnly && !event.sourceIsSpell) return false

        // Valiant: check if the targeting spell/ability is controlled by "you" (the trigger's controller)
        if (trigger.byYou && event.controllerId != controllerId) return false

        // Check if the targeting spell/ability is controlled by an opponent
        if (trigger.byOpponent && event.controllerId == controllerId) return false

        // Valiant: check if this is the first time this turn
        if (trigger.firstTimeEachTurn && !event.firstTimeByThisController) return false

        // Check targetFilter against the targeted entity. The target is live (a battlefield
        // permanent, or a spell on the stack for includeSpellTargets), so evaluate the whole
        // filter through PredicateEvaluator: card predicates read projected types when the
        // entity has a battlefield projection — an animated land IS "a creature you control"
        // while the effect lasts — and fall back to base card data for stack objects; the
        // controller predicate likewise falls back to the spell's caster (Surrak, Elusive
        // Hunter). Targeted abilities on the stack carry no card data and never match.
        if (trigger.targetFilter != GameObjectFilter.Any) {
            val targetContainer = state.getEntity(event.targetEntityId) ?: return false
            if (!targetContainer.has<CardComponent>()) return false
            val predicateContext = PredicateContext(controllerId = controllerId, sourceId = sourceId)
            if (!PredicateEvaluator().matches(
                    state, state.projectedState, event.targetEntityId, trigger.targetFilter, predicateContext
                )) return false
        }

        return true
    }

    fun matchesDealsDamageTrigger(
        trigger: EventPattern.DealsDamageEvent,
        event: DamageDealtEvent,
        state: GameState,
        controllerId: EntityId? = null
    ): Boolean {
        val combatMatches = trigger.damageType == DamageType.Any ||
            (trigger.damageType == DamageType.Combat && event.isCombatDamage) ||
            (trigger.damageType == DamageType.NonCombat && !event.isCombatDamage)
        val recipientMatches = when (trigger.recipient) {
            RecipientFilter.Any -> true
            RecipientFilter.AnyPlayer -> event.targetId in state.turnOrder
            RecipientFilter.AnyPlayerOrPlaneswalker -> {
                event.targetId in state.turnOrder ||
                    state.projectedState.isPlaneswalker(event.targetId)
            }
            RecipientFilter.AnyCreature -> event.targetId !in state.turnOrder
            RecipientFilter.You -> false // handled separately in detectDamageToControllerTriggers
            RecipientFilter.Opponent -> {
                event.targetId in state.turnOrder && event.targetId != controllerId
            }
            RecipientFilter.CreatureOpponentControls -> {
                val targetController = recipientControllerLki(event, state)
                recipientIsCreatureLki(event, state) && targetController != null && targetController != controllerId
            }
            RecipientFilter.CreatureYouControl -> {
                recipientIsCreatureLki(event, state) && recipientControllerLki(event, state) == controllerId
            }
            RecipientFilter.PermanentYouControl -> {
                recipientControllerLki(event, state) == controllerId
            }
            RecipientFilter.AnyPermanent -> event.targetId !in state.turnOrder
            RecipientFilter.Self -> false // handled elsewhere
            else -> false
        }
        // Check sourceFilter (e.g., "creature you control" for Gossip's Talent)
        val sourceMatches = if (trigger.sourceFilter != null && event.sourceId != null) {
            val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = controllerId ?: EntityId(""),
                sourceId = null
            )
            predicateEvaluator.matches(
                state, state.projectedState, event.sourceId, trigger.sourceFilter!!, predicateContext
            )
        } else {
            true
        }
        // "Excess damage" triggers (Fall of Cair Andros) fire only when the recipient took
        // damage past lethal — CR 120.4a. Non-creature targets and at-or-below-lethal hits leave
        // event.excessAmount at 0 and silently fail this gate.
        val excessMatches = !trigger.requireExcess || event.excessAmount > 0
        return combatMatches && recipientMatches && sourceMatches && excessMatches
    }

    /**
     * The damage recipient's controller, preferring live state but falling back to the controller
     * captured at damage time ([DamageDealtEvent.targetControllerId]). A creature destroyed by the
     * same damage event has already moved to the graveyard — losing its [ControllerComponent] — by
     * the time combat-damage triggers are detected, so without the LKI fallback a recipient-based
     * trigger ("a creature you control / an opponent controls is dealt damage") would silently miss
     * the killing blow (CR 603.10).
     */
    private fun recipientControllerLki(event: DamageDealtEvent, state: GameState): EntityId? =
        state.getEntity(event.targetId)?.get<ControllerComponent>()?.playerId
            ?: event.targetControllerId

    /** Whether the damage recipient was a creature, with the same LKI fallback as [recipientControllerLki]. */
    private fun recipientIsCreatureLki(event: DamageDealtEvent, state: GameState): Boolean =
        state.getEntity(event.targetId)?.get<CardComponent>()?.typeLine?.isCreature == true ||
            event.targetWasCreature

    fun matchesStepTrigger(
        trigger: EventPattern,
        step: Step,
        controllerId: EntityId,
        state: GameState,
        sourceId: EntityId? = null
    ): Boolean {
        if (trigger !is EventPattern.StepEvent) return false
        if (step != trigger.step) return false
        return matchesPlayerForStep(trigger.player, controllerId, state, sourceId)
    }

    /**
     * Whether a step trigger owned by [controllerId] fires this step. "Your" step is the *team's*
     * step in Two-Headed Giant (CR 805.4d): every member of the active team sees their own "at the
     * beginning of your upkeep/draw/end step" trigger fire, and a nonactive player's "each opponent's
     * step" trigger fires while the opposing team is active. In a non-team game this reduces to the
     * ordinary active-player comparison. (The 805.4d per-opponent *multiplicity* nuance — firing once
     * per opposing teammate — is not yet modelled; the trigger fires once.)
     *
     * [Player.ChosenOpponent] keys the step to the opponent stored on the source permanent when
     * it entered (`ChoiceSlot.OPPONENT`, via [com.wingedsheep.sdk.scripting.EntersWithChoice]) —
     * The Rack: "at the beginning of the chosen player's upkeep". The trigger fires only on that
     * player's step; without [sourceId] (or before a choice is recorded) it can't resolve and
     * doesn't fire, rather than firing on every player's step.
     */
    fun matchesPlayerForStep(
        player: Player,
        controllerId: EntityId,
        state: GameState,
        sourceId: EntityId? = null
    ): Boolean {
        return when (player) {
            Player.You -> state.isActiveTurnFor(controllerId)
            Player.Each -> true
            Player.EachOpponent -> !state.isActiveTurnFor(controllerId)
            Player.ChosenOpponent -> {
                val chosen = sourceId?.let { state.getEntity(it)?.chosenOpponent() } ?: return false
                state.isActiveTurnFor(chosen)
            }
            else -> true
        }
    }

    fun matchesSpellFilter(
        spellFilter: GameObjectFilter,
        event: SpellCastEvent,
        state: GameState,
        triggerSourceId: EntityId? = null
    ): Boolean {
        // No card predicates = match any spell (equivalent to old SpellTypeFilter.ANY)
        if (spellFilter.cardPredicates.isEmpty()) return true

        val container = state.getEntity(event.spellEntityId) ?: return false

        // Face-down spells have no characteristics (CR 708.2) — they don't match any type filter
        val isFaceDown = container.get<SpellOnStackComponent>()?.castFaceDown == true
        if (isFaceDown) return false

        // Use base-state matching (spells on the stack don't get continuous effects)
        // Pass sourceId so HasChosenSubtype can read the trigger source's CastChoicesComponent
        val context = com.wingedsheep.engine.handlers.PredicateContext(
            controllerId = event.casterId,
            sourceId = triggerSourceId
        )
        return predicateEvaluator.matches(state, state.projectedState, event.spellEntityId, spellFilter, context)
    }

    /**
     * Check whether the spell was cast from the zone required by the trigger
     * (e.g., HAND for "Whenever you cast a spell from your hand"). When the
     * trigger doesn't specify a zone, every cast matches.
     */
    /**
     * Resolve one [SpellCastPredicate] against the runtime [SpellCastEvent].
     *
     * Add a new branch here when extending [SpellCastPredicate] with a new
     * cast-time fact. The matcher is conjunctive — every predicate the
     * trigger declares must hold.
     *
     * [SpellCastPredicate.PaidWithManaFromSubtype] matches against
     * [SpellCastEvent.spentManaSubtypes] (any producing-source subtype, snapshotted at production);
     * [SpellCastPredicate.PaidWithManaFromSource] matches the trigger's own source against
     * [SpellCastEvent.spentManaSourceIds]. See [ManaPoolComponent.manaBySubtype].
     */
    /**
     * "Whenever you activate an ability that targets [a creature or player]" — true when the
     * activated ability on the stack ([abilityEntityId]) has at least one chosen target satisfying
     * [match]. The ability's chosen targets live on its [TargetsComponent]; a non-targeting ability
     * has no such component, so this returns false (the trigger doesn't fire).
     */
    private fun matchesAbilityTargetConstraint(
        abilityEntityId: EntityId?,
        match: AbilityTargetMatch,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        if (abilityEntityId == null) return false
        val targets = state.getEntity(abilityEntityId)?.get<TargetsComponent>()?.targets ?: return false
        return targets.any { target -> matchesAbilityTarget(target, match, sourceId, controllerId, state) }
    }

    /** True if a single chosen target satisfies the [match] constraint. */
    private fun matchesAbilityTarget(
        target: ChosenTarget,
        match: AbilityTargetMatch,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean = when (match) {
        is AbilityTargetMatch.AnyPlayer -> target is ChosenTarget.Player
        is AbilityTargetMatch.AnyOf -> match.options.any {
            matchesAbilityTarget(target, it, sourceId, controllerId, state)
        }
        is AbilityTargetMatch.ObjectMatching -> {
            val objectId = when (target) {
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Card -> target.cardId
                is ChosenTarget.Spell -> target.spellEntityId
                is ChosenTarget.Player -> null
            }
            if (objectId == null) false
            else {
                val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                    controllerId = controllerId,
                    sourceId = sourceId
                )
                predicateEvaluator.matches(
                    state, state.projectedState, objectId, match.filter, predicateContext
                )
            }
        }
    }

    /**
     * Resolve one [AttackPredicate] against the runtime [AttackersDeclaredEvent].
     *
     * Add a new branch here when extending [AttackPredicate] with a new
     * attack-time fact. The matcher is conjunctive — every predicate the
     * trigger declares must hold.
     *
     * [state] is threaded for predicates that must read **projected** creature stats across the
     * attacking band (Training's [AttackPredicate.AttackedAlongsideGreaterPower]); count-only and
     * stamped-set predicates ignore it.
     */
    internal fun matchesAttackPredicate(
        predicate: AttackPredicate,
        event: AttackersDeclaredEvent,
        boundEntityId: EntityId,
        state: GameState
    ): Boolean = when (predicate) {
        AttackPredicate.Alone -> event.attackers.size == 1
        is AttackPredicate.AttackerCountAtLeast -> event.attackers.size >= predicate.n
        // Per-attacker: the trigger's own source (SELF / the attached creature) is among the
        // creatures attacking for the first time this turn. Stamped on the event at declaration
        // (post-declaration state can't tell, since the per-turn attacker set already includes it).
        AttackPredicate.FirstTimeEachTurn -> boundEntityId in event.firstTimeAttackers
        // Per-attacker: the trigger's own source was declared as attacking a player (not a
        // planeswalker or battle). Stamped on the event at declaration (CR 508.1 defender kind).
        AttackPredicate.DefenderIsPlayer -> boundEntityId in event.attackersAgainstPlayer
        // Training (CR 702.149a): the source attacked, and at least one *other* declared attacker
        // has strictly greater PROJECTED power (Rule 613 layers — so an anthem/aura on the other
        // attacker counts). Read every power through the cached projected state, never raw
        // CreatureStatsComponent, per the projected-state load-bearing rule.
        AttackPredicate.AttackedAlongsideGreaterPower -> {
            if (boundEntityId !in event.attackers) {
                false
            } else {
                val projected = state.projectedState
                val myPower = projected.getPower(boundEntityId) ?: 0
                event.attackers.any { other ->
                    other != boundEntityId && (projected.getPower(other) ?: 0) > myPower
                }
            }
        }
    }

    private fun matchesSpellCastPredicate(
        predicate: SpellCastPredicate,
        event: SpellCastEvent,
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        controllerId: com.wingedsheep.sdk.model.EntityId
    ): Boolean = when (predicate) {
        is SpellCastPredicate.CastFromZone -> {
            val spellComponent = state.getEntity(event.spellEntityId)?.get<SpellOnStackComponent>()
            spellComponent?.castFromZone == predicate.zone
        }
        is SpellCastPredicate.CastFromZoneOtherThan -> {
            val spellComponent = state.getEntity(event.spellEntityId)?.get<SpellOnStackComponent>()
            val from = spellComponent?.castFromZone
            from != null && from != predicate.zone
        }
        SpellCastPredicate.WasKicked -> event.wasKicked
        is SpellCastPredicate.PaidWithManaFromSubtype -> predicate.subtype in event.spentManaSubtypes
        is SpellCastPredicate.PaidWithManaFromSource -> sourceId in event.spentManaSourceIds
        SpellCastPredicate.IsModal -> event.chosenModesCount > 0
        SpellCastPredicate.HasXInCost ->
            state.getEntity(event.spellEntityId)?.get<CardComponent>()?.manaCost?.hasX == true
        SpellCastPredicate.TargetsSource -> castTargetEntities(event, state).contains(sourceId)
        is SpellCastPredicate.TargetsMatching -> {
            val predicateEvaluator = PredicateEvaluator()
            val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = controllerId,
                sourceId = sourceId
            )
            castTargetEntities(event, state).any { targetId ->
                predicateEvaluator.matches(
                    state, state.projectedState, targetId, predicate.filter, predicateContext
                )
            }
        }
        SpellCastPredicate.NotOwnedByController -> {
            val ownerId = state.getEntity(event.spellEntityId)
                ?.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()
                ?.playerId
            ownerId != null && ownerId != controllerId
        }
    }

    /** Permanent/spell entity ids chosen as targets by the just-cast spell. */
    private fun castTargetEntities(
        event: SpellCastEvent,
        state: GameState
    ): List<com.wingedsheep.sdk.model.EntityId> {
        val targets = state.getEntity(event.spellEntityId)
            ?.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()
            ?.targets ?: return emptyList()
        return targets.mapNotNull { t ->
            when (t) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> t.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> t.spellEntityId
                else -> null
            }
        }
    }

    /**
     * Filter out triggers whose intervening-if condition (Rule 603.4) is not met.
     * If a triggered ability has a triggerCondition, it is only allowed to fire
     * when that condition is true at the time of trigger detection.
     */
    fun filterByTriggerCondition(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        return triggers.filter { trigger ->
            val condition = trigger.ability.triggerCondition ?: return@filter true
            val context = EffectContext(
                sourceId = trigger.sourceId,
                controllerId = trigger.controllerId,
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggeringPlayerId = trigger.triggerContext.triggeringPlayerId,
                triggerDamageAmount = trigger.triggerContext.damageAmount,
                triggerCounterCount = trigger.triggerContext.counterCount,
                triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
                triggerMinusOneMinusOneCounterCount = trigger.triggerContext.minusOneMinusOneCounterCount,
                triggerLastKnownSubtypes = trigger.triggerContext.lastKnownSubtypes,
                triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                triggerDiedBatchTotalPower = trigger.triggerContext.diedBatchTotalPower,
                triggerScryCount = trigger.triggerContext.scryCount,
                triggerDiscoverValue = trigger.triggerContext.discoverValue,
                triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount,
                triggerRecipientToughness = trigger.triggerContext.recipientToughnessAtDamage,
                triggerManaSpentOnTriggeringSpell = trigger.triggerContext.manaSpentOnTriggeringSpell,
                triggerColorsSpentOnTriggeringSpell = trigger.triggerContext.colorsSpentOnTriggeringSpell,
                triggerManaValueOfTriggeringSpell = trigger.triggerContext.manaValueOfTriggeringSpell,
                triggerXValueOfTriggeringSpell = trigger.triggerContext.xValueOfTriggeringSpell
            )
            conditionEvaluator.evaluate(state, condition, context)
        }
    }

    /**
     * Sort triggers by APNAP order.
     * Active player's triggers go on the stack first (resolve last),
     * then non-active players in turn order.
     */
    fun sortByApnapOrder(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        val activePlayerId = state.activePlayerId ?: return triggers

        // Group by controller
        val byController = triggers.groupBy { it.controllerId }

        // Get player order starting from active player
        val playerOrder = state.turnOrder.let { players ->
            val activeIndex = players.indexOf(activePlayerId)
            if (activeIndex == -1) players
            else players.drop(activeIndex) + players.take(activeIndex)
        }

        // Build result in APNAP order
        // Active player's triggers first (they go on stack first = resolve last)
        return playerOrder.flatMap { playerId ->
            byController[playerId] ?: emptyList()
        }
    }

    /**
     * Evaluate a [StatePredicate] in the zone-change trigger gating path, with last-known
     * info taken from the event so the dying / leaving entity can still be evaluated after
     * it has left the battlefield. Falls back to [matchesStatePredicateForTrigger] for
     * predicates that don't need event-side LKI.
     */
    private fun matchesStatePredicateForZoneChangeTrigger(
        predicate: com.wingedsheep.sdk.scripting.predicates.StatePredicate,
        state: GameState,
        event: ZoneChangeEvent
    ): Boolean = when (predicate) {
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasGreatestPower -> {
            // For permanents leaving the battlefield we need the dying entity's power +
            // controller from the event's last-known info (the entity is no longer on the
            // battlefield by the time the trigger gates). For ETB triggers (to=BATTLEFIELD)
            // the entity is live and the projected path applies.
            val leavingBattlefield = event.fromZone == Zone.BATTLEFIELD
            if (leavingBattlefield) {
                val dyingPower = event.lastKnown?.power
                val dyingController = event.lastKnown?.controllerId ?: event.ownerId
                if (dyingPower == null) false
                else {
                    val projected = state.projectedState
                    val maxSurvivorPower = state.getBattlefield()
                        .filter { id ->
                            projected.getController(id) == dyingController && projected.isCreature(id)
                        }
                        .maxOfOrNull { projected.getPower(it) ?: Int.MIN_VALUE }
                    // Singleton case (no survivors under same controller): the dying
                    // creature is trivially its own maximum and qualifies.
                    maxSurvivorPower == null || dyingPower >= maxSurvivorPower
                }
            } else {
                matchesStatePredicateForTrigger(predicate, state, event.entityId)
            }
        }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasCounter -> {
            // The dying / leaving entity is no longer on the battlefield, so its live counters are
            // gone; gate against the counters captured on the event (LKI). For non-leave triggers
            // (e.g. ETB, to=BATTLEFIELD) the entity is live, so read its current counters.
            if (event.fromZone == Zone.BATTLEFIELD) {
                (event.lastKnown?.counters ?: emptyMap()).any { (type, count) ->
                    count > 0 && counterTypesMatch(predicate.counterType, type)
                }
            } else {
                val counters = state.getEntity(event.entityId)?.get<CountersComponent>()
                counters?.counters?.entries?.any { (type, count) ->
                    count > 0 && counterTypesMatch(predicate.counterType, counterTypeToString(type))
                } ?: false
            }
        }
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasAnyCounter -> {
            if (event.fromZone == Zone.BATTLEFIELD) {
                (event.lastKnown?.totalCounters ?: 0) > 0
            } else {
                val counters = state.getEntity(event.entityId)?.get<CountersComponent>()
                counters?.counters?.values?.any { it > 0 } ?: false
            }
        }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Or ->
            predicate.predicates.any { matchesStatePredicateForZoneChangeTrigger(it, state, event) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.And ->
            predicate.predicates.all { matchesStatePredicateForZoneChangeTrigger(it, state, event) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Not ->
            !matchesStatePredicateForZoneChangeTrigger(predicate.predicate, state, event)
        else -> matchesStatePredicateForTrigger(predicate, state, event.entityId)
    }

    private fun matchesStatePredicateForTrigger(
        predicate: com.wingedsheep.sdk.scripting.predicates.StatePredicate,
        state: GameState,
        entityId: EntityId
    ): Boolean = when (predicate) {
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsFaceDown -> {
            val entity = state.getEntity(entityId) ?: return false
            entity.has<FaceDownComponent>()
        }
        // Graveyard-zone-only predicate; trigger gating never sees a stamped entity here.
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn -> false
        // No granter context in trigger gating — granter-relative exclusion is resolution-time only.
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsGrantingPermanent -> false
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Or ->
            predicate.predicates.any { matchesStatePredicateForTrigger(it, state, entityId) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.And ->
            predicate.predicates.all { matchesStatePredicateForTrigger(it, state, entityId) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Not ->
            !matchesStatePredicateForTrigger(predicate.predicate, state, entityId)
        // Trigger-matching predicates beyond IsFaceDown are not currently used as
        // *trigger-gating* filters (those evaluate the triggering entity, not the source
        // state). Returning true preserves the prior "don't gate" behavior, but listing
        // every variant forces a compile-time choice when a new predicate is added.
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasLockedDoor,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsTapped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsUntapped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsAttacking,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlocking,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlocked,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsUnblocked,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.InSameBandAsSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlockingSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.CreatedBySource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.EnteredThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.WasDealtDamageThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasDealtDamage,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasDealtCombatDamageToPlayer,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.DealtCombatDamageToSourceControllerThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.AttackedThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.AttackedThisCombat,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.BlockedThisCombat,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsFaceUp,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasMorphAbility,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsRingBearer,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasGreatestPower,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasLeastPowerAmongAllCreatures,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasLeastPower,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEquipped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsModified,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsSaddled,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.CrewedOrSaddledSourceThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.CrewedOrSaddledBySourceThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsWarpExiled,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.NotTargetedByAbilityFromSameNamedSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsAttachedToBySource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsAttachedToSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.ExiledWithSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.WasCastForWarp,
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.WasCastFromZone,
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.AttachedToCardType -> true
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.AttachedTo -> true
        // Counter predicates require last-known-info to evaluate a creature that has already left
        // the battlefield; the zone-change path ([matchesStatePredicateForZoneChangeTrigger])
        // handles them against the event's captured counters. This entity-only fallback has no LKI,
        // so it fails closed rather than fail-open (which would create tokens for counter-less deaths).
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasCounter,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasAnyCounter -> false
    }

    /**
     * Compare counter type strings, normalizing different representations to allow matching
     * between trigger specs (e.g., "+1/+1") and event strings (which may be "+1/+1",
     * "plus_one_plus_one", "PLUS_ONE_PLUS_ONE", etc.).
     */
    private fun counterTypesMatch(triggerType: String, eventType: String): Boolean {
        if (triggerType == eventType) return true
        return normalizeCounterType(triggerType) == normalizeCounterType(eventType)
    }

    private fun normalizeCounterType(type: String): String =
        type.lowercase()
            .replace("+1/+1", "plus_one_plus_one")
            .replace("-1/-1", "minus_one_minus_one")
            .replace(" ", "_")
}
