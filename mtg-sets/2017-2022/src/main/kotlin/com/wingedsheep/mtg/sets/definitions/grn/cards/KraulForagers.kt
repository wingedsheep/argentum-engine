package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kraul Foragers
 * {4}{G}
 * Creature — Insect Scout
 * 4/4
 * Undergrowth — When this creature enters, you gain 1 life for each creature card in your graveyard.
 */
val KraulForagers = card("Kraul Foragers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Scout"
    oracleText = "Undergrowth — When this creature enters, you gain 1 life for each creature card in your graveyard."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.creatureCardsInYourGraveyard())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Aaron Miller"
        flavorText = "Feed on food, you eventually rot. Feed on rot, you live forever.\n—Kraul saying"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ed4b08-8583-4ad4-b0ba-0463d367efc8.jpg?1783934150"
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
    }
}
