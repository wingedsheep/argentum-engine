package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The combat-damage resolution board (CR 510 / 702.22). A single bipartite graph that
 * replaces the old chain of per-attacker damage modals and the standalone blocker/attacker
 * ordering pre-step: attackers and blockers are nodes, defenders/planeswalkers/battles are
 * drain nodes, and each [DamageEdge] carries an engine-computed default amount the player can
 * adjust before confirming.
 *
 * There is no separate damage-assignment *order* step: the chooser assigns damage to each edge
 * directly, and the validator accepts any split that is legal under *some* order the attacker
 * could pick (CR 510.1c) — i.e. for an [DamageEdge.orderConstrained] source, at most one blocker
 * it damages may be left below [DamageEdge.lethal] (counting cross-source damage this step).
 * Banding (CR 702.22j/k) lifts even that, letting the chooser divide freely, and flips
 * [DamageEdge.editableBy]; it does NOT lift the separate CR 702.19b trample lethal-first gate,
 * which keys off [DamageEdge.lethal] directly regardless of [DamageEdge.orderConstrained].
 */

/** Which way a [DamageEdge] points, and therefore how it is gated. */
@Serializable
enum class DamageEdgeDirection {
    /** Attacker assigns to one of its blockers (CR 510.1c order applies). */
    ATTACKER_TO_BLOCKER,

    /** Blocker assigns to one of the attackers it blocks (CR 510.1c order applies). */
    BLOCKER_TO_ATTACKER,

    /** Trample / free-assignment overflow to the defending player (CR 702.19b drain). */
    ATTACKER_TO_PLAYER,

    /** Trample overflow to the attacked planeswalker. */
    ATTACKER_TO_PLANESWALKER,

    /** Trample overflow to the attacked battle. */
    ATTACKER_TO_BATTLE,
}

/** What kind of object a [ResolutionDefender] node represents. */
@Serializable
enum class ResolutionTargetKind { PLAYER, PLANESWALKER, BATTLE }

/**
 * One directed damage assignment from [sourceId] to [targetId], pre-filled with the
 * engine-computed default [amount].
 *
 * @property id Stable wire id (`"$sourceId->$targetId"`). Clients echo it back in the response;
 *   the engine never parses it — it reads [sourceId] / [targetId] off the cached edge instead.
 * @property amount Engine-computed default; the editor may change it within `[0, maximum]`.
 * @property maximum Cap for this edge — the source's available combat damage (its power, or
 *   toughness for Doran-style sources).
 * @property lethal The true lethal need for [targetId] from this source: 1 for a deathtouch
 *   source, otherwise `toughness − damage already marked` (at least 1). Always the real value
 *   so the CR 702.19b trample gate stays correct even when [orderConstrained] is false.
 * @property orderConstrained Whether this edge participates in CR 510.1c damage-assignment order
 *   gating. False for banding (CR 702.22j/k) and free-assignment edges, which may be divided
 *   ignoring order.
 * @property isTrampleDrain True for a trample overflow edge to a player/planeswalker/battle;
 *   gated by CR 702.19b (every blocker at lethal first), independent of [orderConstrained].
 * @property editableBy The player allowed to modify this edge. Banding flips this to the
 *   opposing player for the affected edges (CR 702.22j/k).
 */
@Serializable
data class DamageEdge(
    val id: String,
    val sourceId: EntityId,
    val targetId: EntityId,
    val direction: DamageEdgeDirection,
    val amount: Int,
    val maximum: Int,
    val lethal: Int,
    val orderConstrained: Boolean,
    val isTrampleDrain: Boolean,
    val editableBy: EntityId,
)

/** An attacker node on the board. */
@Serializable
data class ResolutionAttacker(
    val id: EntityId,
    val name: String,
    val power: Int,
    val toughness: Int,
    val hasTrample: Boolean,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    /** False for non-first-strike sources on the first-strike board (rendered greyed). */
    val dealsDamageThisStep: Boolean,
    /** Non-null when this attacker is part of a band (CR 702.22). */
    val bandId: String?,
    val attackedDefenderId: EntityId,
    val blockedByIds: List<EntityId>,
    /** Damage already marked (e.g. surviving first strike into the regular step). */
    val markedDamage: Int,
)

/** A blocker node on the board. */
@Serializable
data class ResolutionBlocker(
    val id: EntityId,
    val name: String,
    val power: Int,
    val toughness: Int,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    val dealsDamageThisStep: Boolean,
    val blockedAttackerIds: List<EntityId>,
    /** Damage-assignment order for the attackers this blocker blocks (CR 510.1c). */
    val orderedAttackers: List<EntityId>,
    val markedDamage: Int,
)

/** A drain target (player / planeswalker / battle) attacked by one or more attackers. */
@Serializable
data class ResolutionDefender(
    val id: EntityId,
    val kind: ResolutionTargetKind,
    val name: String,
    /** Life (player), loyalty (planeswalker), or defense (battle); null when not applicable. */
    val lifeOrLoyaltyOrDefense: Int?,
)

/**
 * The decision the engine pauses on for a combat damage step.
 *
 * @property coChooserId For the two-actor banding case (CR 702.22j + 702.22k together), the
 *   other player who owns the inverted edges. The resumer hands off to each chooser in turn via
 *   [CombatResolutionContinuation.pendingChoosers].
 */
@Serializable
@SerialName("CombatResolutionDecision")
data class CombatResolutionDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val firstStrike: Boolean,
    val attackers: List<ResolutionAttacker>,
    val blockers: List<ResolutionBlocker>,
    val defenders: List<ResolutionDefender>,
    val edges: List<DamageEdge>,
    val coChooserId: EntityId? = null,
) : PendingDecision

/** A single (edge id -> chosen amount) entry in a [CombatResolutionResponse]. */
@Serializable
data class DamageEdgeAmount(val edgeId: String, val amount: Int)

/**
 * Response to a [CombatResolutionDecision].
 *
 * @property edges The chosen amounts. A chooser submits only the edges they own ([DamageEdge.editableBy]);
 *   the resumer filters out any others.
 * @property orderedBlockers Optional row-order overrides: attacker -> its blockers in assignment order.
 * @property orderedAttackers Optional row-order overrides: blocker -> the attackers it blocks, in order.
 */
@Serializable
@SerialName("CombatResolutionResponse")
data class CombatResolutionResponse(
    override val decisionId: String,
    val edges: List<DamageEdgeAmount>,
    val orderedBlockers: Map<EntityId, List<EntityId>> = emptyMap(),
    val orderedAttackers: Map<EntityId, List<EntityId>> = emptyMap(),
) : DecisionResponse
