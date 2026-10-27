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
        /**
         * Names of the permanent card types (CR 110.4), for matchers that read types as the
         * projected type-name strings rather than as [CardType] values.
         */
        val PERMANENT_TYPE_NAMES: Set<String> = entries.filter { it.isPermanent }.map { it.name }.toSet()

        /**
         * The card types a "choose a card type" effect offers (CR 205.2a), as display names: every
         * type a card in a library can have. Vanguard lives only in the command zone, so it's left
         * out — Alrund, God of the Cosmos's ruling lists exactly these.
         */
        val CHOOSABLE_TYPE_NAMES: List<String> = listOf(
            ARTIFACT, BATTLE, CREATURE, ENCHANTMENT, INSTANT, KINDRED, LAND, PLANESWALKER, SORCERY
        ).map { it.displayName }

        fun fromString(value: String): CardType? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }
    }
}
