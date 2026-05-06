package com.wingedsheep.mtg.sets.definitions.edgeofeternities

import com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards.*

/**
 * Edge of Eternities Set (2025)
 *
 * Set Code: EOE
 * Release Date: August 1, 2025
 * Card Count: 261
 */
object EdgeOfEternitiesSet {

    const val SET_CODE = "EOE"
    const val SET_NAME = "Edge of Eternities"

    val basicLands = EdgeOfEternitiesBasicLands.map { it.copy(setCode = SET_CODE) }

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        AlpharaelDreamingAcolyte,
        Annul,
        ArchenemysCharm,
        AtmosphericGreenhouse,
        AtomicMicrosizer,
        AuxiliaryBoosters,
        BeamsawProspector,
        BeyondTheQuiet,
        BiomechanEngineer,
        BiosynthicBurst,
        BloomingStinger,
        Bombard,
        BreedingPool,
        BiotechSpecialist,
        CerebralDownload,
        ChromeCompanion,
        CloudsculptTechnician,
        CometCrawler,
        CosmograndZenith,
        CryogenRelic,
        Cryoshatter,
        DarkEndurance,
        DauntlessScrapbot,
        DawnsireSunstarDreadnought,
        DebrisFieldCrusher,
        Depressurize,
        DiplomaticRelations,
        DrillTooDeep,
        DualSunAdepts,
        DualSunTechnique,
        DubiousDelicacy,
        EmbraceOblivion,
        EmergencyEject,
        EmissaryEscort,
        EumidianTerrabotanist,
        EvendoWakingHaven,
        ExosuitSavior,
        GalacticWayfarer,
        GalvanizingSawship,
        GlacierGodmaw,
        GodlessShrine,
        GravbladeHeavy,
        Gravkill,
        GravpackMonoist,
        HardlightContainment,
        HarmoniousGrovestrider,
        HemosymbicMite,
        Honor,
        HonoredKnightCaptain,
        Hullcarver,
        IcecaveCrasher,
        IllvoiGaleblade,
        IllvoiLightJammer,
        IllvoiOperative,
        IntrepidTenderfoot,
        InvasiveManeuvers,
        KavaronHarrier,
        KavaronTurbodrone,
        LightlessEvangel,
        LostInSpace,
        LumenClassFrigate,
        MechanAssembler,
        MechanNavigator,
        MeldedMoxite,
        MeltstridersGear,
        MeltstridersResolve,
        MmmenonUthrosExile,
        MolecularModifier,
        MonoistCircuitFeeder,
        MonoistSentry,
        MouthOfTheStorm,
        NanoformSentinel,
        NebulaDragon,
        NutrientBlock,
        PinnacleKillShip,
        RadiantStrike,
        RemnantElemental,
        RerouteSystems,
        RigForWar,
        RustHarvester,
        SacredFoundry,
        SamisCuriosity,
        SeamRip,
        SecludedStarforge,
        SeedshipAgrarian,
        ShatteredWings,
        SlagdrillScrapper,
        SledgeClassSeedship,
        StarfighterPilot,
        StompingGround,
        SunstarExpansionist,
        SunstarLightsmith,
        SyrVondamTheLucent,
        SystemsOverride,
        Thawbringer,
        VirusBeetle,
        WateryGrave,
        WedgelightRammer,
        ZookeeperMechan,

        // Basic lands
    ) + EdgeOfEternitiesBasicLands
}
