package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Saruman's Trickery
 * {1}{U}{U}
 * Instant
 *
 * Counter target spell.
 * Amass Orcs 1.
 */
val SarumansTrickery = card("Saruman's Trickery") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\n" +
        "Amass Orcs 1. (Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        target("target spell", Targets.Spell)
        effect = Effects.CounterSpell()
            .then(Effects.Amass(1, "Orc"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        flavorText = "\"I gave you the chance of aiding me willingly.\"\n—Saruman"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eb6c23b-e52e-4533-9625-884eb0a4d866.jpg?1686968278"
    }
}
