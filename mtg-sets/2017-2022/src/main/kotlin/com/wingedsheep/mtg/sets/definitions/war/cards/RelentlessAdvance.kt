package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Relentless Advance — War of the Spark #64 (canonical printing)
 * {3}{U}
 * Sorcery
 * Amass Zombies 3.
 */
val RelentlessAdvance = card("Relentless Advance") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Amass Zombies 3. (Put three +1/+1 counters on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    spell {
        effect = Effects.Amass(3, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Stanton Feng"
        flavorText = "They approach in formation, every movement efficient and sure. " +
            "They halt, listen, and then press on in silent unison."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4258e242-91c6-4b30-86ec-7759705aa97c.jpg?1783933458"
        ruling(
            "2023-06-16",
            "In the rare case that you control multiple Army creatures (perhaps because you played a creature with changeling) while you amass Zombies, you choose which of your Army creatures to put the +1/+1 counters on. If that creature isn't a Zombie, it becomes a Zombie in addition to its other types."
        )
        ruling(
            "2023-06-16",
            "If you don't control an Army, the Zombie Army token you create enters the battlefield as a 0/0 creature before receiving counters. Any abilities that trigger when a creature with a certain power enters the battlefield, such as that of Mentor of the Meek, will see the token enter as a 0/0 creature before it gets +1/+1 counters."
        )
    }
}
