package com.wingedsheep.mtg.sets.definitions.bloomburrow

import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.*

/**
 * Bloomburrow Set (2024)
 *
 * Bloomburrow is a plane inhabited entirely by anthropomorphic animals.
 * The set features a variety of creature types including Mice, Rabbits,
 * Frogs, Otters, and more.
 *
 * Set Code: BLB
 * Release Date: August 2, 2024
 * Card Count: 266
 */
object BloomburrowSet {

    const val SET_CODE = "BLB"
    const val SET_NAME = "Bloomburrow"

    /**
     * All cards implemented from this set.
     */
    val basicLands = BloomburrowBasicLands.map { it.copy(setCode = SET_CODE) }

    val allCards = listOf(
        AgateBladeAssassin,
        ArtistsTalent,
        BarkKnuckleBoxer,
        BarkformHarvester,
        BaylenTheHaymaker,
        BellowingCrier,
        BonebindOrator,
        BrambleguardCaptain,
        BrambleguardVeteran,
        BrazenCollector,
        BraveKinDuo,
        BywayBarterer,
        ClementTheWorrywort,
        ConductElectricity,
        CamelliaTheSeedmiser,
        CorpseberryCultivator,
        CrumbAndGetIt,
        DaggerfangDuo,
        DarkstarAugur,
        DazzlingDenial,
        DawnsTruce,
        DireDowndraft,
        DreamdewEntrancer,
        DruidOfTheSpade,
        DriftgloomCoyote,
        EarlyWinter,
        EmberheartChallenger,
        EddymurkCrab,
        FabledPassage,
        Fell,
        FeatherOfFlight,
        FecundGreenshell,
        FlamecacheGecko,
        FrilledSparkshooter,
        FlowerfootSwordmaster,
        GlidediveDuo,
        HeapedHarvest,
        HeirloomEpic,
        HelgaSkittishSeer,
        HiddenGrotto,
        HiredClaw,
        HopToIt,
        HonoredDreyleader,
        IntrepidRabbit,
        IntoTheFloodMaw,
        IridescentVinelasher,
        JackdawSavior,
        JunkbladeBruiser,
        KindlesparkDuo,
        Knightfisher,
        HuntersTalent,
        Mindwhisker,
        MindSpiral,
        LifecreedDuo,
        LilypadVillage,
        LupinflowerVillage,
        MabelsMettle,
        ManifoldMouse,
        MoonriseCleric,
        MoonstoneHarbinger,
        MouseTrapper,
        MuerraTrashTactician,
        NocturnalHunger,
        NettleGuard,
        NightwhorlHermit,
        PatchworkBanner,
        PileatedProvisioner,
        Overprotect,
        OsteomancerAdept,
        PondProphet,
        PlayfulShove,
        SalvationSwan,
        RuthlessNegotiation,
        QuaketuskBoar,
        RavineRaider,
        RepelCalamity,
        ReptilianRecruiter,
        RunAwayTogether,
        SazacapsBrew,
        ScavengersTalent,
        SeasonOfLoss,
        SeasonedWarrenguard,
        SeedglaiveMentor,
        ShoreUp,
        ShorelineLooter,
        SinisterMonolith,
        SonarStrike,
        SplashLasher,
        StickytongueSentinel,
        SugarCoat,
        SteampathCharger,
        StormcatchMentor,
        StormchasersTalent,
        SunspineLynx,
        TenderWildguide,
        ThistledownPlayers,
        TidecallerMentor,
        ThornplateIntimidator,
        ThornvaultForager,
        ThreeTreeRootweaver,
        ThundertrapTrainer,
        ValleyFloodcaller,
        ValleyMightcaller,
        ValleyQuestcaller,
        ValleyRally,
        ValleyRotcaller,
        WarrenWarleader,
        WaxWaneWitness,
        WearDown,
        WhiskervaleForerunner,
        WildfireHowl,
        WickTheWhorledMind,
        YgraEaterOfAll,
        ZoralineCosmosCaller,

        // Basic lands
    ) + BloomburrowBasicLands
}
