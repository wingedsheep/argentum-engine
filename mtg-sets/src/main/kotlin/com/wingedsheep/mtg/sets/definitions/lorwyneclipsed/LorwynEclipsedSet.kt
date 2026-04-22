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
        BloodlineBidding,
        Blossombind,
        BoggartPrankster,
        BoldwyrAggressor,
        BoulderDash,
        BramblebackBrute,
        BrigidsCommand,
        BurdenedStoneback,
        Catharsis,
        ChampionsOfThePerfect,
        ChampionsOfTheShoal,
        ChaosSpewer,
        ChompingChangeling,
        ChronicleOfVictory,
        ClachanFestival,
        DarknessDescends,
        DawnhandDissident,
        DawnhandEulogist,
        DawnsLightArcher,
        DeepchannelDuelist,
        DreamHarvest,
        DreamSeizer,
        EclipsedKithkin,
        EclipsedMerrow,
        EirduCarrierOfDawn,
        Emptiness,
        EvershrikesGift,
        FeistySpikeling,
        FlamekinGildweaver,
        FlaringCinder,
        GlamerGifter,
        GlenElendraGuardian,
        GlenElendrasAnswer,
        GloomRipper,
        Goatnap,
        GoldmeadowNomad,
        Graveshifter,
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
        LochMare,
        Luminollusk,
        LysAlanaDignitary,
        MerrowSkyswimmer,
        Mirrorform,
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
        RaidingSchemes,
        ReapingWillow,
        RecklessRansacking,
        RimefireTorque,
        RiverguardsReflexes,
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
