package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.handlers.effects.permanent.counters.counterTypeToString
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageSourceLki
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Read-only view of a permanent's characteristics. Implemented by both the *live* projected
 * board ([LiveEntityView], a thin [ProjectedState] adapter) and a *frozen* [EntitySnapshot].
 *
 * This is the single abstraction over "read this permanent's property" that lets last-known
 * information (CR 113.7a / 603.10 / 608.2h) resolve uniformly: a read site asks for an
 * [EntityView] for an entity — live if it is still on the battlefield, otherwise its captured
 * snapshot — and reads the same accessors regardless of whether the permanent is still in play.
 */
interface EntityView {
    val entityId: EntityId
    val power: Int?
    val toughness: Int?
    val controllerId: EntityId?

    /** Counter-type-string → count (e.g. "+1/+1", "-1/-1", "loyalty"). Matches the counter wire format. */
    val counters: Map<String, Int>
    val keywords: Set<String>
    val subtypes: Set<String>
    val supertypes: Set<String>
    val lostAllAbilities: Boolean

    val plusOnePlusOneCounters: Int get() = counters["+1/+1"] ?: 0
    val minusOneMinusOneCounters: Int get() = counters["-1/-1"] ?: 0
    val totalCounters: Int get() = counters.values.sum()
}

/**
 * Live, projection-backed [EntityView]. Reads flow straight through to [GameState.projectedState]
 * (and the entity's [CountersComponent] for counters, which projection does not surface), so the
 * values are always current. Used as the "still on the battlefield" arm of last-known resolution.
 */
class LiveEntityView(
    private val state: GameState,
    override val entityId: EntityId,
) : EntityView {
    private val projected: ProjectedState get() = state.projectedState
    override val power: Int? get() = projected.getPower(entityId)
    override val toughness: Int? get() = projected.getToughness(entityId)
    override val controllerId: EntityId? get() = projected.getController(entityId)
    override val counters: Map<String, Int> get() = countersOf(state, entityId)
    override val keywords: Set<String> get() = projected.getKeywords(entityId)
    override val subtypes: Set<String> get() = projected.getSubtypes(entityId)
    override val supertypes: Set<String> get() = projected.getSupertypes(entityId)
    override val lostAllAbilities: Boolean get() = projected.hasLostAllAbilities(entityId)
}

/**
 * Frozen projected characteristics of a permanent captured at a specific moment — typically just
 * before it leaves the battlefield (CR 113.7a / 603.10 / 608.2h, "as it last existed on the
 * battlefield"). The single last-known-information value type for the whole engine. It backs:
 *
 * - **cost-time references** — sacrificed / tapped / chosen permanents and a self-sacrificing
 *   source (CR 113.7a), so "deals damage equal to its power" reads the pre-cost power; and
 * - **death / leaves-the-battlefield triggers** — carried as a single value on
 *   [com.wingedsheep.engine.core.ZoneChangeEvent.lastKnown] and threaded into trigger resolution,
 *   replacing what used to be ~16 parallel `lastKnown*` scalar fields.
 *
 * The first six parameters preserve the order of the former `PermanentSnapshot` so positional
 * construction at existing call sites is unaffected; the remaining fields (defaulted) carry the
 * death/leave last-known information that previously lived as loose scalars on the event.
 */
