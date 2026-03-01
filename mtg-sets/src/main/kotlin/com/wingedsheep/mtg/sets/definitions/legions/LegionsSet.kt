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
        AkromasDevoted,
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
        WindbornMuse,
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
        RiptideMangler,
        ShiftingSliver,
        VoidmageApprentice,
        WallOfDeceit,
        WarpedResearcher,
        Willbender,

        // Black creatures
        AphettoExterminator,
        Earthblighter,
        GravebornMuse,
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
        SpectralSliver,
        VileDeacon,
        WitheredWretch,
        ZombieBrute,

        // Red creatures
        BloodstokeHowler,
        BladeSliver,
        Clickslither,
        CrestedCraghorn,
        HunterSliver,
        FlamewaveInvoker,
        GoblinDynamo,
        GoblinGoon,
        GoblinLookout,
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
        TimberwatchElf,
        TotemSpeaker,
        VexingBeetle,
        WirewoodHivemaster,
    )
}
