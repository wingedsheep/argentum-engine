package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ZoneType
import kotlinx.serialization.Serializable

/**
 * Zone enumeration for game events.
 * Represents the different zones in Magic: The Gathering.
 */
@Serializable
enum class Zone(val description: String) {
    Battlefield("the battlefield"),
    Graveyard("a graveyard"),
    Hand("a hand"),
    Library("a library"),
    Exile("exile"),
    Stack("the stack"),
    Command("the command zone")
}

/**
 * Convert SDK Zone to engine ZoneType.
 */
fun Zone.toZoneType(): ZoneType = when (this) {
    Zone.Battlefield -> ZoneType.BATTLEFIELD
    Zone.Graveyard -> ZoneType.GRAVEYARD
    Zone.Hand -> ZoneType.HAND
    Zone.Library -> ZoneType.LIBRARY
    Zone.Exile -> ZoneType.EXILE
    Zone.Stack -> ZoneType.STACK
    Zone.Command -> ZoneType.COMMAND
}
