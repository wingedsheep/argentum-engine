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
        BeamsawProspector,
        AuxiliaryBoosters,
        AtomicMicrosizer,
        BiomechanEngineer,
        BiosynthicBurst,
        BloomingStinger,
        Bombard,
        BreedingPool,
        CerebralDownload,
        ChromeCompanion,
        CloudsculptTechnician,
        CometCrawler,
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
        EumidianTerrabotanist,
        ExosuitSavior,
        EvendoWakingHaven,
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
        LumenClassFrigate,
        LostInSpace,
        MeldedMoxite,
        MeltstridersGear,
        MolecularModifier,
        RemnantElemental,
        RigForWar,
        RustHarvester,
        RadiantStrike,
        RerouteSystems,
        SacredFoundry,
        SamisCuriosity,
        SecludedStarforge,
        SeedshipAgrarian,
        ShatteredWings,
        SeamRip,
        SlagdrillScrapper,
        SledgeClassSeedship,
        StompingGround,
        SystemsOverride,
        Thawbringer,
        VirusBeetle,
        WateryGrave,

        // Basic lands
    ) + EdgeOfEternitiesBasicLands
}
