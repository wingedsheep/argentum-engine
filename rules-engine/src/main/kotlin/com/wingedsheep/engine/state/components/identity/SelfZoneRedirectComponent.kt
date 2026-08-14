package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import kotlinx.serialization.Serializable

/**
 * A card's own "would be put into [zone] from anywhere → redirect instead" replacement
 * effect(s), carried on the card entity so they function in **every** zone (CR 113.6b — an
 * ability that states which zones it functions in functions only from those zones, and these say
 * "from anywhere"), not
 * only while the source is on the battlefield.
 *
 * Built at entity creation from the printed [RedirectZoneChange] effects marked
 * `selfOnly = true` (Darksteel Colossus, Blightsteel Colossus, Progenitus, …). Because it lives
 * on the moving card rather than on a battlefield permanent, the redirect fires when the card is
 * milled from the library, discarded from hand, or countered on the stack — not just when it dies.
 *
 * [com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect] reads this
 * before scanning the battlefield for other permanents' redirects.
 */
@Serializable
data class SelfZoneRedirectComponent(
    val redirects: List<RedirectZoneChange>
) : Component
