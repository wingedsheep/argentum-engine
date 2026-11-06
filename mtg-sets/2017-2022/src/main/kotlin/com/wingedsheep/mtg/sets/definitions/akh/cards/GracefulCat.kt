package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Graceful Cat
 * {2}{W}
 * Creature — Cat
 * 2/2
 * Whenever this creature attacks, it gets +1/+1 until end of turn.
 */
val GracefulCat = card("Graceful Cat") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    oracleText = "Whenever this creature attacks, it gets +1/+1 until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        inBooster = false
        rarity = Rarity.COMMON
        collectorNumber = "273"
        artist = "John Stanko"
        flavorText = "Though they are held in high regard as symbols of the god Oketra, cats often lack her sense of solidarity."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b42ba8bf-9fc1-4d57-9c80-42491d18d929.jpg?1783936438"
    }
}
