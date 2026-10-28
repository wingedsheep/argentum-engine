package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gut Shot
 * {R/P}
 * Instant
 * ({R/P} can be paid with either {R} or 2 life.)
 * Gut Shot deals 1 damage to any target.
 *
 * The Phyrexian pip is parsed by `ManaCost.parse` (the Dismember shape); it always counts 1 toward
 * mana value however it's paid.
 */
val GutShot = card("Gut Shot") {
    manaCost = "{R/P}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Gut Shot deals 1 damage to any target."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Greg Staples"
        flavorText = "\"Down here, we have a more pointed version of the scriptures.\"\n—Urabrask's enforcer"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a54a2a30-b96a-49c7-9151-1f4b0d4a4413.jpg?1783941308"
        ruling(
            "2011-06-01",
            "A card with Phyrexian mana symbols in its mana cost is each color that appears in that mana cost, " +
                "regardless of how that cost may have been paid.",
        )
        ruling("2011-06-01", "To calculate the mana value of a card with Phyrexian mana symbols in its cost, count each Phyrexian mana symbol as 1.")
        ruling("2011-06-01", "If you're at 1 life or less, you can't pay 2 life.")
    }
}
