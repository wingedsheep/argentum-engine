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
        CloudreachCavalry,
        DaruSanctifier,
        LowlandTracker,
        PlatedSliver,
        StarlightInvoker,
        WallOfHope,
        WhiteKnight,
        WingbeatWarrior,

        // Blue creatures
        CephalidPathmage,
        CovertOperative,
        EchoTracer,
        FugitiveWizard,
        GlintwingInvoker,
        KeeneyeAven,
        MerchantOfSecrets,

        // Black creatures
        DrinkerOfSorrow,
        DrippingDead,
        GoblinTurncoat,
        Skinthinner,
        SmokespewInvoker,
        SootfeatherFlock,
        VileDeacon,
        WitheredWretch,

        // Red creatures
        CrestedCraghorn,
        FlamewaveInvoker,
        GoblinFirebug,
        GoblinGrappler,
        LavabornMuse,
        RidgetopRaptor,
        SkirkMarauder,
        SkirkOutrider,

        // Green creatures
        Brontotherium,
        DefiantElf,
        EnormousBaloth,
        Hundroog,
        NantukoVigilante,
        NeedleshotGourna,
        PatronOfTheWild,
        StonewoodInvoker,
    )
}
