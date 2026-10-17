package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Maestros Ascendancy
 * {U}{B}{R}
 * Enchantment
 *
 * Once during each of your turns, you may cast an instant or sorcery spell from your graveyard by
 * sacrificing a creature in addition to paying its other costs. If a spell cast this way would be
 * put into your graveyard, exile it instead.
 *
 * One graveyard-cast grant: once per turn, your turns only, a sacrifice-a-creature additional cost
 * owed only when the cast goes through this grant, and the exile-instead rider for spells cast
 * this way (resolved, countered, or fizzled).
 */
val MaestrosAscendancy = card("Maestros Ascendancy") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Enchantment"
    oracleText = "Once during each of your turns, you may cast an instant or sorcery spell from your " +
        "graveyard by sacrificing a creature in addition to paying its other costs. If a spell cast " +
        "this way would be put into your graveyard, exile it instead."

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.InstantOrSorcery,
            duringYourTurnOnly = true,
            oncePerTurn = true,
            exileInsteadOfGraveyard = true,
            additionalCost = Costs.additional.SacrificePermanent(GameObjectFilter.Creature)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/469a0f03-7874-453d-b9e4-275c13244f4a.jpg?1783923080"
        ruling("2022-04-29", "If you cast an Adventure spell from your graveyard this way, it's exiled as it resolves with the replacement effect created by the Adventure rules and you may cast the creature card from exile later.")
    }
}
