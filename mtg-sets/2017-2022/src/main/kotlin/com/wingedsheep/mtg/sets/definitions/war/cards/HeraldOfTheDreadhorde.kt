package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Herald of the Dreadhorde
 * {3}{B}
 * Creature — Zombie Warrior
 * 3/2
 * When this creature dies, amass Zombies 2.
 */
val HeraldOfTheDreadhorde = card("Herald of the Dreadhorde") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warrior"
    power = 3
    toughness = 2
    oracleText = "When this creature dies, amass Zombies 2. (Put two +1/+1 counters on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Amass(2, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Heonhwa"
        flavorText = "Amonkhet's dead flowed into a new world in service to their God-Pharaoh."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7ae9cb7-a3af-4224-89a5-a72f4d09dacf.jpg?1783933444"
        ruling(
            "2023-06-16",
            "To amass Zombies N, if you don't control an Army creature, create a 0/0 black Zombie Army creature token. Then you choose an Army creature you control and put N +1/+1 counters on it. If that Army isn't already a Zombie, it becomes a Zombie in addition to its other types."
        )
    }
}
