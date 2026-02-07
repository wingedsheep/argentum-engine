package com.wingedsheep.mtg.sets.definitions.onslaught

import com.wingedsheep.mtg.sets.definitions.onslaught.cards.*

/**
 * Onslaught Set (2002)
 *
 * Onslaught was the first set in the Onslaught block, featuring tribal themes
 * and introducing mechanics like Morph and Cycling.
 *
 * Set Code: ONS
 * Release Date: October 7, 2002
 * Card Count: 350
 */
object OnslaughtSet {

    const val SET_CODE = "ONS"
    const val SET_NAME = "Onslaught"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // White creatures and spells
        BattlefieldMedic,
        AvenBrigadier,
        AvenSoulgazer,
        AkromasBlessing,
        DoubtlessOne,
        AkromasVengeance,
        AncestorsProphet,
        AstralSlide,
        AuraExtraction,
        Aurification,
        CatapultSquad,
        CrudeRampart,
        Demystify,
        FoothillGuide,
        GrasslandCrusader,
        GravelSlinger,
        ImprovisedArmor,
        Inspirit,
        Pacifism,
        RenewedFaith,
        DaruLancer,
        DiscipleOfGrace,
        GlorySeeker,
        PearlspearCourier,
        Whipcorder,

        // Blue creatures and spells
        AirborneAid,
        Annex,
        AvenFateshaper,
        Backslide,
        BlatantThievery,
        CraftyPathmage,
        GhosthelmCourier,
        ThoughtboundPrimoc,
        NamelessOne,
        AphettoAlchemist,
        AphettoGrifter,
        ArcanisTheOmnipotent,
        ArtificialEvolution,
        AscendingAven,
        ChokingTethers,
        SageAven,
        SlipstreamEel,

        // Black creatures and spells
        AccursedCentaur,
        Blackmail,
        Boneknitter,
        AphettoDredging,
        DirgeOfDread,
        FrightshroudCourier,
        Headhunter,
        AphettoVulture,
        AnuridMurkdiver,
        CabalArchon,
        CruelRevival,
        DoomedNecromancer,
        DiscipleOfMalice,
        FadeFromMemory,
        FeedingFrenzy,
        FesteringGoblin,
        GluttonousZombie,
        GrinningDemon,
        Infest,
        ProfanePrayers,
        ShepherdOfRot,
        SpinedBasher,
        NantukoHusk,
        SoullessOne,
        ScreechingBuzzard,
        SeveredLegion,
        Smother,
        Swat,
        SyphonSoul,
        VisaraTheDreadful,
        WretchedAnurid,

        // Red creatures and spells
        AetherCharge,
        Avarax,
        BlisteringFirecat,
        ButcherOrgg,
        BreakOpen,
        BrightstoneRitual,
        SkirkCommando,
        AggravatedAssault,
        AirdropCondor,
        BatteringCraghorn,
        ChargingSlateback,
        DragonRoost,
        EmbermageGoblin,
        FlamestickCourier,
        GoblinPiledriver,
        GoblinSharpshooter,
        GoblinSkyRaider,
        GoblinSledder,
        GoblinTaskmaster,
        LayWaste,
        LightningRift,
        PinpointAvalanche,
        RecklessOne,
        RorixBladewing,
        SearingFlesh,
        Shock,
        SliceAndDice,
        SolarBlast,
        Sparksmith,
        SkirkProspector,
        SpurredWolverine,

        // Green creatures and spells
        AnimalMagnetism,
        BirchloreRangers,
        BloodlineShaman,
        Biorhythm,
        BroodhatchNantuko,
        ElvenRiders,
        ElvishPioneer,
        ElvishScrapper,
        EvergloveCourier,
        ExplosiveVegetation,
        InvigoratingBoon,
        KrosanColossus,
        KrosanGroundshaker,
        LeeryFogbeast,
        KrosanTusker,
        MythicProportions,
        Naturalize,
        RavenousBaloth,
        SnarlingUndorak,
        VenomspoutBrackus,
        BarkhideMauler,
        ElvishVanguard,
        ElvishWarrior,
        HeedlessOne,
        SpittingGourna,
        SymbioticBeast,
        TauntingElf,
        SymbioticElf,
        SymbioticWurm,
        ToweringBaloth,
        TreespringLorian,
        Wellwisher,
        WirewoodElf,
        WirewoodHerald,
        WirewoodSavage,

        // Lands
        BloodstainedMire,
        BarrenMoor,
        ForgottenCave,
        GoblinBurrows,
        LonelySandbar,
        SecludedSteppe,
        TranquilThicket,

        // Basic lands
    ) + OnslaughtBasicLands
}
