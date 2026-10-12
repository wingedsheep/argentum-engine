package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tocasia's Welcome
 * {2}{W}
 * Enchantment
 * Whenever one or more creatures you control with mana value 3 or less enter, draw a card.
 * This ability triggers only once each turn.
 *
 * The batching `Triggers.oneOrMore(filter).enter()` (Caretaker's Talent shape) with
 * `oncePerTurn` for "triggers only once each turn".
 */
val TocasiasWelcome = card("Tocasia's Welcome") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creatures you control with mana value 3 or less enter, draw a card. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.manaValueAtMost(3).youControl()).enter()
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Johan Grenier"
        flavorText = "\"Your father was a dear friend to whom I owe much. He wanted me to look after you, to care for you should something happen to him.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56cd89f1-f9f4-4cb5-a573-79809d0b6dfd.jpg?1783920123"
    }
}
