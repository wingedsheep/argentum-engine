package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hypnotic Grifter
 * {U}
 * Creature — Human Rogue
 * 1 / 2
 * {3}: This creature connives.
 */
val HypnoticGrifter = card("Hypnotic Grifter") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "{3}: This creature connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Josu Hernaiz"
        flavorText = "\"All is calm. All is well. You know me to be a trusted member of your family, and I would like to discuss the contents of our safe.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69af30b3-d4b0-453a-891f-4506e06b5e14.jpg"
    }
}
