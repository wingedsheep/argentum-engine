package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
/**
 * Watcher of the Roost
 * {2}{W}
 * Creature — Bird Soldier
 * 2/1
 * Flying
 * Morph—Reveal a white card in your hand.
 * When this creature is turned face up, you gain 2 life.
 */
val WatcherOfTheRoost = card("Watcher of the Roost") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 1
    oracleText = "Flying\nMorph—Reveal a white card in your hand. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, you gain 2 life."

    keywords(Keyword.FLYING)

    morphCost = Costs.pay.RevealCard(filter = GameObjectFilter.Any.withColor(Color.WHITE))

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8684039-e4d9-4e48-9a41-7ccf4a795507.jpg?1562791631"
    }
}
