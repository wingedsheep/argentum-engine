package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Eternal
 * {2}{U}
 * Creature — Zombie Bird Warrior
 * 2/2
 * Flying
 * When this creature enters, amass Zombies 1.
 */
val AvenEternal = card("Aven Eternal") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Bird Warrior"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, amass Zombies 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(1, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Johan Grenier"
        flavorText = "\"I've never seen the firemane angels so anxious.\"\n—Tajic"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe479484-a827-4d3c-8e35-e905e4f6664a.jpg?1783933467"
    }
}