@Serializable
data class EntitySnapshot(
    override val entityId: EntityId,
    override val power: Int? = null,
    override val toughness: Int? = null,
    override val subtypes: Set<String> = emptySet(),
    /** Projected supertypes at capture time (e.g. "LEGENDARY", "BASIC", "SNOW", "WORLD"). */
    override val supertypes: Set<String> = emptySet(),
    /**
     * Controller frozen at capture time, NOT at the eventual zone-leave. If control shifts after the
     * snapshot is taken (e.g. Threaten resolves while the ability is on the stack) and the permanent
     * then leaves, this reports the older controller — acceptable for current callers; revisit if a
     * card needs control-at-zone-leave fidelity.
     */
    override val controllerId: EntityId? = null,
    override val counters: Map<String, Int> = emptyMap(),
    override val keywords: Set<String> = emptySet(),
    override val lostAllAbilities: Boolean = false,
    // --- battlefield-exit-only fields (no meaning for a live permanent) ---
    /** Projected type line at capture, so leaves-battlefield triggers see continuous-effect-granted types. */
    val typeLine: TypeLine? = null,
    /** Card definition id, so dies/leaves triggers resolve for tokens after 704.5d cleanup. */
    val cardDefinitionId: String? = null,
    /**
     * The permanent's name at capture time. Frozen because a *cost* has to be describable after the
     * permanent it consumed is gone: the emerge sacrifice (CR 702.119a) is named on the stack card
     * and in the game log, and a sacrificed token leaves no entity to look the name up on once
     * 704.5d cleanup has run.
     */
    val name: String? = null,
    /** The original card name when this permanent entered as a copy (Clever Impersonator). */
    val copyOfOriginalName: String? = null,
    /** For auras/equipment: the entity this was attached to when it left (enchanted-creature dies triggers). */
    val attachedTo: EntityId? = null,
    /**
     * True if this permanent had at least one Equipment attached when it left the battlefield. The
     * live attachment links are torn down by the exit cleanup, so leaves/dies triggers asking "was
     * it modified/equipped?" must read last-known information (CR 608.2h). Backs the last-known leg
     * of [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEquipped] and (together with
     * counters / [wasEnchanted]) [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsModified].
     */
    val wasEquipped: Boolean = false,
    /**
     * The Auras/Equipment that were attached to this permanent when it left the battlefield, in
     * attachment order. [wasEquipped]/[wasEnchanted] answer "was it attached to *anything* of this
     * kind"; this field answers "by *what*", which is what an ATTACHED-bound leaves/dies trigger
     * borne by a still-on-the-battlefield Equipment needs to find itself again.
     *
     * The live links are torn down by the exit cleanup (CR 704.5m/n), and when the permanent dies
     * to a state-based action the unattach runs in the *same* SBA pass as the death — so by the
     * time triggers are detected, the attachment index no longer connects the Equipment to its
     * dead host. Freezing the ids here is the last-known information (CR 608.2h) that lets
     * `AttachmentTriggerDetector` still fire "whenever equipped creature dies".
     */
    val attachmentIds: List<EntityId> = emptyList(),
    /**
     * True if this permanent had at least one Aura attached when it left the battlefield (CR 303.4).
     * Last-known counterpart to [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted];
     * a leg of the last-known [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsModified].
     */
    val wasEnchanted: Boolean = false,
    /** Creatures blocking, or blocked by, this one when it left (CR 509; Abu Ja'far). */
    val blockingOrBlockedByIds: List<EntityId> = emptyList(),
    /**
     * True if this permanent was an attacking creature (CR 506.4) at the moment it left the
     * battlefield. Frozen here because the live `AttackingComponent` is torn down by the exit's
     * combat cleanup, so a dies/leaves trigger asking "was it attacking?" has to read last known
     * information (CR 608.2h) — Garna, Bloodfist of Keld ("draw a card if it was attacking").
     * Backs the last-known leg of
     * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsAttacking].
     */
    val wasAttacking: Boolean = false,
    /**
     * What this permanent was attacking when it left the battlefield — the player, planeswalker or
     * battle its `AttackingComponent` named. The id half of [wasAttacking], and CR 802.2a is why it
     * has to be frozen: when the creature "is no longer attacking", the defending player its
     * ability refers to is still "the player that creature was attacking before it was removed from
     * combat". Mindstab Thrull sacrifices itself and *then* makes the defending player discard, so
     * by that point the live component the defender is normally read off has been torn down.
     */
    val attackedDefenderId: EntityId? = null,
    /** True if the leaving entity was a token (CR 704.5d — suppress persist-style return triggers). */
    val wasToken: Boolean = false,
    /**
     * The permanent that created this one ([CreatedByComponent]), frozen as it left. A token is
     * swept out of existence before a leaves-the-battlefield trigger gates (CR 704.5d), so
     * "when **the token** leaves the battlefield" (Dance of Many) can only tell its own token from
     * anyone else's by last-known information. See
     * [com.wingedsheep.engine.state.components.identity.CreatedByComponent].
     */
    val createdBy: EntityId? = null,
    /**
     * True if this permanent carried the suspected designation (CR 701.60a) at capture time.
     *
     * Frozen because the designation is a floating effect keyed on the entity, and a sacrificed
     * permanent's floating effects are torn down with it — so "if the sacrificed creature was
     * suspected" (Agency Coroner) has to read last-known information (CR 608.2h), exactly as
     * [supertypes] does for "was legendary".
     */
    val wasSuspected: Boolean = false,
    /** Per-player damage dealt to this entity this turn, keyed by source-controller (Grothama). */
    val damageDealtByPlayers: Map<EntityId, Int> = emptyMap(),
    /** Snapshots of the sources that dealt damage to this entity this turn (Shelob, Child of Ungoliant). */
    val damageSources: Set<DamageSourceLki> = emptySet(),
    /** The cast-time {X} carried by `CastChoicesComponent`, so dies/leaves triggers read `DynamicAmount.CastX`. */
    val castX: Int? = null,
    /**
     * True if this permanent was face down (CR 708) when it left the battlefield.
     *
     * Frozen because a card put into a graveyard is always turned face up (CR 708.4), so by the
     * time a dies trigger is gated the `FaceDownComponent` is gone along with the battlefield
     * entity — "whenever a face-down creature you control dies" (Yarus, Roar of the Old Gods) can
     * only be answered from last-known information (CR 608.2h). Backs the last-known leg of
     * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsFaceDown] / `IsFaceUp`, the same
     * way [wasSuspected] does for the suspected designation.
     */
    val wasFaceDown: Boolean = false,
) : EntityView {
    companion object {
        /**
         * Capture the projection-derivable characteristics of [entityId] from [state], including
         * counters/keywords/lost-abilities. Caller must invoke this BEFORE any zone change so
         * projected values still resolve. The zone-transition path augments the result with the
         * battlefield-exit-only fields ([typeLine], [attachedTo], …) via `copy(...)`.
         */
        fun fromProjection(entityId: EntityId, state: GameState): EntitySnapshot {
            val projected = state.projectedState
            return EntitySnapshot(
                entityId = entityId,
                power = projected.getPower(entityId),
                toughness = projected.getToughness(entityId),
                subtypes = projected.getSubtypes(entityId),
                supertypes = projected.getSupertypes(entityId),
                controllerId = projected.getController(entityId),
                counters = countersOf(state, entityId),
                keywords = projected.getKeywords(entityId),
                lostAllAbilities = projected.hasLostAllAbilities(entityId),
                wasSuspected = projected.isSuspected(entityId),
                name = state.getEntity(entityId)?.get<CardComponent>()?.name,
            )
        }
    }
}

