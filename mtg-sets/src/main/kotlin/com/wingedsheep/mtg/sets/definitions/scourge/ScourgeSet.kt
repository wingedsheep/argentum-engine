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

        // Black/Red creatures
        BladewingTheRisen,

        // Black creatures
        CarrionFeeder,

        // Green creatures
        ElvishAberration,
        FierceEmpath,
        KrosanDrover,
        KrosanWarchief,
        Kurgadon,
        TitanicBulvox,
        TreetopScout,
        WirewoodGuardian,
        Woodcloaker,

        // Green enchantments
        DragonFangs,
        OneWithNature,

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
        FrozenSolid,

        // Blue instants
        DispersalShield,

        // Blue sorceries
        RushOfKnowledge,

        // Red creatures
        ChartoothCougar,
        DragonspeakerShaman,
        GoblinBrigand,
        GoblinWarchief,
        RockJockey,
        SiegeGangCommander,

        // Red enchantments
        DragonBreath,
        PyrostaticPillar,
        SulfuricVortex,

        // Red sorceries
        GoblinWarStrike,
        MisguidedRage,
        TorrentOfFire,

        // Red instants
        Carbonize,
        Enrage,
        SparkSpray,

        // White creatures
        AvenFarseer,
        AvenLiberator,
        DaruSpiritualist,
        DaruWarchief,
        Dragonstalker,
        FrontlineStrategist,
        NobleTemplar,
        SilverKnight,

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
        PutridRaptor,
        TwistedAbomination,
        UndeadWarchief,
        VengefulDead,
        ZombieCutthroat,

        // Black sorceries
        Skulltap,
        Unburden,
    )
}
