package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed

import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.*

/**
 * Lorwyn Eclipsed Set (2026)
 *
 * A return to the Lorwyn/Shadowmoor plane featuring tribal themes
 * with Elves, Kithkin, Merfolk, Goblins, and Elementals.
 *
 * Set Code: ECL
 * Release Date: January 23, 2026
 * Card Count: 273
 */
object LorwynEclipsedSet {

    const val SET_CODE = "ECL"
    const val SET_NAME = "Lorwyn Eclipsed"

    val allCards = listOf(
        BarbedBloodletter,
        BramblebackBrute,
        Catharsis,
        ChampionsOfThePerfect,
        ChronicleOfVictory,
        ClachanFestival,
        DawnsLightArcher,
        DeepchannelDuelist,
        EclipsedMerrow,
        Emptiness,
        FeistySpikeling,
        GlamerGifter,
        GoldmeadowNomad,
        HovelHurler,
        MirrormindCrown,
        NamelessInversion,
        NoggleTheMind,
        SaplingNursery,
        SteamVents,
        SyggsCommand,
        VirulentEmissary,
        WanderwineFarewell,
        WildUnraveling,
        WildvinePummeler,

        // Basic lands
    ) + LorwynEclipsedBasicLands
}
