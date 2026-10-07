package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

val SaltwaterStalwart = card("Saltwater Stalwart") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Warrior"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature deals damage to an opponent, target player draws a card."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        val player = target(Targets.Player)
        effect = Effects.DrawCards(1, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Sam Burley"
        flavorText = "Every attack is a test of your defenses. Every hit marks the map of your defeat."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae11adef-57b1-4f39-951b-cd4d5c0a0b80.jpg?1783934865"
    }
}
