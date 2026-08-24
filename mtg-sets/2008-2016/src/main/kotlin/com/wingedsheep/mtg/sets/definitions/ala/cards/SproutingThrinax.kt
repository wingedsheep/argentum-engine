package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sprouting Thrinax
 * {B}{R}{G}
 * Creature — Lizard
 * 3/3
 * When this creature dies, create three 1/1 green Saproling creature tokens.
 *
 * A plain [Triggers.Dies] (battlefield → graveyard, SELF binding) over a single
 * [Effects.CreateToken] with `count = 3`; the three tokens are identical, so one effect with a
 * count is the whole ability — no composition needed.
 */
val SproutingThrinax = card("Sprouting Thrinax") {
    manaCost = "{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Lizard"
    power = 3
    toughness = 3
    oracleText = "When this creature dies, create three 1/1 green Saproling creature tokens."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            count = 3
        )
        description = "When this creature dies, create three 1/1 green Saproling creature tokens."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Jarreau Wimberly"
        flavorText = "The vast network of predation on Jund has actually caused some strange creatures to adapt to being eaten."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8950df86-1e27-4b6b-8b5c-25298a3bda85.jpg"
    }
}
