package com.wingedsheep.engine.state.permissions

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.Serializable

/**
 * A "you may play this card" permission held in [com.wingedsheep.engine.state.GameState].
 *
 * Permissions live as a list on the game state — like
 * [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect] for continuous P/T effects —
 * and read sites query the list rather than inspect the card.
 *
 * Why a list instead of a stamp:
 * - The same permission may apply to multiple cards (Mind's Desire storm, Cruelclaw exile pile).
 * - Conditions (Possibility Technician's "if you control a Kavu") must re-evaluate at every read,
 *   not just when the permission is granted.
 * - The granting permanent may leave play before the permission expires; the permission's
 *   lifecycle is owned by the game state, not the card.
 *
 * @param id Unique id for this permission, used for targeted removal (consume on cast, etc.).
 * @param cardIds Which cards this permission applies to. Read sites do their own zone check —
 *   a permission targeting an exiled card is irrelevant once the card moves to the graveyard.
 * @param controllerId Who may play the cards.
 * @param sourceId Granting permanent / spell, for trigger-context reconstruction. Required when
 *   [condition] is set so source-keyed conditions (`SourceHas*`, `SourceIs*`) can resolve
 *   correctly; optional otherwise.
 * @param condition Optional gate re-evaluated on every query. When present, the permission is
 *   only honored while the condition holds.
 * @param withAnyManaType If true, mana of any type can be spent to cast (Taster of Wares).
 * @param landEntersTapped If true, a land card played via this permission enters the battlefield
 *   tapped. Used by Lightstall Inquisitor-style exile-from-hand effects whose "lands played
 *   this way enter tapped" clause must be enforced on top of the played card's intrinsic ETB
 *   state, independent of the card's own script.
 * @param permanent If true, the permission is not auto-removed at end of turn cleanup. Permanent
 *   permissions are removed explicitly (e.g., when their card resolves). Non-permanent grants
 *   expire via [expiresAfterTurn].
 * @param expiresAfterTurn Turn number after whose cleanup this permission is removed. `null`
 *   plus `permanent=false` means "remove at the next end-of-turn cleanup."
 * @param timestamp Monotonic timestamp from [com.wingedsheep.engine.state.GameState.timestamp]
 *   for ordering equal permissions.
 */
@Serializable
data class MayPlayPermission(
    val id: EntityId,
    val cardIds: Set<EntityId>,
    val controllerId: EntityId,
    val sourceId: EntityId? = null,
    val condition: Condition? = null,
    val withAnyManaType: Boolean = false,
    val landEntersTapped: Boolean = false,
    val permanent: Boolean = false,
    val expiresAfterTurn: Int? = null,
    val timestamp: Long
) {
    init {
        // gateOpen falls back sourceId ?: cardId when building the EffectContext, which would
        // make SourceHas* / SourceIs* conditions silently misfire (they'd read the exiled card
        // as the source). Require a real sourceId whenever a condition is attached.
        require(condition == null || sourceId != null) {
            "MayPlayPermission with a condition must specify sourceId (condition: ${condition!!.description})"
        }
    }
}
