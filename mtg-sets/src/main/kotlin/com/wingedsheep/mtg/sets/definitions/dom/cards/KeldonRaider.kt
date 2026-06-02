package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Keldon Raider
 * {2}{R}{R}
 * Creature — Human Warrior
 * 4/3
 * When Keldon Raider enters the battlefield, you may discard a card. If you do, draw a card.
 */
val KeldonRaider = card("Keldon Raider") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "When Keldon Raider enters the battlefield, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.Composite(listOf(
                HandPatterns.discardCards(1),
                Effects.DrawCards(1)
            ))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Chris Seaman"
        flavorText = "Keldon raiders' spoils are limited to what their colos can carry. No matter the value, the rest goes up in smoke."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1245212-d081-4e95-a508-e1d3a7496473.jpg?1562745383"
    }
}
