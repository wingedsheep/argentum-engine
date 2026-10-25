package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Displacer Beast
 * {2}{U}
 * Creature — Cat Beast
 * 3/2
 * When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 * Displacement — {3}{U}: Return this creature to its owner's hand.
 */
val DisplacerBeast = card("Displacer Beast") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Cat Beast"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "Displacement — {3}{U}: Return this creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Bryan Sola"
        flavorText = "The only place you can be sure it's not is wherever it appears to be."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95d5c36c-bcc8-459c-9f4b-b265ccdb1f06.jpg?1783926516"
    }
}
