package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Irascible Wolverine
 * {2}{R}
 * Creature — Wolverine
 * 3/2
 * When this creature enters, exile the top card of your library. Until end of turn, you may play that card.
 * Plot {2}{R}
 */
val IrascibleWolverine = card("Irascible Wolverine") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolverine"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, exile the top card of your library. Until end of turn, you may play that card.\n" +
        "Plot {2}{R} (You may pay {2}{R} and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{2}{R}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/324c0af5-7cdf-4c71-84cc-6349f10e0d66.jpg"

        ruling("2024-04-12", "Plot abilities are written \"Plot [cost],\" which means \"Any time you have priority during your main phase while the stack is empty, you may pay [cost] and exile this card from your hand. It becomes plotted.\"")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted. On any future turn, you may cast that card from exile without paying its mana cost during your main phase while the stack is empty.")
    }
}
