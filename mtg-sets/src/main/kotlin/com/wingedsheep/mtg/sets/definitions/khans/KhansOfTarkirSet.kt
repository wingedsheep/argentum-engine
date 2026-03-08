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
        AbzanBattlePriest,
        AbzanFalconer,
        AinokBondKin,
        AlabasterKirin,
        DazzlingRamparts,
        FirehoofCavalry,
        HeraldOfAnafenza,
        JeskaiStudent,
        MarduHateblade,
        MarduHordechief,
        MasterOfPearls,
        SageEyeHarrier,
        SaltRoadPatrol,
        SeekerOfTheWay,
        TimelyHordemate,
        VenerableLammasu,
        WarBehemoth,
        WatcherOfTheRoost,
        WingmateRoc,

        HighSentinelsOfArashin,

        // White enchantments
        BraveTheSands,
        SuspensionField,

        // White spells
        DefiantStrike,
        EndHostilities,
        Erase,
        FeatOfResistance,
        KillShot,
        RushOfBattle,
        Siegecraft,
        SmiteTheMonstrous,
        TakeUpArms,

        // Blue creatures
        DragonsEyeSavants,
        EmbodimentOfSpring,
        GlacialStalker,
        JeskaiElder,
        JeskaiWindscout,
        MonasteryFlock,
        MysticOfTheHiddenWay,
        RiverwheelAerialists,
        Scaldkin,
        ScionOfGlaciers,
        WetlandSambar,
        PearlLakeAncient,
        WhirlwindAdept,
        MistfireWeaver,

        // Blue spells
        BlindingSpray,
        ForceAway,
        IcyBlast,
        Cancel,
        CripplingChill,
        DigThroughTime,
        DisdainfulStroke,
        SetAdrift,
        StubbornDenial,
        TaigamsScheming,
        ThousandWinds,
        TreasureCruise,
        Waterwhirl,
        WeaveFate,

        // Blue enchantments
        QuietContemplation,
        SingingBellStrike,

        // Black creatures
        BloodsoakedChampion,
        DisownedAncestor,
        BellowingSaddlebrute,
        GrimHaruspex,
        GurmagSwiftwing,
        KheruBloodsucker,
        KheruDreadmaw,
        KrumarBondKin,
        MarduSkullhunter,
        MerEkNightblade,
        RottingMastodon,
        RuthlessRipper,
        SidisisPet,
        ShamblingAttendants,
        SultaiScavenger,
        SwarmOfBloodflies,
        UnyieldingKrumar,

        // Black enchantments
        RaidersSpoils,

        // Black spells
        BitterRevelation,
        EmptyThePits,
        MoltingSnakeskin,
        DeadDrop,
        DeathFrenzy,
        DebilitatingInjury,
        Despise,
        DutifulReturn,
        MurderousCut,
        NecropolisFiend,
        RakshasasSecret,
        RetributionOfTheAncients,
        RiteOfTheSerpent,
        Throttle,

        // Green creatures
        AlpineGrizzly,
        ArchersParapet,
        HeirOfTheWilds,
        HighlandGame,
        KinTreeWarden,
        HootingMandrills,
        LongshotSquad,
        SaguArcher,
        SmokeTeller,
        SultaiFlayer,
        TemurCharger,
        TuskedColossodon,
        TuskguardCaptain,
        PineWalker,
        RattleclawMystic,
        WoollyLoxodon,

        // Green enchantments
        HardenedScales,
        TrailOfMystery,

        // Green spells
        AwakenTheBear,
        BecomeImmense,
        DragonscaleBoon,
        FeedTheClan,
        IncrementalGrowth,
        Naturalize,
        RoarOfChallenge,
        SavagePunch,
        ScoutTheBorders,
        SeeTheUnwritten,
        SeekTheHorizon,
        Windstorm,

        // Red creatures
        AshcloudPhoenix,
        AinokTracker,
        BloodfireExpert,
        BloodfireMentor,
        CanyonLurkers,
        DragonStyleTwins,
        HordeAmbusher,
        JeeringInstigator,
        LeapingMaster,
        MarduBlazebringer,
        MarduHeartPiercer,
        MarduWarshrieker,
        MonasterySwiftspear,
        SummitProwler,
        ValleyDasher,
        WarNameAspirant,

        // Red enchantments
        Goblinslide,

        // Red spells
        ActOfTreason,
        ArcLightning,
        DragonGrip,
        ArrowStorm,
        BarrageOfBoulders,
        BringLow,
        BurnAway,
        CratersClaws,
        HordelingOutburst,
        Shatter,
        SwiftKick,
        TormentingVoice,
        TrumpetBlast,

        // Multicolor
        AbominationOfGudul,
        AnkleShanker,
        AvalancheTusker,
        ArmamentCorps,
        KinTreeInvocation,
        AbzanGuide,
        CracklingDoom,
        DeflectingPalm,
        EfreetWeaponmaster,
        BearsCompanion,
        ButcherOfTheHorde,
        ChiefOfTheEdge,
        ChiefOfTheScale,
        HighspireMantis,
        IcefeatherAven,
        IvorytuskFortress,
        JeskaiCharm,
        MantisRider,
        MasterTheWay,
        Mindswipe,
        MarduCharm,
        MarduRoughrider,
        PonybackBrigade,
        RideDown,
        SavageKnuckleblade,
        SiegeRhino,
        SageOfTheInwardEye,
        SaguMauler,
        SnowhornRider,
        SurrakDragonclaw,
        SecretPlans,
        SultaiAscendancy,
        SultaiCharm,
        SultaiSoothsayer,
        TemurAscendancy,
        TemurCharm,
        TrapEssence,
        UtterEnd,
        WardenOfTheEye,
        Winterflame,
        ZurgoHelmsmasher,

        // Colorless
        AbzanBanner,
        CranialArchive,
        HeartPiercerBow,
        JeskaiBanner,
        MarduBanner,
        SultaiBanner,
        TemurBanner,
        WitnessOfTheAges,

        // Lands
        JungleHollow,
        RuggedHighlands,
        SwiftwaterCliffs,
        ThornwoodFalls,
        WindScarredCrag,

        // Basic lands
    ) + KhansBasicLands

    val basicLands = KhansBasicLands
}
