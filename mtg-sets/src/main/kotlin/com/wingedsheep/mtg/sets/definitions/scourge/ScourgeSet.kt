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
        Stabilizer,

        // Black/Red creatures
        BladewingTheRisen,

        // Black creatures
        BladewingsThrall,
        CabalInterrogator,
        CarrionFeeder,
        ConsumptiveGoo,

        // Green creatures
        ElvishAberration,
        FierceEmpath,
        KrosanDrover,
        KrosanWarchief,
        Kurgadon,
        RootElemental,
        TitanicBulvox,
        TreetopScout,
        WirewoodGuardian,
        WirewoodSymbiote,
        Woodcloaker,

        // Green enchantments
        AlphaStatus,
        DragonFangs,
        OneWithNature,
        PrimitiveEtchings,

        // Green instants
        AcceleratedMutation,

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
        DragonWings,
        FacesOfThePast,
        FrozenSolid,

        // Blue instants
        DispersalShield,
        LongTermPlans,
        Metamorphose,

        // Blue sorceries
        RushOfKnowledge,

        // Red creatures
        ChartoothCougar,
        DragonMage,
        DragonspeakerShaman,
        GoblinBrigand,
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
        GoblinWarStrike,
        MisguidedRage,
        TorrentOfFire,

        // Red instants
        Carbonize,
        Enrage,
        SparkSpray,

        // White creatures
        AgelessSentinels,
        AvenFarseer,
        AvenLiberator,
        DaruSpiritualist,
        DaruWarchief,
        DawnElemental,
        Dragonstalker,
        FrontlineStrategist,
        KaronasZealot,
        NobleTemplar,
        SilverKnight,
        ZealousInquisitor,

        // White enchantments
        DragonScales,
        GuiltyConscience,

        // White instants
        GildedLight,
        RainOfBlades,
        Recuperate,
        RewardTheFaithful,
        WipeClean,

        // Black enchantments
        ClutchOfUndeath,
        DragonShadow,
        FatalMutation,
        LingeringDeath,
        UnspeakableSymbol,

        // Black creatures (additional)
        DeathsHeadBuzzard,
        Nefashu,
        PutridRaptor,
        TwistedAbomination,
        UndeadWarchief,
        VengefulDead,
        ZombieCutthroat,

        // Black sorceries
        Skulltap,
        Unburden,

        // Lands
        TempleOfTheFalseGod,
    )
}
