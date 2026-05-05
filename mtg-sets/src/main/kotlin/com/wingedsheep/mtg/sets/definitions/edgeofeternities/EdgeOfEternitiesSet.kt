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
        Annul,
        ArchenemysCharm,
        AtmosphericGreenhouse,
        AtomicMicrosizer,
        AuxiliaryBoosters,
        BeamsawProspector,
        BiomechanEngineer,
        BiosynthicBurst,
        BloomingStinger,
        Bombard,
        BreedingPool,
        CerebralDownload,
        ChromeCompanion,
        CloudsculptTechnician,
        CometCrawler,
        CosmograndZenith,
        CryogenRelic,
        Cryoshatter,
        DarkEndurance,
        Depressurize,
        DiplomaticRelations,
        DrillTooDeep,
        DualSunAdepts,
        DualSunTechnique,
        DubiousDelicacy,
        EmbraceOblivion,
        EmergencyEject,
        EumidianTerrabotanist,
        EvendoWakingHaven,
        ExosuitSavior,
        GalacticWayfarer,
        GalvanizingSawship,
        GlacierGodmaw,
        GodlessShrine,
        GravbladeHeavy,
        GravpackMonoist,
        HardlightContainment,
        HarmoniousGrovestrider,
        HemosymbicMite,
        Honor,
        HonoredKnightCaptain,
        Hullcarver,
        IcecaveCrasher,
        IllvoiGaleblade,
        IntrepidTenderfoot,
        KavaronHarrier,
        KavaronTurbodrone,
        LostInSpace,
        LumenClassFrigate,
        MechanNavigator,
        MeldedMoxite,
        MeltstridersGear,
        MolecularModifier,
        MonoistSentry,
        RadiantStrike,
        RemnantElemental,
        RerouteSystems,
        RigForWar,
        RustHarvester,
        SacredFoundry,
        SamisCuriosity,
        SecludedStarforge,
        SeedshipAgrarian,
        ShatteredWings,
        SeamRip,
        SlagdrillScrapper,
        SledgeClassSeedship,
        StompingGround,
        StarfighterPilot,
        SunstarExpansionist,
        SunstarLightsmith,
        SystemsOverride,
        Thawbringer,
        VirusBeetle,
        WedgelightRammer,
        WateryGrave,

        // Basic lands
    ) + EdgeOfEternitiesBasicLands
}
