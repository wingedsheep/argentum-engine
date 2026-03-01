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
        Siegecraft,
        SmiteTheMonstrous,
        TakeUpArms,

        // Blue creatures
        JeskaiWindscout,
        MonasteryFlock,
        MysticOfTheHiddenWay,
        Scaldkin,
        WetlandSambar,
        WhirlwindAdept,

        // Blue spells
        Cancel,
        CripplingChill,
        DisdainfulStroke,
        WeaveFate,

        // Black creatures
        BellowingSaddlebrute,
        GurmagSwiftwing,
        RottingMastodon,
        SultaiScavenger,
        UnyieldingKrumar,

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
        HootingMandrills,
        TuskedColossodon,
        WoollyLoxodon,

        // Green spells
        AwakenTheBear,
        FeedTheClan,
        Naturalize,
        SavagePunch,

        // Red creatures
        BloodfireExpert,
        CanyonLurkers,
        LeapingMaster,
        MarduBlazebringer,
        MonasterySwiftspear,
        SummitProwler,
        ValleyDasher,

        // Red spells
        BringLow,
        HordelingOutburst,
        TrumpetBlast,

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
