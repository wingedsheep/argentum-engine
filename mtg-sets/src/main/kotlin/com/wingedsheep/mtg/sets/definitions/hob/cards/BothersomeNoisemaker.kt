package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bothersome Noisemaker
 * {1}{R}
 * Creature — Goblin Bard
 * 2/2
 *
 * Whenever you cast a noncreature spell, amass Goblins 1.
 */
val BothersomeNoisemaker = card("Bothersome Noisemaker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Bard"
    oracleText = "Whenever you cast a noncreature spell, amass Goblins 1. (Put a +1/+1 counter on " +
        "an Army you control. It's also a Goblin. If you don't control an Army, create a 0/0 black " +
        "Goblin Army creature token first.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.Amass(1, "Goblin")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Andreia Ugrai"
        flavorText = "The Goblins began to sing, or croak, keeping time with the flap of their flat feet on the stone."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb25b11a-6bf5-4a9a-b60f-d4dcac3816d6.jpg?1784894881"
    }
}
