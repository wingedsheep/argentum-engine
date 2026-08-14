package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
enum class CardType(val displayName: String) {
    CREATURE("Creature"),
    SORCERY("Sorcery"),
    INSTANT("Instant"),
    ENCHANTMENT("Enchantment"),
    ARTIFACT("Artifact"),
    LAND("Land"),
    PLANESWALKER("Planeswalker"),
    KINDRED("Kindred"),  // Replaces "Tribal" - allows non-creature spells to have creature types
    VANGUARD("Vanguard"),  // Oversized avatar card; lives only in the command zone (Momir Basic)
    BATTLE("Battle");  // CR 310 — defense counters, a protector, and it can be attacked

    val isPermanent: Boolean
        get() = this in listOf(CREATURE, ENCHANTMENT, ARTIFACT, LAND, PLANESWALKER, BATTLE)

    companion object {
        fun fromString(value: String): CardType? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }
    }
}
