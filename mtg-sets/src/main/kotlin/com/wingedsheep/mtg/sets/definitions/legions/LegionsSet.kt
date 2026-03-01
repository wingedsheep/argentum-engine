package com.wingedsheep.mtg.sets.definitions.legions

import com.wingedsheep.mtg.sets.definitions.legions.cards.*

/**
 * Legions Set (2003)
 *
 * Legions is the second set in the Onslaught block, notable for being the only
 * set in Magic history to consist entirely of creature cards. It features tribal
 * themes and the morph mechanic.
 *
 * Set Code: LGN
 * Release Date: February 3, 2003
 * Card Count: 145
 */
object LegionsSet {

    const val SET_CODE = "LGN"
    const val SET_NAME = "Legions"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // White creatures
        AvenWarhawk,
        CloudreachCavalry,
        DaruMender,
        DaruSanctifier,
        DeftbladeElite,
        GempalmAvenger,
        Glowrider,
        LiegeOfTheAxe,
        LowlandTracker,
        PlatedSliver,
        StarlightInvoker,
        StoicChampion,
        SwoopingTalon,
        WallOfHope,
        WhiteKnight,
        WingbeatWarrior,

        // Blue creatures
        AvenEnvoy,
        CephalidPathmage,
        CovertOperative,
        DreambornMuse,
        EchoTracer,
        FugitiveWizard,
        GempalmSorcerer,
        GlintwingInvoker,
        KeeneyeAven,
        MerchantOfSecrets,
        MistformSeaswift,
        PrimocEscapee,
        VoidmageApprentice,
        WallOfDeceit,
        Willbender,

        // Black creatures
        AphettoExterminator,
        Earthblighter,
        InfernalCaretaker,
        EmbalmedBrawler,
        DrinkerOfSorrow,
        DrippingDead,
        GempalmPolluter,
        GoblinTurncoat,
        HavocDemon,
        NoxiousGhoul,
        Skinthinner,
        SmokespewInvoker,
        SootfeatherFlock,
        VileDeacon,
        WitheredWretch,
        ZombieBrute,

        // Red creatures
        BladeSliver,
        Clickslither,
        CrestedCraghorn,
        HunterSliver,
        FlamewaveInvoker,
        GoblinGoon,
        FreneticRaptor,
        GempalmIncinerator,
        GoblinFirebug,
        GoblinGrappler,
        LavabornMuse,
        MacetailHystrodon,
        RidgetopRaptor,
        ShaleskinPlower,
        SkirkMarauder,
        SkirkOutrider,

        // Green creatures
        BerserkMurlodont,
        CanopyCrawler,
        BranchsnapLorian,
        GempalmStrider,
        GloweringRogon,
        Brontotherium,
        DefiantElf,
        EnormousBaloth,
        Hundroog,
        KrosanVorine,
        NantukoVigilante,
        NeedleshotGourna,
        PatronOfTheWild,
        StonewoodInvoker,
        TotemSpeaker,
        WirewoodHivemaster,
    )
}
