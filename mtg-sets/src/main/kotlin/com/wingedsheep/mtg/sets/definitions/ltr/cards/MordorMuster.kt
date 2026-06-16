package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mordor Muster
 * {1}{B}
 * Sorcery
 *
 * You draw a card and you lose 1 life.
 * Amass Orcs 1. (Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't
 * control an Army, create a 0/0 black Orc Army creature token first.)
 */
val MordorMuster = card("Mordor Muster") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw a card and you lose 1 life.\n" +
        "Amass Orcs 1. (Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        effect = Effects.DrawCards(1)
            .then(Effects.LoseLife(1, EffectTarget.Controller))
            .then(Effects.Amass(1, "Orc"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        flavorText = "\"Orcs, thousands of Orcses. Nice Hobbits mustn't go to those places.\"\n—Gollum"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aae2f15a-0629-453e-992c-a199af194a3c.jpg?1686968586"
    }
}
