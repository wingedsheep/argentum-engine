package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blast of Genius
 * {4}{U}{R}
 * Sorcery
 * Choose any target. Draw three cards, then discard a card. Blast of Genius deals damage equal
 * to the discarded card's mana value to that permanent or player.
 *
 * "Choose any target" is a real target picked on cast, so an illegal target fizzles the whole
 * spell (no draw, no discard — ruling). The discard stores its card in [Patterns.Hand.discarded],
 * which the damage reads as its mana value ({X} counts as 0).
 */
val BlastOfGenius = card("Blast of Genius") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Choose any target. Draw three cards, then discard a card. Blast of Genius deals damage " +
        "equal to the discarded card's mana value to that permanent or player."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DrawCards(3) then
            Effects.Discard(1) then
            Effects.DealDamage(DynamicAmounts.manaValueOf(Patterns.Hand.discarded), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Terese Nielsen"
        flavorText = "Ral Zarek's brainstorms bring actual thunder and lightning."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2ff592c-bd35-4947-ba17-8b6170d5388e.jpg?1783940032"
        ruling(
            "2018-12-07",
            "If the target permanent or player becomes an illegal target for Blast of Genius, the spell " +
                "doesn't resolve. You don't draw or discard any cards."
        )
        ruling("2018-12-07", "If a discarded card has {X} in its mana cost, X is considered to be 0.")
    }
}
