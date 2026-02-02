package com.wingedsheep.sdk.scripting

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
