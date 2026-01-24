package com.wingedsheep.engine.support

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats

/**
 * Test card definitions for unit tests.
 *
 * These are simplified cards designed for testing specific mechanics.
 * They intentionally use minimal abilities to make test assertions clearer.
 */
object TestCards {

    // =========================================================================
    // Basic Lands
    // =========================================================================

    val Forest = CardDefinition.basicLand("Forest", Subtype("Forest"))
    val Island = CardDefinition.basicLand("Island", Subtype("Island"))
    val Mountain = CardDefinition.basicLand("Mountain", Subtype("Mountain"))
    val Plains = CardDefinition.basicLand("Plains", Subtype("Plains"))
    val Swamp = CardDefinition.basicLand("Swamp", Subtype("Swamp"))

    // =========================================================================
    // Vanilla Creatures
    // =========================================================================

    /**
     * 2/2 for {1}{G}
     */
    val GrizzlyBears = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    /**
     * 3/3 for {2}{G}
     */
    val CentaurCourser = CardDefinition.creature(
        name = "Centaur Courser",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Centaur"), Subtype("Warrior")),
        power = 3,
        toughness = 3
    )

    /**
     * 5/5 for {3}{G}{G}
     */
    val ForceOfNature = CardDefinition.creature(
        name = "Force of Nature",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype("Elemental")),
        power = 5,
        toughness = 5
    )

    /**
     * 2/1 for {R}
     */
    val GoblinGuide = CardDefinition.creature(
        name = "Goblin Guide",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Scout")),
        power = 2,
        toughness = 1
    )

    /**
     * 1/1 for {W}
     */
    val SavannahLions = CardDefinition.creature(
        name = "Savannah Lions",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Cat")),
        power = 1,
        toughness = 1
    )

    // =========================================================================
    // Creatures with Keywords
    // =========================================================================

    /**
     * 2/2 Flying for {1}{U}{U}
     */
    val WindDrake = CardDefinition.creature(
        name = "Wind Drake",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Drake")),
        power = 2,
        toughness = 2,
        oracleText = "Flying",
        keywords = setOf(Keyword.FLYING)
    )

    /**
     * 2/1 First Strike for {1}{R}
     */
    val BladeOfTheNinthWatch = CardDefinition.creature(
        name = "Blade of the Ninth Watch",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 1,
        oracleText = "First strike",
        keywords = setOf(Keyword.FIRST_STRIKE)
    )

    // =========================================================================
    // Instants
    // =========================================================================

    /**
     * {R} - Deal 3 damage to any target.
     */
    val LightningBolt = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target."
    )

    /**
     * {G} - +3/+3 until end of turn.
     */
    val GiantGrowth = CardDefinition.instant(
        name = "Giant Growth",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Target creature gets +3/+3 until end of turn."
    )

    /**
     * {U}{U} - Counter target spell.
     */
    val Counterspell = CardDefinition.instant(
        name = "Counterspell",
        manaCost = ManaCost.parse("{U}{U}"),
        oracleText = "Counter target spell."
    )

    /**
     * {U} - Counter target spell with mana value 2 or less.
     */
    val SpellPierce = CardDefinition.instant(
        name = "Spell Pierce",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Counter target noncreature spell unless its controller pays {2}."
    )

    // =========================================================================
    // Sorceries
    // =========================================================================

    /**
     * {1}{B} - Destroy target nonblack creature.
     */
    val DoomBlade = CardDefinition.sorcery(
        name = "Doom Blade",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Destroy target nonblack creature."
    )

    // =========================================================================
    // All Test Cards
    // =========================================================================

    val all: List<CardDefinition> = listOf(
        // Lands
        Forest,
        Island,
        Mountain,
        Plains,
        Swamp,
        // Creatures
        GrizzlyBears,
        CentaurCourser,
        ForceOfNature,
        GoblinGuide,
        SavannahLions,
        WindDrake,
        BladeOfTheNinthWatch,
        // Instants
        LightningBolt,
        GiantGrowth,
        Counterspell,
        SpellPierce,
        // Sorceries
        DoomBlade
    )
}
