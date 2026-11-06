package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rogue Refiner
 * {1}{G}{U}
 * Creature — Human Rogue
 * 3/2
 * When this creature enters, draw a card and you get {E}{E} (two energy counters).
 */
val RogueRefiner = card("Rogue Refiner") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, draw a card and you get {E}{E} (two energy counters)."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Victor Adame Minguez"
        flavorText = "\"It's natural, it's all around us, and it's there for the taking. " +
            "Just try and tell me what I'm doing is wrong.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/618652b4-7ce9-4994-9d16-68d2cc8644ef.jpg?1783936736"
    }
}
