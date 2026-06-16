package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Plan the Heist
 * {2}{U}{U}
 * Sorcery
 *
 * Surveil 3 if you have no cards in hand. Then draw three cards.
 * Plot {3}{U}
 *
 * The conditional surveil is gated on [Conditions.EmptyHand], checked at resolution before
 * the draw (CR/OTJ ruling: "You'll only surveil 3 if you have no cards in hand when Plan the
 * Heist resolves, but you'll still draw three cards either way"). Surveil precedes the draw in
 * instruction order, so the empty-hand check happens while the hand is still empty.
 */
val PlanTheHeist = card("Plan the Heist") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Surveil 3 if you have no cards in hand. Then draw three cards. " +
        "(To surveil 3, look at the top three cards of your library, then put any number of " +
        "them into your graveyard and the rest on top of your library in any order.)\n" +
        "Plot {3}{U} (You may pay {3}{U} and exile this card from your hand. Cast it as a " +
        "sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    spell {
        effect = ConditionalEffect(
            condition = Conditions.EmptyHand,
            effect = Patterns.Library.surveil(3)
        ) then Effects.DrawCards(3)
    }

    keywordAbility(KeywordAbility.plot("{3}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e8c3a0e-d61c-457f-ac85-577d0bb94b96.jpg?1712355477"

        ruling("2024-04-12", "You'll only surveil 3 if you have no cards in hand when Plan the Heist resolves, but you'll still draw three cards either way.")
        ruling("2024-04-12", "If a plotted card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
