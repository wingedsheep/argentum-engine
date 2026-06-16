package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Loan Shark
 * {3}{U}
 * Creature — Shark Rogue
 * 3/4
 * When this creature enters, if you've cast two or more spells this turn, draw a card.
 * Plot {3}{U}
 *
 * Intervening-if (CR 603.4): the enters trigger only fires — and only resolves — if you've cast
 * two or more spells this turn. The cast of Loan Shark itself counts as one of those spells, so a
 * second spell earlier in the turn satisfies it. When Loan Shark is cast from exile as a plotted
 * card it isn't free of that requirement: it still counts as a spell cast this turn.
 */
val LoanShark = card("Loan Shark") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shark Rogue"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, if you've cast two or more spells this turn, draw a card.\n" +
        "Plot {3}{U} (You may pay {3}{U} and exile this card from your hand. Cast it as a sorcery on a " +
        "later turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{3}{U}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.YouCastSpellsThisTurn(atLeast = 2)
        effect = Effects.DrawCards(1)
        description = "When this creature enters, if you've cast two or more spells this turn, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49f12760-ff07-4c9f-a7a9-1e64bd3a9adf.jpg?1712355452"

        ruling("2024-04-12", "The cast of Loan Shark counts toward the number of spells you've cast this turn. You'll need to have cast at least one other spell this turn for its ability to draw a card.")
    }
}
