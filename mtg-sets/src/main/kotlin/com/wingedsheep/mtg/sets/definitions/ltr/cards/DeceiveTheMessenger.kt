package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deceive the Messenger
 * {U}
 * Instant
 *
 * Target creature gets -3/-0 until end of turn.
 * Amass Orcs 1.
 */
val DeceiveTheMessenger = card("Deceive the Messenger") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-0 until end of turn.\n" +
        "Amass Orcs 1. (Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-3, 0, creature)
            .then(Effects.Amass(1, "Orc"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        flavorText = "\"Tell Gandalf that he must seek my aid at once.\"\n—Saruman"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2984bc6-7c12-46e4-8dd3-b23e29a7a7ec.jpg?1686968062"
    }
}
