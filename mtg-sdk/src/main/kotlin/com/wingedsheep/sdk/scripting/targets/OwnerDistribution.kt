package com.wingedsheep.sdk.scripting.targets

import kotlinx.serialization.Serializable

/** Distribute a target requirement over the owners represented by its matching cards. */
@Serializable
enum class OwnerDistribution {
    EXACTLY_ONE,
    UP_TO_ONE,
}
