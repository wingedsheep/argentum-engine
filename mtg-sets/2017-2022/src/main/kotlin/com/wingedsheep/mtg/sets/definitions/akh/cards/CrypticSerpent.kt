package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Cryptic Serpent
 * {5}{U}{U}
 * Creature — Serpent
 * 6/5
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 */
val CrypticSerpent = card("Cryptic Serpent") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    oracleText = "This spell costs {1} less to cast for each instant and sorcery card in your graveyard."

    power = 6
    toughness = 5

    // Cost reduction: {1} less per instant/sorcery in graveyard
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Lius Lasahido"
        flavorText = "It slithers through the senses, constricting consciousness and poisoning perceptions."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43d11a24-8abf-46ff-8cc6-57b8ac3013f6.jpg?1783936524"
    }
}
