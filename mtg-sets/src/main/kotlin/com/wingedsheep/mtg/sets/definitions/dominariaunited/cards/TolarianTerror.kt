package com.wingedsheep.mtg.sets.definitions.dominariaunited.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SpellCostReduction

/**
 * Tolarian Terror
 * {6}{U}
 * Creature — Serpent
 * 5/5
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 * Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)
 */
val TolarianTerror = card("Tolarian Terror") {
    manaCost = "{6}{U}"
    typeLine = "Creature — Serpent"
    oracleText = "This spell costs {1} less to cast for each instant and sorcery card in your graveyard.\nWard {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)"
    
    power = 5
    toughness = 5

    // Cost reduction: {1} less per instant/sorcery in graveyard
    staticAbility {
        ability = SpellCostReduction(
            reductionSource = CostReductionSource.CardsInGraveyardMatchingFilter(
                filter = GameObjectFilter.Companion.InstantOrSorcery
            )
        )
    }

    // Ward {2}
    keywords(Keyword.WARD)
    keywordAbility(KeywordAbility.ward("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Vincent Christiaens"
        flavorText = "Afterward, a number of treatises on sea serpent morphology were swiftly revised."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42f01cba-43d4-46ad-b7a5-d7631b0e1347.jpg?1673306903"
    }
}
