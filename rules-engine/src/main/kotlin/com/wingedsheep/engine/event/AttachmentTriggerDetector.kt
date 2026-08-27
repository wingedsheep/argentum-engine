package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Handles all aura/equipment "enchanted/equipped creature" triggers using the unified
 * [TriggerBinding.ATTACHED] binding. Instead of matching specific attachment event types,
 * this detector checks if general events (DamageDealtEvent, AttackersDeclaredEvent, etc.)
 * match ATTACHED-bound triggers on auras/equipment attached to the event's entity.
 */
class AttachmentTriggerDetector(
    private val abilityResolver: TriggerAbilityResolver,
    private val matcher: TriggerMatcher,
) {

    /**
     * Unified detection for all ATTACHED-bound triggers on battlefield auras/equipment.
     * Extracts relevant entity IDs from the engine event, looks them up in the aurasByTarget
     * index, and checks each ATTACHED trigger against the event.
     */
    fun detectAttachmentTriggers(
        state: GameState,
        event: EngineGameEvent,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        val relevantIds = getRelevantEntityIds(event)
        if (relevantIds.isEmpty()) return

        val isZoneChange = event is ZoneChangeEvent

        for (entityId in relevantIds) {
            val live = index.aurasByTarget[entityId].orEmpty()
            // When the attached permanent left the battlefield, the live links may already be torn
            // down — see [lastKnownAttachments].
            val entries = live + lastKnownAttachments(state, event, index, live)
            for (entry in entries) {
                for (ability in entry.abilities) {
                    if (ability.binding != TriggerBinding.ATTACHED) continue
                    // For zone-change events on the attached creature (e.g., creature dies),
                    // skip auras — they go to graveyard with the creature and are handled by
                    // DeathAndLeaveTriggerDetector.detectDeadAuraAttachmentTriggers via the
                    // aura's own ZoneChangeEvent. Only equipment stays on the battlefield.
                    if (isZoneChange && ability.trigger is EventPattern.ZoneChangeEvent &&
                        !entry.cardComponent.typeLine.isEquipment) continue
                    if (matchesAttachedTrigger(ability.trigger, event, entityId, entry.controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entry.entityId,
                                sourceName = entry.cardComponent.name,
                                controllerId = entry.controllerId,
                                triggerContext = buildTriggerContext(event, entityId)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * The Equipment that was attached to a permanent leaving the battlefield but is no longer
     * linked to it in [TriggerIndex.aurasByTarget] — CR 608.2h last-known information.
     *
     * Needed because the timing of the unattach depends on *how* the host left. A destroy or
     * bounce effect emits the [ZoneChangeEvent] during resolution, and triggers are detected
     * while the link is still live. A death by state-based action does not: `LETHAL_DAMAGE`
     * (CR 704.5g) and `UNATTACHED_AURAS` (CR 704.5m) run in the *same* SBA pass, so by the time
     * the batch's events reach trigger detection the Equipment is already unattached and the
     * index no longer connects it to its dead host. Without this fallback, "whenever equipped
     * creature dies" would fire on removal but silently no-op on combat damage — the far more
     * common way a creature actually dies.
     *
     * Only Equipment is reconstructed: an Aura goes to the graveyard along with its host and is
     * handled from its own [ZoneChangeEvent] by
     * [DeathAndLeaveTriggerDetector.detectDeadAuraAttachmentTriggers].
     *
     * [live] entries are excluded so an attachment the index still knows about is never counted
     * twice.
     */
    private fun lastKnownAttachments(
        state: GameState,
        event: EngineGameEvent,
        index: TriggerIndex,
        live: List<TriggerIndex.IndexedEntity>,
    ): List<TriggerIndex.IndexedEntity> {
        if (event !is ZoneChangeEvent) return emptyList()
        val attachmentIds = event.lastKnown?.attachmentIds.orEmpty()
        if (attachmentIds.isEmpty()) return emptyList()

        val alreadyLive = live.mapTo(mutableSetOf()) { it.entityId }
        val battlefield = state.getBattlefield()
        return attachmentIds.mapNotNull { attachmentId ->
            if (attachmentId in alreadyLive) return@mapNotNull null
            // The Equipment must still be on the battlefield for its ability to function.
            if (attachmentId !in battlefield) return@mapNotNull null
            val container = state.getEntity(attachmentId) ?: return@mapNotNull null
            val cardComponent = container
                .get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?: return@mapNotNull null
            if (!cardComponent.typeLine.isEquipment) return@mapNotNull null
            val controllerId = container
                .get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                ?.playerId ?: return@mapNotNull null
            val abilities = abilityResolver.getTriggeredAbilities(
                attachmentId, cardComponent.cardDefinitionId, state, index.statics
            )
            if (abilities.isEmpty()) return@mapNotNull null
            TriggerIndex.IndexedEntity(
                entityId = attachmentId,
                cardComponent = cardComponent,
                controllerId = controllerId,
                abilities = abilities,
            )
        }
    }

    /**
     * Extract entity IDs from the engine event that should be looked up in aurasByTarget.
     * Returns a list because damage events need to check both source and target.
     */
    private fun getRelevantEntityIds(event: EngineGameEvent): List<com.wingedsheep.sdk.model.EntityId> {
        return when (event) {
            is DamageDealtEvent -> {
                // Check both target (for "enchanted creature/player takes damage") and
                // source (for "enchanted creature deals damage"). The target id is included
                // even when it's a player so "whenever enchanted player is dealt damage"
                // auras (Grievous Wound) fire — aurasByTarget only resolves to player-attached
                // auras for that id, so non-enchant-player damage stays a no-op lookup.
                buildList {
                    add(event.targetId)
                    event.sourceId?.let { add(it) }
                }
            }
            is AttackersDeclaredEvent -> event.attackers
            is TurnFaceUpEvent -> listOf(event.entityId)
            is TappedEvent -> listOf(event.entityId)
            is UntappedEvent -> listOf(event.entityId)
            // "When equipped creature transforms" (Neglected Heirloom). A transform flips the
            // permanent in place, so the Equipment is still attached when the event fires.
            is TransformedEvent -> listOf(event.entityId)
            // "an ability of enchanted artifact … was activated" (Artifact Possession) — the
            // activated ability's source is the enchanted permanent.
            is AbilityActivatedEvent -> listOf(event.sourceId)
            is ZoneChangeEvent -> {
                if (event.fromZone == Zone.BATTLEFIELD) listOf(event.entityId) else emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Check whether an ATTACHED trigger's event type matches the given engine event
     * for a specific attached entity.
     */
    private fun matchesAttachedTrigger(
        trigger: EventPattern,
        event: EngineGameEvent,
        attachedEntityId: com.wingedsheep.sdk.model.EntityId,
        auraControllerId: com.wingedsheep.sdk.model.EntityId,
        state: GameState
    ): Boolean {
        return when (trigger) {
            is EventPattern.DamageReceivedEvent -> {
                event is DamageDealtEvent && event.targetId == attachedEntityId
            }
            is EventPattern.DealsDamageEvent -> {
                event is DamageDealtEvent &&
                    event.sourceId == attachedEntityId &&
                    matcher.matchesDealsDamageTrigger(trigger, event, state, auraControllerId)
            }
            is EventPattern.AttackEvent -> {
                event is AttackersDeclaredEvent && attachedEntityId in event.attackers &&
                    // Apply the trigger's attack-time predicates (e.g. AttackPredicate.Alone for
                    // Bilbo's Ring) — the same conjunctive check the main loop runs at declaration,
                    // which the ATTACHED path must not skip.
                    trigger.requires.all { matcher.matchesAttackPredicate(it, event, attachedEntityId, state) }
            }
            is EventPattern.TapEvent -> {
                if (event !is TappedEvent || event.entityId != attachedEntityId) return false
                // A named tap cause ("… becomes tapped to pay a teamwork cost") narrows the
                // ATTACHED path too; null asks for no particular cause.
                val reason = trigger.reason
                if (reason != null && event.reason != reason) return false
                // "Whenever you tap …" attribution applies on the ATTACHED path too; relative to
                // the aura/equipment's controller, as everywhere else here.
                val tapper = trigger.tapper ?: return true
                val tappedById = event.tappedById ?: return false
                matcher.matchesPlayer(state, tapper, tappedById, auraControllerId)
            }
            // "Whenever equipped creature becomes untapped" (Fishing Pole). UntapEvent carries no
            // filter, so identity with the attached permanent is the whole match.
            is EventPattern.UntapEvent -> {
                event is UntappedEvent && event.entityId == attachedEntityId
            }
            is EventPattern.AbilityActivatedEvent -> {
                if (event !is AbilityActivatedEvent) return false
                if (event.sourceId != attachedEntityId) return false
                if (!matcher.matchesPlayer(state, trigger.player, event.controllerId, auraControllerId)) return false
                // Mirror the main matcher's two wordings (see TriggerMatcher.AbilityActivatedEvent):
                // "without {T} in its activation cost" vs. "isn't a mana ability".
                when {
                    trigger.requireExhaust -> event.isExhaust
                    trigger.requireNoTapInCost -> !event.costsTap
                    else -> !event.isManaAbility
                }
            }
            is EventPattern.TurnFaceUpEvent -> {
                event is TurnFaceUpEvent && event.entityId == attachedEntityId
            }
            // "When equipped creature transforms" (Neglected Heirloom). Identity with the attached
            // permanent plus the pattern's direction filter is the whole match — the same two checks
            // TriggerMatcher applies on the SELF path.
            is EventPattern.TransformEvent -> {
                if (event !is TransformedEvent || event.entityId != attachedEntityId) return false
                trigger.intoBackFace == null || trigger.intoBackFace == event.intoBackFace
            }
            is EventPattern.ZoneChangeEvent -> {
                if (event !is ZoneChangeEvent) return false
                if (event.entityId != attachedEntityId) return false
                if (trigger.from != null && event.fromZone != trigger.from) return false
                if (trigger.to != null && event.toZone != trigger.to) return false
                true
            }
            else -> false
        }
    }

    /**
     * Build trigger context from the engine event, using the attached entity as the triggering entity.
     */
    private fun buildTriggerContext(
        event: EngineGameEvent,
        attachedEntityId: com.wingedsheep.sdk.model.EntityId
    ): TriggerContext {
        return when (event) {
            is DamageDealtEvent -> TriggerContext.fromEvent(event)
            else -> TriggerContext(triggeringEntityId = attachedEntityId)
        }
    }
}
