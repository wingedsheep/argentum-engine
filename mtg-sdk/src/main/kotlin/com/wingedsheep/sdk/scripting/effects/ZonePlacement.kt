package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.Serializable

/**
 * How a card should be placed in its destination zone.
 */
@Serializable
enum class ZonePlacement {
    Default,
    Top,
    Bottom,
    Shuffled,
    Tapped
}
