package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MeldEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [MeldEffect] — "If you both own and control [this] and a [partner], exile them, then
 * meld them into [result]" (CR 701.42, CR 712.4a).
 *
 * The melded permanent is the *source's* entity, wearing the meld result's [CardComponent]; the
 * partner card is pulled out of exile and held zoneless under a [MeldedComponent]. Both exiles and
 * the battlefield entry happen inside this one resolution, so nothing sees the cards in exile
 * between the two steps — but the exile is real: leaves-the-battlefield triggers fire for both, and
 * the result enters as a new object (enters triggers fire, counters and auras are gone).
 */
class MeldEffectExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry
) : EffectExecutor<MeldEffect> {

    override val effectType: KClass<MeldEffect> = MeldEffect::class

    override fun execute(state: GameState, effect: MeldEffect, context: EffectContext): EffectResult {
        val you = context.controllerId
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        // CR 400.7: a meld card that left and came back is a new object the ability can't find.
        if (context.isUnavailableBattlefieldSource(EffectTarget.Self, state)) return EffectResult.success(state)
        val projected = state.projectedState
        val battlefield = state.getBattlefield()

        // "If you both own and control [this] and a [partner]" — checked as the ability resolves.
        fun ownedAndControlledByYou(id: EntityId): Boolean =
            id in battlefield && ownerOf(state, id) == you && projected.getController(id) == you

        if (!ownedAndControlledByYou(sourceId)) return EffectResult.success(state)
        val predicateContext = PredicateContext.fromEffectContext(context)
        // Of several matching partners, prefer a non-token card — the one that can actually meld
        // (CR 701.42b); a token copy is only chosen when it's the sole candidate.
        val partnerId = battlefield.filter { id ->
            id != sourceId && ownedAndControlledByYou(id) &&
                zones.predicateEvaluator.matches(state, projected, id, effect.partner, predicateContext)
        }.minByOrNull { id -> if (state.getEntity(id)?.has<TokenComponent>() == true) 1 else 0 }
            ?: return EffectResult.success(state)

        // "…exile them" — together, as one event.
        val exiled = zones.moveToZoneBatch(state, listOf(sourceId, partnerId), Zone.EXILE)
        val afterExile = exiled.state
        val events = exiled.events.toMutableList()

        // "…then meld them": only the two cards of the result's meld pair can be melded
        // (CR 701.42b); anything else stays in exile (CR 701.42c), and an exiled token ceases to
        // exist through the usual state-based action.
        val result = cardRegistry.getCard(effect.into)
        if (result == null || !canMeld(afterExile, sourceId, partnerId, result)) {
            return EffectResult.success(afterExile, events)
        }

        val hostFront = afterExile.getEntity(sourceId)!!.get<CardComponent>()!!
        val partnerFront = afterExile.getEntity(partnerId)!!.get<CardComponent>()!!
        val partnerOwner = ownerOf(afterExile, partnerId) ?: you

        // CR 712.8g: the melded permanent's mana value is the sum of its front faces'.
        val meldedCard = buildCardComponentForDfcFace(
            hostFront, result, manaValueOverride = hostFront.manaValue + partnerFront.manaValue
        )
        val combined = afterExile
            .removeFromZone(ZoneKey(partnerOwner, Zone.EXILE), partnerId)
            .updateEntity(partnerId) { it.without<ExiledFromZoneComponent>() }
            .updateEntity(sourceId) { c ->
                withFaceIntrinsicComponents(
                    c.with(meldedCard).with(MeldedComponent(partnerId = partnerId, hostFrontCard = hostFront)),
                    result
                )
            }

        val owner = ownerOf(combined, sourceId) ?: you
        val moved = zones.moveToZone(
            combined, sourceId, Zone.BATTLEFIELD,
            options = ZoneEntryOptions(controllerId = owner, tappedAndAttacking = effect.tappedAndAttacking)
        )
        if (moved.actualDestination != Zone.BATTLEFIELD) {
            // Something kept the result off the battlefield (an entry prohibition): the two cards
            // were never combined, so both stay in exile as themselves.
            val unmelded = separateMeldedHost(moved.state, cardRegistry, sourceId, MeldedComponent(partnerId, hostFront))
                .addToZone(ZoneKey(partnerOwner, Zone.EXILE), partnerId)
                .updateEntity(partnerId) { it.with(ExiledFromZoneComponent(Zone.BATTLEFIELD)) }
            return EffectResult.success(unmelded, events + moved.events)
        }
        events.addAll(moved.events)
        val (entered, entryEvents) = EntersWithReplacements.applyOnEntry(
            moved.state, sourceId, owner, cardRegistry,
            predicateEvaluator = zones.predicateEvaluator, preEntryZone = ZoneKey(owner, Zone.EXILE)
        )
        events.addAll(entryEvents)
        return EffectResult.success(entered, events)
    }

    /** CR 701.42b: two non-token cards in exile that are exactly [result]'s meld pair. */
    private fun canMeld(state: GameState, hostId: EntityId, partnerId: EntityId, result: CardDefinition): Boolean {
        if (!result.meldResult || result.meldParts.size != 2) return false
        val names = listOf(hostId, partnerId).map { id ->
            val container = state.getEntity(id) ?: return false
            if (container.has<TokenComponent>()) return false
            val owner = ownerOf(state, id) ?: return false
            if (id !in state.getZone(ZoneKey(owner, Zone.EXILE))) return false
            container.get<CardComponent>()?.name ?: return false
        }
        return names.toSet() == result.meldParts.toSet()
    }

    private fun ownerOf(state: GameState, id: EntityId): EntityId? {
        val container = state.getEntity(id) ?: return null
        return container.get<OwnerComponent>()?.playerId ?: container.get<CardComponent>()?.ownerId
    }
}

/**
 * Undo a meld on the host entity: give it back its own front face ([MeldedComponent.hostFrontCard])
 * and drop the marker. The partner card is the caller's to place — [ZoneTransitionService] puts it
 * into the zone the melded permanent went to (CR 712.21).
 */
internal fun separateMeldedHost(
    state: GameState,
    cardRegistry: CardRegistry,
    hostId: EntityId,
    melded: MeldedComponent,
): GameState {
    val frontDef = cardRegistry.getCard(melded.hostFrontCard.cardDefinitionId)
    return state.updateEntity(hostId) { c ->
        val reverted = c.with(melded.hostFrontCard).without<MeldedComponent>()
        if (frontDef != null) withFaceIntrinsicComponents(reverted, frontDef) else reverted
    }
}
