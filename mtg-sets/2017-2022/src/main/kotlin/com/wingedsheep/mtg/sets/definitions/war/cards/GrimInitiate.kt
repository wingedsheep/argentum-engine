package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grim Initiate
 * {R}
 * Creature — Zombie Warrior
 * 1/1
 * First strike
 * When this creature dies, amass Zombies 1.
 */
val GrimInitiate = card("Grim Initiate") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Warrior"
    power = 1
    toughness = 1
    oracleText = "First strike\n" +
        "When this creature dies, amass Zombies 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Amass(1, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29b6ec9d-3861-48bf-a198-dc7efba5d89c.jpg?1783933428"
        ruling(
            "2023-06-16",
            "To amass Zombies N, if you don't control an Army creature, create a 0/0 black Zombie Army creature token. Then you choose an Army creature you control and put N +1/+1 counters on it. If that Army isn't already a Zombie, it becomes a Zombie in addition to its other types."
        )
    }
}
