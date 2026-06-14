package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a card as a player's Vanguard avatar (the Momir Basic format —
 * [com.wingedsheep.sdk.core.Format.MomirBasic]).
 *
 * Attached at game-init time to the avatar card placed in each player's command zone. The avatar
 * never enters the battlefield or the stack; it sits in the command zone and grants its activated
 * ability from there (`activateFromZone == Zone.COMMAND`, surfaced by
 * [com.wingedsheep.engine.legalactions.enumerators.CommandZoneAbilityEnumerator]). The marker lets
 * later layers (client rendering, AI) identify the avatar without inferring it from the zone.
 *
 * @property ownerId The player whose avatar this is.
 */
@Serializable
data class VanguardAvatarComponent(
    val ownerId: EntityId,
) : Component
