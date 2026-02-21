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
        Kurgadon,
        TitanicBulvox,
        TreetopScout,
        WirewoodGuardian,
        Woodcloaker,

        // Green instants
        AcceleratedMutation,

        // Green sorceries
        BreakAsunder,

        // Blue creatures
        AphettoRunecaster,
        CoastWatcher,
        RiptideSurvivor,
        ScornfulEgotist,
        ShorelineRanger,

        // Blue enchantments
        FrozenSolid,

        // Blue instants
        DispersalShield,

        // Blue sorceries
        RushOfKnowledge,

        // Red creatures
        ChartoothCougar,
        GoblinWarchief,
        RockJockey,
        SiegeGangCommander,

        // Red enchantments
        SulfuricVortex,

        // Red sorceries
        GoblinWarStrike,
        MisguidedRage,

        // Red instants
        Carbonize,
        Enrage,
        SparkSpray,

        // White creatures
        AvenFarseer,
        AvenLiberator,
        DaruWarchief,
        Dragonstalker,
        NobleTemplar,
        SilverKnight,

        // White instants
        RainOfBlades,
        Recuperate,
        WipeClean,

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
