package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.snapshotFor
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Whether an [EntityReference] falls back to last-known information (CR 113.7a / 603.10 / 608.2h)
 * when its referenced permanent is no longer on the battlefield, or only ever reads the live board.
 *
 * Declared as one exhaustive mapping ([lkiPolicyFor]) so that adding a new [EntityReference] variant
 * is a compile error until its last-known behavior is classified — the fallback can no longer be
 * silently forgotten, nor silently applied where it must not be. Filtered enumeration
 * (Gather / ForEach) is deliberately not modeled here at all: it reads the live battlefield, and a
 * permanent that has left simply is not in the set.
 */
enum class LkiPolicy {
    /** Always read the live board; a departed permanent reads as absent (targets, iteration, Amass). */
    LIVE_ONLY,

    /** Read live while on the battlefield; once it has left, read the captured [EntitySnapshot]. */
    LIVE_THEN_LKI,
}

fun lkiPolicyFor(reference: EntityReference): LkiPolicy = when (reference) {
    // A self-sacrificing/exiling source, the triggering permanent of a dies/leaves trigger, an
    // enchanted creature whose aura detached, and cost-paid permanents (sacrificed / tapped /
    // chosen) are all routinely read after they have left the battlefield (CR 113.7a / 608.2h).
    EntityReference.Source,
    EntityReference.Triggering,
    EntityReference.EnchantedCreature,
    is EntityReference.Sacrificed,
    is EntityReference.TappedAsCost,
    is EntityReference.FromCostStorage,
    -> LkiPolicy.LIVE_THEN_LKI

    // Targets are re-validated at resolution (a departed target fizzles, CR 608.2b); iteration and
    // Amass references only ever name a live permanent; Ring-bearer must be on the battlefield.
    EntityReference.AffectedEntity,
    EntityReference.IterationEntity,
    EntityReference.AmassedArmy,
    is EntityReference.Target,
    is EntityReference.RingBearer,
    // An Imprint pile's card is *never* on the battlefield — it is a card in exile, and its
    // characteristics are the printed ones. There is nothing to snapshot, and asking for a snapshot
    // would be wrong: the read must fall through to base characteristics, which is what LIVE_ONLY
    // does. The reference itself already resolves to null once the card leaves exile.
    is EntityReference.LinkedExiledCard,
    -> LkiPolicy.LIVE_ONLY
}

/**
 * The captured [EntitySnapshot] backing a [LkiPolicy.LIVE_THEN_LKI] [reference] that resolved to
 * [entityId] after it left the battlefield, or null if none was captured (the read then falls
 * through to base characteristics). Triggering- and enchanted-creature last-known P/T are still
 * carried as scalars on [EffectContext] and resolved at their own read sites, so they return null
 * here.
 */
fun EffectContext.lkiSnapshotFor(reference: EntityReference, entityId: EntityId): EntitySnapshot? =
    when (reference) {
        is EntityReference.Sacrificed -> sacrificedPermanents.snapshotFor(entityId)
        is EntityReference.TappedAsCost -> tappedEntitySnapshots.snapshotFor(entityId)
        is EntityReference.FromCostStorage -> chosenEntitySnapshots.snapshotFor(entityId)
        EntityReference.Source -> lastKnownSourceSnapshot?.takeIf { it.entityId == entityId }
        else -> null
    }
