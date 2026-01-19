package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
enum class Keyword(val displayName: String) {
    // Evasion
    FLYING("Flying"),
    MENACE("Menace"),
    INTIMIDATE("Intimidate"),
    FEAR("Fear"),
    SHADOW("Shadow"),
    HORSEMANSHIP("Horsemanship"),
    UNBLOCKABLE("Unblockable"),

    // Landwalk
    SWAMPWALK("Swampwalk"),
    FORESTWALK("Forestwalk"),
    ISLANDWALK("Islandwalk"),
    MOUNTAINWALK("Mountainwalk"),
    PLAINSWALK("Plainswalk"),

    // Combat modifiers
    FIRST_STRIKE("First strike"),
    DOUBLE_STRIKE("Double strike"),
    TRAMPLE("Trample"),
    DEATHTOUCH("Deathtouch"),
    LIFELINK("Lifelink"),
    VIGILANCE("Vigilance"),
    REACH("Reach"),
    DEFENDER("Defender"),
    INDESTRUCTIBLE("Indestructible"),

    // Speed modifiers
    HASTE("Haste"),
    FLASH("Flash"),

    // Protection and prevention
    HEXPROOF("Hexproof"),
    SHROUD("Shroud"),
    WARD("Ward"),
    PROTECTION("Protection"),

    // Other
    FLYING_REACH("Flying, Reach"),

    // Lorwyn Eclipsed Keywords
    CHANGELING("Changeling"),
    PROWESS("Prowess"),
    CONVOKE("Convoke"),
    CANT_BE_BLOCKED("Can't be blocked");

    companion object {
        fun fromString(value: String): Keyword? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }

        fun parseFromOracleText(oracleText: String): Set<Keyword> {
            val keywords = mutableSetOf<Keyword>()
            val lines = oracleText.split("\n")

            for (line in lines) {
                val trimmed = line.trim()
                // Check for single keyword on a line (most common)
                fromString(trimmed)?.let { keywords.add(it) }

                // Check for comma-separated keywords (e.g., "Flying, vigilance")
                if (trimmed.contains(",")) {
                    trimmed.split(",").forEach { part ->
                        fromString(part.trim())?.let { keywords.add(it) }
                    }
                }
            }

            return keywords
        }
    }
}
