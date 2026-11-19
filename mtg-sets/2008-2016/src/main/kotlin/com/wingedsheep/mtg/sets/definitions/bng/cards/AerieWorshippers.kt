package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aerie Worshippers
 * {3}{U}
 * Creature — Human Cleric
 * 2/4
 *
 * Inspired — Whenever this creature becomes untapped, you may pay {2}{U}. If you do, create a 2/2
 * blue Bird enchantment creature token with flying.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val AerieWorshippers = card("Aerie Worshippers") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Inspired — Whenever this creature becomes untapped, you may pay {2}{U}. If you do, " +
        "create a 2/2 blue Bird enchantment creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{U}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Bird"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/c/a/ca6ce21f-2a29-480e-91f9-b089232863ff.jpg?1783939516",
                enchantmentToken = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Mike Sass"
        flavorText = "They can conjure stars from a clear sky."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06c8b3b9-f0fb-4cc1-8916-82ee2c74f089.jpg?1783939575"
    }
}