/** Counter-type-string → count for [entityId], in the counter wire format. */
private fun countersOf(state: GameState, entityId: EntityId): Map<String, Int> =
    state.getEntity(entityId)?.get<CountersComponent>()
        ?.counters?.filterValues { it > 0 }
        ?.mapKeys { (type, _) -> counterTypeToString(type) }
        ?: emptyMap()

/**
 * Capture frozen [EntitySnapshot]s (projected P/T, subtypes, supertypes, controller) for a list of
 * permanents, in order. Caller must invoke this BEFORE any zone change so projected values still
 * resolve. Used for cost-time last-known information (sacrificed / tapped / chosen permanents).
 */
fun captureEntitySnapshots(
    ids: List<EntityId>,
    projected: ProjectedState,
): List<EntitySnapshot> = ids.map { id ->
    EntitySnapshot(
        entityId = id,
        power = projected.getPower(id),
        toughness = projected.getToughness(id),
        subtypes = projected.getSubtypes(id),
        supertypes = projected.getSupertypes(id),
        controllerId = projected.getController(id),
        wasSuspected = projected.isSuspected(id),
    )
}

/**
 * [captureEntitySnapshots] overload that also records the facts only [GameState] carries, not
 * [ProjectedState]: each permanent's **token-ness** ([EntitySnapshot.wasToken], via [TokenComponent])
 * and its **name** ([EntitySnapshot.name], via [CardComponent]). Use at a sacrifice site when a
 * following sibling needs either — Exploit's `ExploitedEvent.sacrificedWasToken` (read by Skull
 * Skaab's "exploits a nontoken creature" clause) for the first, naming what an alternative cost ate
 * for the second. Caller must invoke this BEFORE the zone change so projected values, the token
 * component and the card component all still resolve.
 */
fun captureEntitySnapshots(
    ids: List<EntityId>,
    state: GameState,
): List<EntitySnapshot> = captureEntitySnapshots(ids, state.projectedState).map { snapshot ->
    val container = state.getEntity(snapshot.entityId)
    snapshot.copy(
        wasToken = container?.has<TokenComponent>() ?: false,
        name = container?.get<CardComponent>()?.name,
    )
}

/**
 * The permanent's **projected** type line: its printed types overlaid with whatever continuous
 * effects have granted or replaced (an animated artifact reads "Artifact Creature", a Vehicle
 * crewed this turn reads "Artifact Creature — Vehicle"). Falls back to the printed type line when
 * the entity has no projection entry, and returns null when it has no [CardComponent] at all.
 *
 * Caller must invoke this BEFORE any zone change, while the projection entry still exists. It is
 * the single value frozen into [EntitySnapshot.typeLine], so the zone-exit path
 * (`ZoneTransitionService`) and the cost-time path (`ActivateAbilityHandler`'s
 * [com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent.lastKnownSourceSnapshot])
 * capture the same thing rather than two hand-rolled copies.
 */
fun projectedTypeLine(state: GameState, entityId: EntityId): TypeLine? {
    val base = state.getEntity(entityId)?.get<CardComponent>()?.typeLine ?: return null
    return projectedTypeLine(state, entityId, base)
}

/** [projectedTypeLine] for a caller that already holds the printed [baseTypeLine]. */
fun projectedTypeLine(state: GameState, entityId: EntityId, baseTypeLine: TypeLine): TypeLine {
    val projected = state.projectedState.getProjectedValues(entityId) ?: return baseTypeLine
    val cardTypes = projected.types
        .mapNotNull { runCatching { CardType.valueOf(it) }.getOrNull() }
        .toSet()
        .ifEmpty { baseTypeLine.cardTypes }
    return baseTypeLine.copy(
        cardTypes = cardTypes,
        subtypes = projected.subtypes.map { Subtype(it) }.toSet(),
    )
}

fun List<EntitySnapshot>.snapshotFor(id: EntityId): EntitySnapshot? =
    firstOrNull { it.entityId == id }

val List<EntitySnapshot>.entityIds: List<EntityId>
    get() = map { it.entityId }
