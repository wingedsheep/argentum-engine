package com.wingedsheep.mtg.sets.definitions.onslaught

import com.wingedsheep.mtg.sets.definitions.onslaught.cards.*

/**
 * Onslaught Set (2002)
 *
 * Onslaught was the first set in the Onslaught block, featuring tribal themes
 * and introducing mechanics like Morph and Cycling.
 *
 * Set Code: ONS
 * Release Date: October 7, 2002
 * Card Count: 350
 */
object OnslaughtSet {

    const val SET_CODE = "ONS"
    const val SET_NAME = "Onslaught"

    /**
     * All cards implemented from this set.
     */
    val allCards = listOf(
        // White creatures and spells
        AkromasBlessing,
        AkromasVengeance,
        AncestorsProphet,
        CrudeRampart,
        DaruLancer,
        DiscipleOfGrace,
        GlorySeeker,

        // Blue creatures and spells
        AirborneAid,
        Annex,
        AphettoAlchemist,
        AphettoGrifter,
        SageAven,

        // Black creatures and spells
        AccursedCentaur,
        AphettoDredging,
        AphettoVulture,
        AnuridMurkdiver,
        DiscipleOfMalice,
        FesteringGoblin,
        NantukoHusk,
        ScreechingBuzzard,
        SeveredLegion,
        Smother,
        Swat,

        // Red creatures and spells
        AetherCharge,
        AggravatedAssault,
        AirdropCondor,
        BatteringCraghorn,
        ChargingSlateback,
        GoblinSkyRaider,
        GoblinSledder,
        PinpointAvalanche,
        Shock,

        // Green creatures and spells
        AnimalMagnetism,
        BarkhideMauler,
        ElvishVanguard,
        ElvishWarrior,
        SpittingGourna,
        SymbioticBeast,
        SymbioticElf,
        SymbioticWurm,
        TreespringLorian,
        Wellwisher,
        WirewoodElf,
        WirewoodHerald,
        WirewoodSavage,
    )
}
