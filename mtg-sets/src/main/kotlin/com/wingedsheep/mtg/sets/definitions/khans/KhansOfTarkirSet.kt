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
        FirehoofCavalry,
        JeskaiStudent,

        // White spells
        DefiantStrike,
        Erase,
        KillShot,
        SmiteTheMonstrous,

        // Blue creatures
        JeskaiWindscout,
        MonasteryFlock,
        WetlandSambar,
        WhirlwindAdept,

        // Blue spells
        CripplingChill,
        DisdainfulStroke,
        WeaveFate,

        // Black creatures
        BellowingSaddlebrute,
        GurmagSwiftwing,

        // Black spells
        BitterRevelation,
        DebilitatingInjury,
        Despise,
        DutifulReturn,
        Throttle,

        // Green creatures
        AlpineGrizzly,
        HeirOfTheWilds,
        HighlandGame,
        WoollyLoxodon,

        // Green spells
        AwakenTheBear,
        FeedTheClan,
        SavagePunch,

        // Red creatures
        CanyonLurkers,
        LeapingMaster,
        MonasterySwiftspear,
        SummitProwler,
        ValleyDasher,

        // Red spells
        BringLow,
        HordelingOutburst,

        // Multicolor
        ChiefOfTheEdge,
        ChiefOfTheScale,
        HighspireMantis,
        PonybackBrigade,
        SnowhornRider,
        UtterEnd,
        Winterflame,

        // Colorless
        AbzanBanner,
        MarduBanner,
        WitnessOfTheAges,

        // Lands
        JungleHollow,
        RuggedHighlands,
        WindScarredCrag,
    )
}
