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
        TreetopScout,
        WirewoodGuardian,

        // Green instants
        AcceleratedMutation,

        // Green sorceries
        BreakAsunder,

        // Blue creatures
        CoastWatcher,
        RiptideSurvivor,
        ScornfulEgotist,
        ShorelineRanger,

        // Blue instants
        DispersalShield,

        // Blue sorceries
        RushOfKnowledge,

        // Red creatures
        ChartoothCougar,
        GoblinWarchief,
        SiegeGangCommander,

        // Red enchantments
        SulfuricVortex,

        // Red sorceries
        GoblinWarStrike,
        MisguidedRage,

        // Red instants
        Carbonize,
        SparkSpray,

        // White creatures
        AvenLiberator,
        DaruWarchief,
        Dragonstalker,
        NobleTemplar,
        SilverKnight,

        // White instants
        RainOfBlades,
        WipeClean,

        // Black creatures (additional)
        DeathsHeadBuzzard,
        TwistedAbomination,
        UndeadWarchief,
        VengefulDead,
        ZombieCutthroat,

        // Black sorceries
        Unburden,
    )
}
