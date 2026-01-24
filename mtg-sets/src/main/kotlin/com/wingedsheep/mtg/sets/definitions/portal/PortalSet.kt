package com.wingedsheep.mtg.sets.definitions.portal

import com.wingedsheep.mtg.sets.definitions.portal.cards.*

/**
 * Portal Set (1997)
 *
 * Portal was an introductory set designed to teach new players
 * the basics of Magic: The Gathering with simplified rules.
 *
 * Set Code: POR
 * Release Date: June 1997
 * Card Count: 222
 */
object PortalSet {

    const val SET_CODE = "POR"
    const val SET_NAME = "Portal"

    /**
     * All cards in this set (first 130 implemented).
     */
    val allCards = listOf(
        // Cards 1-10
        AlabasterDragon,
        AngelicBlessing,
        Archangel,
        ArdentMilitia,
        Armageddon,
        ArmoredPegasus,
        BlessedReversal,
        BlindingLight,
        BorderGuard,
        BreathOfLife,
        // Cards 11-20
        ChargingPaladin,
        DefiantStand,
        DevotedHero,
        FalsePeace,
        FleetFootedMonk,
        FootSoldiers,
        GiftOfEstates,
        HarshJustice,
        KeenEyedArchers,
        KnightErrant,
        // Cards 21-30
        PathOfPeace,
        RegalUnicorn,
        RenewingDawn,
        SacredKnight,
        SacredNectar,
        SeasonedMarshal,
        SpiritualGuardian,
        SpottedGriffin,
        Starlight,
        StarlitAngel,
        // Cards 31-40
        Steadfastness,
        SternMarshal,
        TemporaryTruce,
        ValorousCharge,
        VenerableMonk,
        Vengeance,
        WallOfSwords,
        WarriorsCharge,
        WrathOfGod,
        AncestralMemories,
        // Cards 41-50
        BalanceOfPower,
        BalefulStare,
        CapriciousSorcerer,
        CloakOfFeathers,
        CloudDragon,
        CloudPirates,
        CloudSpirit,
        CommandOfUnsummoning,
        CoralEel,
        CruelFate,
        // Cards 51-60
        DeepSeaSerpent,
        DjinnOfTheLamp,
        DejaVu,
        Exhaustion,
        Flux,
        GiantOctopus,
        HornedTurtle,
        IngeniousThief,
        ManOWar,
        MerfolkOfThePearlTrident,
        // Cards 61-70
        MysticDenial,
        Omen,
        OwlFamiliar,
        PersonalTutor,
        PhantomWarrior,
        Prosperity,
        SnappingDrake,
        SorcerousSight,
        StormCrow,
        SymbolOfUnsummoning,
        // Cards 71-80
        Taunt,
        TheftOfDreams,
        ThingFromTheDeep,
        TidalSurge,
        TimeEbb,
        TouchOfBrilliance,
        WindDrake,
        WitheringGaze,
        ArrogantVampire,
        AssassinsBlade,
        // Cards 81-90
        BogImp,
        BogRaiders,
        BogWraith,
        ChargingBandits,
        CravenKnight,
        CruelBargain,
        CruelTutor,
        DreadCharge,
        DreadReaper,
        DrySpell,
        // Cards 91-100
        EbonDragon,
        EndlessCockroaches,
        FeralShadow,
        FinalStrike,
        Gravedigger,
        HandOfDeath,
        HowlingFury,
        KingsAssassin,
        MercenaryKnight,
        MindKnives,
        // Cards 101-110
        MindRot,
        MuckRats,
        NaturesRuin,
        NoxiousToad,
        Python,
        RainOfTears,
        RaiseDead,
        SerpentAssassin,
        SerpentWarrior,
        SkeletalCrocodile,
        // Cards 111-120
        SkeletalSnake,
        SoulShred,
        UndyingBeast,
        VampiricFeast,
        VampiricTouch,
        VirtuesRuin,
        WickedPact,
        Blaze,
        BoilingSeas,
        BurningCloak,
        // Cards 121-130
        CravenGiant,
        DesertDrake,
        Devastation,
        Earthquake,
        FireDragon,
        FireImp,
        FireSnake,
        FireTempest,
        Flashfires,
        ForkedLightning,
        // Cards 131-140
        GoblinBully,
        HighlandGiant,
        HillGiant,
        HulkingCyclops,
        HulkingGoblin,
        LastChance,
        LavaAxe,
        LavaFlow,
        LizardWarrior,
        MinotaurWarrior
    )

    /**
     * Get a card by name.
     */
    fun getCard(name: String) = allCards.find { it.name == name }

    /**
     * Get a card by collector number.
     */
    fun getCardByNumber(collectorNumber: String) =
        allCards.find { it.metadata.collectorNumber == collectorNumber }
}
