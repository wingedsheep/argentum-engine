package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enterprising Scallywag — {1}{R}
 * Creature — Goblin Pirate
 * 2/2
 * At the beginning of your end step, if you descended this turn, create a Treasure token.
 */
val EnterprisingScallywag = card("Enterprising Scallywag") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    oracleText = "At the beginning of your end step, if you descended this turn, create a Treasure token. (You descended if a permanent card was put into your graveyard from anywhere.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouDescendedThisTurn()
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Denman Rooke"
        flavorText = "\"He would've wanted me to have it!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44420f52-2ed8-4f81-93e4-5decc77bed01.jpg?1782694491"
    }
}
