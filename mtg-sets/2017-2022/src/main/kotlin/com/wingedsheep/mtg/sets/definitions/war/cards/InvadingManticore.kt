package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Invading Manticore
 * {5}{R}
 * Creature — Zombie Manticore
 * 4/5
 * When this creature enters, amass Zombies 2.
 */
val InvadingManticore = card("Invading Manticore") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Manticore"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, amass Zombies 2. (Put two +1/+1 counters on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(2, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674e408b-f052-451c-9bf9-ac6f98753a9d.jpg?1783933427"
        ruling(
            "2023-06-16",
            "To amass Zombies N, if you don't control an Army creature, create a 0/0 black Zombie Army creature token. Then you choose an Army creature you control and put N +1/+1 counters on it. If that Army isn't already a Zombie, it becomes a Zombie in addition to its other types."
        )
    }
}
