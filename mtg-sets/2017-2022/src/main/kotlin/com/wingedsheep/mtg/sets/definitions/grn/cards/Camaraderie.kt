package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Camaraderie
 * {4}{G}{W}
 * Sorcery
 * You gain X life and draw X cards, where X is the number of creatures you control.
 * Creatures you control get +1/+1 until end of turn.
 */
val Camaraderie = card("Camaraderie") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Sorcery"
    oracleText = "You gain X life and draw X cards, where X is the number of creatures you control. " +
        "Creatures you control get +1/+1 until end of turn."

    spell {
        effect = Effects.GainLife(DynamicAmounts.creaturesYouControl()) then
            Effects.DrawCards(DynamicAmounts.creaturesYouControl()) then
            Patterns.Group.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Within the song of Mat'Selesnya, one becomes all.\"\n—Heruj, Selesnya hierophant"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/890a2fa9-1141-4a4c-85af-05723aba5e39.jpg?1783934140"
        ruling("2018-10-05", "The second part of Camaraderie's effect affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't get +1/+1.")
    }
}
