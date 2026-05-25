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
    PROVOKE("Provoke"),
    FLANKING("Flanking"),

    /**
     * Banding (CR 702.22). As they declare attackers, a player may group one or more
     * attacking creatures with banding plus up to one without banding into a "band"
     * (CR 702.22c). A band attacks the same defender and is blocked as a group.
     *
     * Banding inverts who assigns combat damage (an exception to CR 510.1c):
     * - CR 702.22j — if an attacker is blocked by a creature with banding, the
     *   *defending* player divides that attacker's combat damage among its blockers.
     * - CR 702.22k — if a blocker is blocking a creature with banding, the *active*
     *   player divides that blocker's combat damage among the attackers it blocks.
     */
    BANDING("Banding"),

    // ── Defense ──────────────────────────────────────────────
    DEFENDER("Defender"),
    INDESTRUCTIBLE("Indestructible"),
    HEXPROOF("Hexproof"),
    SHROUD("Shroud"),
    WARD("Ward"),
    PROTECTION("Protection"),
    PROTECTION_FROM_EACH_OPPONENT("Protection from each opponent"),

    // ── Speed ────────────────────────────────────────────────
    HASTE("Haste"),
    FLASH("Flash"),

    // ── Triggered/Static keyword abilities ───────────────────
    PROWESS("Prowess"),
    CHANGELING("Changeling"),

    // ── ETB modification ──────────────────────────────────────
    AMPLIFY("Amplify"),

    // ── Cost reduction ───────────────────────────────────────
    CONVOKE("Convoke"),
    DELVE("Delve"),
    AFFINITY("Affinity"),

    // ── Spell mechanics ─────────────────────────────────────
    STORM("Storm"),
    FLASHBACK("Flashback"),
    EVOKE("Evoke"),
    CONSPIRE("Conspire"),
    HIDEAWAY("Hideaway"),

    /**
     * Cascade (CR 702.85). "When you cast this spell, exile cards from the top of
     * your library until you exile a nonland card whose mana value is less than
     * this spell's mana value. You may cast that spell without paying its mana
     * cost. Put the exiled cards on the bottom of your library in a random order."
     * The cascade trigger fires at cast time and is implemented by the engine when
     * a spell carries the CASCADE keyword (or is granted it by another effect).
     */
    CASCADE("Cascade"),

    /**
     * Plot (CR 718, Outlaws of Thunder Junction). "Plot [cost]" — special action
     * available any time you have priority during your main phase while the stack is
     * empty: pay the plot cost and exile this card from your hand. It becomes plotted.
     * On any later turn, you may cast a plotted card from exile without paying its
     * mana cost as a sorcery.
     *
     * The keyword itself is display-only; cast/exile wiring lives in
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Plot] and the engine's plot
     * action handler + enumerator.
     */
    PLOT("Plot"),

    // ── Creature mechanics ────────────────────────────────
    OFFSPRING("Offspring"),
    PERSIST("Persist"),

    /**
     * Ascend (Ixalan, CR 702.131). On a permanent spell, means "When this permanent
     * enters, if you control ten or more permanents, you get the city's blessing
     * for the rest of the game." Engine wires the trigger explicitly per card; the
     * keyword itself is only a textual marker for rules-text display.
     */
    ASCEND("Ascend"),

    // ── Damage modification ──────────────────────────────
    WITHER("Wither"),
    TOXIC("Toxic"),

    // ── Numeric (parameterized by N) ──────────────────────
    ANNIHILATOR("Annihilator"),
    BUSHIDO("Bushido"),
    RAMPAGE("Rampage"),
    ABSORB("Absorb"),
    AFFLICT("Afflict"),
    CREW("Crew"),
    MODULAR("Modular"),
    FADING("Fading"),
    VANISHING("Vanishing"),
    RENOWN("Renown"),
    FABRICATE("Fabricate"),
    TRIBUTE("Tribute"),

    // ── Ability words (display prefix, no uniform mechanic) ──
    /**
     * Eerie (Duskmourn: House of Horror).
     * Ability word — flavor prefix for effects that trigger whenever an enchantment
     * you control enters or whenever you fully unlock a Room.
     */
    EERIE("Eerie"),

    /**
     * Vivid (Lorwyn Eclipsed).
     * Ability word — flavor prefix for effects whose magnitude scales with the
     * number of distinct colors among permanents you control. No mechanical
     * behavior is attached to this keyword itself; each Vivid card still spells
     * out its own effect. Wired via the `vivid…` DSL helpers on [CardBuilder]
     * or by adding the appropriate effect/static ability directly.
     */
    VIVID("Vivid"),

    /**
     * Fateful Bite (Marvel's Spider-Man).
     * Ability word — flavor prefix used on Spider creatures whose activated abilities
     * tutor up other Spider-related cards. Per CR 207.2c, ability words have no rules
     * meaning; the prefix is metadata only and does not modify resolution.
     */
    FATEFUL_BITE("Fateful Bite");

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

                // Check for ability word prefix: "Ability Word — effect description" (CR 207.2c)
                if (trimmed.contains('—')) {
                    val prefix = trimmed.substringBefore('—').trim()
                    fromString(prefix)?.let { keywords.add(it) }
                }
            }

            return keywords
        }
    }
}
