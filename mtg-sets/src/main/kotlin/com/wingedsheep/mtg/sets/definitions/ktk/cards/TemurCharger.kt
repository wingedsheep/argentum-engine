package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
/**
 * Temur Charger
 * {1}{G}
 * Creature — Horse
 * 3/1
 * Morph—Reveal a green card in your hand.
 * When Temur Charger is turned face up, target creature gains trample until end of turn.
 */
val TemurCharger = card("Temur Charger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Horse"
    power = 3
    toughness = 1
    oracleText = "Morph—Reveal a green card in your hand. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Temur Charger is turned face up, target creature gains trample until end of turn."

    morphCost = Costs.pay.RevealCard(filter = GameObjectFilter.Any.withColor(Color.GREEN))

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val creature = target("creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77258fc-8683-4656-84a3-4df4ea2a8435.jpg?1562796176"
    }
}
