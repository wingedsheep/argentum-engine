package com.wingedsheep.rulesengine.core

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
    KINDRED("Kindred");  // Replaces "Tribal" - allows non-creature spells to have creature types

    val isPermanent: Boolean
        get() = this in listOf(CREATURE, ENCHANTMENT, ARTIFACT, LAND, PLANESWALKER)

    companion object {
        fun fromString(value: String): CardType? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }
    }
}
