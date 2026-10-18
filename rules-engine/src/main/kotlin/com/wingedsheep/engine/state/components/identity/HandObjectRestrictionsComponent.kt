package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Durational states pinned to one card **while it sits in a hand** — "that player plays with that
 * card revealed in their hand and can't play it" (Firestorm Phoenix).
 *
 * Every entry names the object it was created for by its object [HandObjectRestriction.objectGeneration]
 * (see [com.wingedsheep.engine.state.ObjectRef]). A card that leaves the hand and comes back is a new
 * object (CR 400.7), so an entry whose generation no longer matches is inert even before cleanup
 * strips it. Read and maintained through [com.wingedsheep.engine.mechanics.HandObjectRestrictions].
 */
@Serializable
data class HandObjectRestrictionsComponent(
    val restrictions: List<HandObjectRestriction>
) : Component

/** What a [HandObjectRestriction] does to its card. */
@Serializable
enum class HandObjectRestrictionKind {
    /** The card can't be played — neither cast nor played as a land. */
    CANT_BE_PLAYED,

    /** The card is face up to every player, and stays known even when a same-named card is played. */
    REVEALED,
}

/**
 * One restriction on one hand object.
 *
 * @param removeOn [PlayerEffectRemoval.EndOfTurn], [PlayerEffectRemoval.UntilYourNextTurn] (keyed
 *   to [expiresForPlayerId]) or [PlayerEffectRemoval.Permanent] (for as long as the object exists).
 * @param expiresForPlayerId For [PlayerEffectRemoval.UntilYourNextTurn]: whose next turn ends it —
 *   the card's owner for "until that player's next turn", the effect's controller for "until your
 *   next turn".
 */
@Serializable
data class HandObjectRestriction(
    val kind: HandObjectRestrictionKind,
    val objectGeneration: Long,
    val removeOn: PlayerEffectRemoval,
    val expiresForPlayerId: EntityId? = null
)
