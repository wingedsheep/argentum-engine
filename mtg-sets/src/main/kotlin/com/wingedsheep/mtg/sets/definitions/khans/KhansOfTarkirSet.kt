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
        AinokBondKin,
        AlabasterKirin,
        DazzlingRamparts,
        FirehoofCavalry,
        JeskaiStudent,
        MarduHateblade,
        MarduHordechief,
        SaltRoadPatrol,
        VenerableLammasu,

        // White spells
        DefiantStrike,
        Erase,
        KillShot,
        RushOfBattle,
        Siegecraft,
        SmiteTheMonstrous,
        TakeUpArms,

        // Blue creatures
        GlacialStalker,
        JeskaiWindscout,
        MonasteryFlock,
        MysticOfTheHiddenWay,
        RiverwheelAerialists,
        Scaldkin,
        ScionOfGlaciers,
        WetlandSambar,
        WhirlwindAdept,

        // Blue spells
        BlindingSpray,
        ForceAway,
        Cancel,
        CripplingChill,
        DisdainfulStroke,
        TaigamsScheming,
        TreasureCruise,
        WeaveFate,

        // Black creatures
        DisownedAncestor,
        BellowingSaddlebrute,
        GurmagSwiftwing,
        KheruDreadmaw,
        RottingMastodon,
        SidisisPet,
        ShamblingAttendants,
        SultaiScavenger,
        SwarmOfBloodflies,
        UnyieldingKrumar,

        // Black spells
        BitterRevelation,
        DebilitatingInjury,
        Despise,
        DutifulReturn,
        RakshasasSecret,
        RiteOfTheSerpent,
        Throttle,

        // Green creatures
        AlpineGrizzly,
        ArchersParapet,
        HeirOfTheWilds,
        HighlandGame,
        HootingMandrills,
        LongshotSquad,
        SaguArcher,
        SmokeTeller,
        TuskedColossodon,
        WoollyLoxodon,

        // Green spells
        AwakenTheBear,
        DragonscaleBoon,
        FeedTheClan,
        Naturalize,
        SavagePunch,
        Windstorm,

        // Red creatures
        AinokTracker,
        BloodfireExpert,
        BloodfireMentor,
        CanyonLurkers,
        LeapingMaster,
        MarduBlazebringer,
        MonasterySwiftspear,
        SummitProwler,
        ValleyDasher,
        WarNameAspirant,

        // Red spells
        BarrageOfBoulders,
        BringLow,
        HordelingOutburst,
        Shatter,
        TormentingVoice,
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
