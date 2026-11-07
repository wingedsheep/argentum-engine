package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wriggling Grub — Foundations Jumpstart #12
 * {1}{B} · Creature — Worm · 1/1
 *
 * When this creature dies, create two 1/1 black and green Worm creature tokens.
 */
val WrigglingGrub = card("Wriggling Grub") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Worm"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create two 1/1 black and green Worm creature tokens."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Worm"),
            count = 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Caio Monteiro"
        flavorText = "The professor of essence studies always relished the looks on the first-years' faces " +
            "as their vivisections descended into chaos."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e02aa086-13b2-42cf-acf4-086ba406e886.jpg?1783908867"
    }
}
