package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lotleth Giant
 * {6}{B}
 * Creature — Zombie Giant
 * 6/5
 * Undergrowth — When this creature enters, it deals 1 damage to target opponent for each
 * creature card in your graveyard.
 */
val LotlethGiant = card("Lotleth Giant") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Giant"
    oracleText = "Undergrowth — When this creature enters, it deals 1 damage to target opponent for each creature card in your graveyard."
    power = 6
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(DynamicAmounts.creatureCardsInYourGraveyard(), opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Alex Konstad"
        flavorText = "\"I prefer the big and looming to the small and skittering.\"\n—Cevraya, Golgari shaman"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1dc38b7-52f7-4394-b095-5442429ab291.jpg?1783934174"
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
    }
}
