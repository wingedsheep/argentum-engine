package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EquipmentAttachRestriction

/**
 * The one "attach an Aura or Equipment that is already on the battlefield to a permanent" operation
 * (CR 701.3a), shared by every effect that moves an attachment between hosts.
 */
object AttachmentMover {

    /**
     * Whether the Aura/Equipment [attachmentId] on the battlefield could legally be attached to the
     * permanent [hostId] (CR 701.3a). An Aura must satisfy its printed enchant restriction — "you"
     * meaning the Aura's controller — and the host mustn't have protection from one of its colors
     * (CR 702.16c); an Aura that is also a creature can't enchant anything and can't enchant itself
     * (CR 303.4d). An Equipment can only equip a creature (CR 301.5), not while it is itself a
     * creature without reconfigure (CR 301.5c), not a creature with protection from one of its
     * colors (CR 702.16d), and not one its own [EquipmentAttachRestriction] rules out. Anything else
     * can't be attached at all (CR 701.3b). Reads the projected state so layer-4 type and control
     * changes are seen.
     */
    fun canAttach(
        state: GameState,
        predicateEvaluator: PredicateEvaluator,
        cardRegistry: CardRegistry,
        attachmentId: EntityId,
        hostId: EntityId
    ): Boolean {
        if (attachmentId == hostId) return false
        val battlefield = state.getBattlefield()
        if (attachmentId !in battlefield || hostId !in battlefield) return false
        val card = state.getEntity(attachmentId)?.get<CardComponent>() ?: return false
        val projected = state.projectedState
        if (projected.isBattle(attachmentId)) return false
        return when {
            card.typeLine.isAura -> {
                if (projected.isCreature(attachmentId)) return false
                val controllerId = projected.getController(attachmentId) ?: return false
                EnchantRestriction.couldAttach(
                    state, projected, predicateEvaluator, cardRegistry, attachmentId, card, hostId, controllerId
                )
            }
            card.typeLine.isEquipment ->
                projected.isCreature(hostId) &&
                    !(projected.isCreature(attachmentId) && !projected.hasKeyword(attachmentId, "RECONFIGURE")) &&
                    !EnchantRestriction.hostProtectedFromAttachmentColor(
                        state, projected, cardRegistry, attachmentId, card, hostId
                    ) &&
                    equipRestrictionAllows(state, predicateEvaluator, cardRegistry, attachmentId, hostId)
            else -> false
        }
    }

    /**
     * Whether the Equipment [equipmentId]'s own "can be attached only to …" restrictions
     * ([EquipmentAttachRestriction]) admit [hostId]. True when it has none — including when it is
     * face down or has lost all abilities, since the restriction is a static ability of the Equipment.
     * The host is matched on the projected state with the Equipment as predicate source and its
     * controller as "you". Shared by [canAttach], the equip ability's resolution and the CR 704.5n
     * state-based action, so the three can't disagree about a host.
     */
    fun equipRestrictionAllows(
        state: GameState,
        predicateEvaluator: PredicateEvaluator,
        cardRegistry: CardRegistry,
        equipmentId: EntityId,
        hostId: EntityId
    ): Boolean {
        val container = state.getEntity(equipmentId) ?: return true
        if (container.has<FaceDownComponent>()) return true
        val projected = state.projectedState
        if (projected.hasLostAllAbilities(equipmentId)) return true
        val card = container.get<CardComponent>() ?: return true
        val restrictions = cardRegistry.getCard(card.cardDefinitionId)?.staticAbilities
            ?.filterIsInstance<EquipmentAttachRestriction>()
            .orEmpty()
        if (restrictions.isEmpty()) return true
        val controllerId = projected.getController(equipmentId) ?: return false
        val context = PredicateContext(controllerId = controllerId, sourceId = equipmentId)
        return restrictions.all { predicateEvaluator.matches(state, projected, hostId, it.filter, context) }
    }

    /**
     * Attach [attachmentId] to [hostId], moving it off its current host first. Moving onto a *new*
     * host makes it become unattached from the old one (CR 701.3d), reported through
     * [ZoneMovementUtils.unattachEmittingEvent], and then attached — a [PermanentAttachedEvent] — so
     * "becomes unattached" and "becomes attached" triggers both see the move. Re-attaching to the host
     * it is already on does nothing (CR 701.3b) and emits nothing. Legality is the caller's question:
     * check [canAttach] first where the rules require it.
     */
    fun attach(
        state: GameState,
        attachmentId: EntityId,
        hostId: EntityId,
        fallbackControllerId: EntityId
    ): Pair<GameState, List<GameEvent>> {
        val currentHost = state.getEntity(attachmentId)?.get<AttachedToComponent>()?.targetId
        if (currentHost == hostId) return state to emptyList()

        val events = mutableListOf<GameEvent>()
        var newState = state
        if (currentHost != null) {
            val (detached, unattachEvents) = ZoneMovementUtils.unattachEmittingEvent(newState, attachmentId)
            newState = detached
            events += unattachEvents
        }

        newState = newState.updateEntity(attachmentId) { it.with(AttachedToComponent(hostId)) }
        newState = newState.updateEntity(hostId) { container ->
            val existing = container.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
            container.with(AttachmentsComponent(existing + attachmentId))
        }

        val container = newState.getEntity(attachmentId)
        events += PermanentAttachedEvent(
            attachmentId = attachmentId,
            attachmentName = container?.get<CardComponent>()?.name ?: "",
            attachedToId = hostId,
            controllerId = container?.get<ControllerComponent>()?.playerId ?: fallbackControllerId,
        )
        return newState to events
    }
}
