package com.wingedsheep.mtg.sets.definitions.scourge

import com.wingedsheep.mtg.sets.definitions.scourge.cards.*

/**
 * Scourge Set (2003)
 *
 * Scourge was the third and final set in the Onslaught block, featuring
 * the Storm mechanic and heavy tribal themes including Dragons.
 *
 * Set Code: SCG
 * Release Date: May 26, 2003
 * Card Count: 143
 */
object ScourgeSet {

    const val SET_CODE = "SCG"
    const val SET_NAME = "Scourge"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // Artifacts
        ArkOfBlight,
        ProteusMachine,
        Stabilizer,

        // White/Black creatures
        Edgewalker,

        // Black/Red creatures
        BladewingTheRisen,

        // Black creatures
        BladewingsThrall,
        CabalInterrogator,
        CarrionFeeder,
        ConsumptiveGoo,

        // Green creatures
        AncientOoze,
        ElvishAberration,
        FierceEmpath,
        ForgottenAncient,
        KrosanDrover,
        KrosanWarchief,
        Kurgadon,
        RootElemental,
        TitanicBulvox,
        TreetopScout,
        WirewoodGuardian,
        WirewoodSymbiote,
        Woodcloaker,
        XantidSwarm,

        // Green enchantments
        AlphaStatus,
        DragonFangs,
        OneWithNature,
        PrimitiveEtchings,

        // Green instants
        AcceleratedMutation,
        DecreeOfSavagery,
        DivergentGrowth,
        HuntingPack,
        SproutingVines,

        // Green sorceries
        BreakAsunder,
        ClawsOfWirewood,

        // Blue creatures
        AphettoRunecaster,
        CoastWatcher,
        MercurialKite,
        MistformWarchief,
        RavenGuildInitiate,
        RavenGuildMaster,
        RiptideSurvivor,
        ScornfulEgotist,
        ShorelineRanger,

        ThundercloudElemental,

        // Blue enchantments
        DecreeOfSilence,
        DragonWings,
        FacesOfThePast,
        FrozenSolid,
        PemminsAura,

        // Blue instants
        BrainFreeze,
        DispersalShield,
        HinderingTouch,
        LongTermPlans,
        Metamorphose,
        Stifle,

        // Blue sorceries
        RushOfKnowledge,
        TemporalFissure,

        // Red creatures
        BonethornValesk,
        ChartoothCougar,
        DragonMage,
        DragonspeakerShaman,
        DragonTyrant,
        GoblinBrigand,
        GoblinPsychopath,
        GoblinWarchief,
        RockJockey,
        SiegeGangCommander,
        SkirkVolcanist,

        // Red enchantments
        DragonBreath,
        ExtraArms,
        PyrostaticPillar,
        SulfuricVortex,
        UncontrolledInfestation,

        // Red sorceries
        Dragonstorm,
        GoblinWarStrike,
        MisguidedRage,
        TorrentOfFire,

        // Red instants
        Carbonize,
        Enrage,
        Scattershot,
        SparkSpray,

        // White creatures
        AgelessSentinels,
        AvenFarseer,
        ExiledDoomsayer,
        EternalDragon,
        AvenLiberator,
        DaruSpiritualist,
        DaruWarchief,
        DawnElemental,
        Dragonstalker,
        FrontlineStrategist,
        KaronasZealot,
        NobleTemplar,
        SilverKnight,
        TrapDigger,
        ZealousInquisitor,

        // White enchantments
        DragonScales,
        GuiltyConscience,

        // White sorceries
        DecreeOfJustice,

        // White instants
        AstralSteel,
        GildedLight,
        RainOfBlades,
        Recuperate,
        RewardTheFaithful,
        WipeClean,
        WingShards,

        // Black enchantments
        CallToTheGrave,
        ClutchOfUndeath,
        DragonShadow,
        FatalMutation,
        LethalVapors,
        LingeringDeath,
        UnspeakableSymbol,

        // Black creatures (additional)
        DeathsHeadBuzzard,
        Nefashu,
        PutridRaptor,
        SoulCollector,
        TwistedAbomination,
        UndeadWarchief,
        VengefulDead,
        ZombieCutthroat,

        // Black sorceries
        CabalConditioning,
        DecreeOfPain,
        FinalPunishment,
        Skulltap,
        TendrilsOfAgony,
        Unburden,

        // Black instants
        ChillHaunting,

        // Lands
        TempleOfTheFalseGod,
    )
}
