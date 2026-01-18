package com.wingedsheep.rulesengine.zone

import kotlinx.serialization.Serializable

@Serializable
enum class ZoneType {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    STACK,
    EXILE,
    COMMAND;

    val isPublic: Boolean
        get() = this in listOf(BATTLEFIELD, GRAVEYARD, STACK, EXILE, COMMAND)

    val isHidden: Boolean
        get() = this in listOf(LIBRARY, HAND)

    val isShared: Boolean
        get() = this in listOf(BATTLEFIELD, STACK, EXILE)
}
