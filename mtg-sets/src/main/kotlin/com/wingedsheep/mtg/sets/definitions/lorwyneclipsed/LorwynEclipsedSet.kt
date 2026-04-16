package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed

import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.*
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet

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
        AppealToEirdu,
        AquitectsDefenses,
        BarbedBloodletter,
        Blossombind,
        BramblebackBrute,
        Catharsis,
        ChampionsOfThePerfect,
        ChronicleOfVictory,
        ClachanFestival,
        DawnhandEulogist,
        DawnsLightArcher,
        DeepchannelDuelist,
        EclipsedMerrow,
        EirduCarrierOfDawn,
        Emptiness,
        FeistySpikeling,
        FlaringCinder,
        GlamerGifter,
        GoldmeadowNomad,
        HarmonizedCrescendo,
        HovelHurler,
        Lavaleaper,
        MirrormindCrown,
        NamelessInversion,
        NoggleTheMind,
        PrismaticUndercurrents,
        RimefireTorque,
        SaplingNursery,
        SilvergillPeddler,
        SteamVents,
        SyggsCommand,
        VirulentEmissary,
        WanderwineFarewell,
        WildUnraveling,
        WildvinePummeler,
        Wistfulness,

        // Basic lands
    ) + LorwynEclipsedBasicLands

    val basicLands = LorwynEclipsedBasicLands.map { it.copy(setCode = OnslaughtSet.SET_CODE) }
}
