package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
enum class Keyword(val displayName: String) {
    // ── Evasion ──────────────────────────────────────────────
    FLYING("Flying"),
    MENACE("Menace"),
    INTIMIDATE("Intimidate"),
    FEAR("Fear"),
    SHADOW("Shadow"),
    HORSEMANSHIP("Horsemanship"),
    UNBLOCKABLE("Unblockable"),
    CANT_BE_BLOCKED("Can't be blocked"),
    CANT_BE_BLOCKED_BY_MORE_THAN_ONE("Can't be blocked by more than one creature"),

    // ── Landwalk ─────────────────────────────────────────────
    SWAMPWALK("Swampwalk"),
    FORESTWALK("Forestwalk"),
    ISLANDWALK("Islandwalk"),
    MOUNTAINWALK("Mountainwalk"),
    PLAINSWALK("Plainswalk"),

    // ── Combat ───────────────────────────────────────────────
    FIRST_STRIKE("First strike"),
    DOUBLE_STRIKE("Double strike"),
    TRAMPLE("Trample"),
    DEATHTOUCH("Deathtouch"),
    LIFELINK("Lifelink"),
    VIGILANCE("Vigilance"),
    REACH("Reach"),

    // ── Defense ──────────────────────────────────────────────
    DEFENDER("Defender"),
    INDESTRUCTIBLE("Indestructible"),
    HEXPROOF("Hexproof"),
    SHROUD("Shroud"),
    WARD("Ward"),
    PROTECTION("Protection"),

    // ── Speed ────────────────────────────────────────────────
    HASTE("Haste"),
    FLASH("Flash"),

    // ── Triggered/Static keyword abilities ───────────────────
    PROWESS("Prowess"),
    CHANGELING("Changeling"),

    // ── Cost reduction ───────────────────────────────────────
    CONVOKE("Convoke"),
    DELVE("Delve"),
    AFFINITY("Affinity"),

    // ── Restrictions ─────────────────────────────────────────
    CANT_UNTAP("Can't untap"),
    MAY_NOT_UNTAP("You may choose not to untap");

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
