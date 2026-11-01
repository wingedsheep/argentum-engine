package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lazotep Reaver
 * {1}{B}
 * Creature — Zombie Beast
 * 1/2
 * When this creature enters, amass Zombies 1.
 */
val LazotepReaver = card("Lazotep Reaver") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Beast"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, amass Zombies 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(1, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Craig J Spearing"
        flavorText = "Despite their many forms, the Eternals worked in chilling harmony, unburdened by instinct or emotion."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/594bbe43-a8aa-42aa-bc49-cb4f3bc05cad.jpg?1783933443"
    }
}
