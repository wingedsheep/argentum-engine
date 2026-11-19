package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Satyr Nyx-Smith
 * {2}{R}
 * Creature — Satyr Shaman
 * 2/1
 *
 * Haste
 * Inspired — Whenever this creature becomes untapped, you may pay {2}{R}. If you do, create a 3/1
 * red Elemental enchantment creature token with haste.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SatyrNyxSmith = card("Satyr Nyx-Smith") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Satyr Shaman"
    power = 2
    toughness = 1
    oracleText = "Haste\nInspired — Whenever this creature becomes untapped, you may pay {2}{R}. If you " +
        "do, create a 3/1 red Elemental enchantment creature token with haste."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{R}"),
            then = Effects.CreateToken(
                power = 3,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.HASTE),
                imageUri = "https://cards.scryfall.io/normal/front/9/7/97e5fee3-6dd9-4bbe-b64d-ddfa4b9d4256.jpg?1783939516",
                enchantmentToken = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bc2c65e-3cba-4813-9949-87e15f592247.jpg?1783939541"
    }
}
