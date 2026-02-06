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
        AkromasBlessing,
        AkromasVengeance,
        AncestorsProphet,
        AstralSlide,
        AuraExtraction,
        CatapultSquad,
        CrudeRampart,
        Demystify,
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
        AphettoDredging,
        AphettoVulture,
        AnuridMurkdiver,
        CabalArchon,
        CruelRevival,
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
        ScreechingBuzzard,
        SeveredLegion,
        Smother,
        Swat,
        SyphonSoul,
        VisaraTheDreadful,
        WretchedAnurid,

        // Red creatures and spells
        AetherCharge,
        BrightstoneRitual,
        AggravatedAssault,
        AirdropCondor,
        BatteringCraghorn,
        ChargingSlateback,
        FlamestickCourier,
        GoblinSkyRaider,
        GoblinSledder,
        GoblinTaskmaster,
        LayWaste,
        LightningRift,
        PinpointAvalanche,
        RorixBladewing,
        SearingFlesh,
        Shock,
        SliceAndDice,
        Sparksmith,
        SkirkProspector,
        SpurredWolverine,

        // Green creatures and spells
        AnimalMagnetism,
        EvergloveCourier,
        KrosanColossus,
        KrosanGroundshaker,
        MythicProportions,
        Naturalize,
        RavenousBaloth,
        SnarlingUndorak,
        VenomspoutBrackus,
        BarkhideMauler,
        ElvishVanguard,
        ElvishWarrior,
        SpittingGourna,
        SymbioticBeast,
        SymbioticElf,
        SymbioticWurm,
        ToweringBaloth,
        TreespringLorian,
        Wellwisher,
        WirewoodElf,
        WirewoodHerald,
        WirewoodSavage,

        // Lands
        BarrenMoor,

        // Basic lands
    ) + OnslaughtBasicLands
}
