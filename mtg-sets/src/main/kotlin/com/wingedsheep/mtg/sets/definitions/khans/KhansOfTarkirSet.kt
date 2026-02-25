package com.wingedsheep.mtg.sets.definitions.khans

import com.wingedsheep.mtg.sets.definitions.khans.cards.*

/**
 * Khans of Tarkir Set (2014)
 *
 * Khans of Tarkir is the first set in the Khans of Tarkir block, featuring
 * the morph mechanic's return, wedge-colored clans, and the prowess mechanic.
 *
 * Set Code: KTK
 * Release Date: September 26, 2014
 * Card Count: 269
 */
object KhansOfTarkirSet {

    const val SET_CODE = "KTK"
    const val SET_NAME = "Khans of Tarkir"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // White creatures
        AlabasterKirin,
        JeskaiStudent,

        // White spells
        KillShot,

        // Blue creatures
        JeskaiWindscout,
        MonasteryFlock,
        WetlandSambar,

        // Black spells
        BitterRevelation,
        DebilitatingInjury,
        Throttle,

        // Green creatures
        AlpineGrizzly,
        HighlandGame,

        // Green spells
        AwakenTheBear,

        // Red creatures
        LeapingMaster,
        MonasterySwiftspear,
        SummitProwler,

        // Multicolor
        ChiefOfTheEdge,
        HighspireMantis,
        UtterEnd,

        // Colorless
        AbzanBanner,
        WitnessOfTheAges,

        // Lands
        JungleHollow,
        WindScarredCrag,
    )
}
