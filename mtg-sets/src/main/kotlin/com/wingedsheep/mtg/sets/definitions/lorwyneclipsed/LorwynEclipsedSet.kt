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
        AdeptWatershaper,
        AppealToEirdu,
        AquitectsDefenses,
        AuntiesSentence,
        AuroraAwakener,
        BarbedBloodletter,
        BarkOfDoran,
        BileVialBoggart,
        BlightRot,
        BloodCrypt,
        Blossombind,
        BoggartPrankster,
        BoldwyrAggressor,
        BoulderDash,
        BramblebackBrute,
        BrigidsCommand,
        Catharsis,
        ChampionsOfThePerfect,
        ChampionsOfTheShoal,
        ChaosSpewer,
        ChronicleOfVictory,
        ClachanFestival,
        DarknessDescends,
        DawnhandDissident,
        DawnhandEulogist,
        DawnsLightArcher,
        DeepchannelDuelist,
        DreamHarvest,
        EclipsedKithkin,
        EclipsedMerrow,
        EirduCarrierOfDawn,
        Emptiness,
        FeistySpikeling,
        FlaringCinder,
        GlamerGifter,
        GlenElendrasAnswer,
        GloomRipper,
        Goatnap,
        GoldmeadowNomad,
        GreatForestDruid,
        GristleGlutton,
        HallowedFountain,
        HarmonizedCrescendo,
        HeirloomAuntie,
        HovelHurler,
        ImpoliteEntrance,
        Kinbinding,
        KinsbaileAspirant,
        LastingTarfire,
        Lavaleaper,
        LiminalHold,
        LluwenImperfectNaturalist,
        Luminollusk,
        LysAlanaDignitary,
        MerrowSkyswimmer,
        MirrormindCrown,
        MorcantsEyes,
        MorcantsLoyalist,
        Moonshadow,
        NamelessInversion,
        NoggleTheMind,
        Personify,
        PitilessFists,
        PridefulFeastling,
        ProtectiveResponse,
        PrismaticUndercurrents,
        RecklessRansacking,
        RimefireTorque,
        SaplingNursery,
        ShadowUrchin,
        ShimmerwildsGrowth,
        Shinestriker,
        SilvergillPeddler,
        SpryAndMighty,
        SteamVents,
        StoicGroveGuide,
        Stratosoarer,
        SunDappledCelebrant,
        SwatAway,
        SyggsCommand,
        TanufelRimespeaker,
        ThirstForIdentity,
        ThoughtweftCharge,
        TrystansCommand,
        UnwelcomeSprite,
        VinebredBrawler,
        VirulentEmissary,
        VoraciousTomeSkimmer,
        WanderwineFarewell,
        WildUnraveling,
        WildvinePummeler,
        Wistfulness,

        // Basic lands
    ) + LorwynEclipsedBasicLands

    val basicLands = LorwynEclipsedBasicLands.map { it.copy(setCode = OnslaughtSet.SET_CODE) }
}
