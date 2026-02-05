package com.wingedsheep.sdk.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Zone(val displayName: String) {
    @SerialName("Library") LIBRARY("a library"),
    @SerialName("Hand") HAND("a hand"),
    @SerialName("Battlefield") BATTLEFIELD("the battlefield"),
    @SerialName("Graveyard") GRAVEYARD("a graveyard"),
    @SerialName("Stack") STACK("the stack"),
    @SerialName("Exile") EXILE("exile"),
    @SerialName("Command") COMMAND("the command zone");

    val isPublic: Boolean
        get() = this in listOf(BATTLEFIELD, GRAVEYARD, STACK, EXILE, COMMAND)

    val isHidden: Boolean
        get() = this in listOf(LIBRARY, HAND)

    val isShared: Boolean
        get() = this in listOf(BATTLEFIELD, STACK, EXILE)
}
