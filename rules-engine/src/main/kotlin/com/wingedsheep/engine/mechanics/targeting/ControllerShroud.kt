package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Player-level shroud granted by a permanent ("You have shroud" — True Believer).
 *
 * The shroud counterpart of [ControllerHexproof], and it exists for the same two reasons. First,
 * three readers — [TargetValidator], `TargetEnumerationUtils` and `TargetFinder` — each held their
 * own copy of the same battlefield scan and could drift apart. Second, and load-bearing: the grant
 * can be gated. [GrantsControllerShroudComponent] is stamped once by `StaticAbilityHandler` as the
 * permanent enters, so an "as long as …" wrapper can't be resolved at stamp time; the condition
 * travels on the marker and is re-evaluated here, against current state, on every read.
 *
 * Shroud is stricter than hexproof: it stops the controller's *own* spells and abilities too
 * (CR 702.18), so there is no `appliesAgainst` counterpart — the caster is irrelevant.
 */
object ControllerShroud {

    /**
     * Whether [playerId] has shroud — either directly ([PlayerShroudComponent], from a
     * resolution-time effect like Gilded Light) or from a permanent they control that grants it.
     */
    fun appliesTo(state: GameState, playerId: EntityId): Boolean {
        if (state.getEntity(playerId)?.has<PlayerShroudComponent>() == true) return true
        return ControllerGrants.grantedTo<GrantsControllerShroudComponent>(state, playerId)
    }
}
