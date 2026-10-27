package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Legion Angel — Zendikar Rising #23
 * {2}{W}{W} · Creature — Angel Warrior · 3/3
 *
 * Flying
 * When this creature enters, you may reveal a card you own named Legion Angel from outside the
 * game and put it into your hand.
 *
 * The ETB is a name-restricted wish: [Patterns.Sideboard.wish] (gather from the owner's sideboard
 * → choose up to one, the "may" → reveal → hand) over `GameObjectFilter.Any.named("Legion Angel")`.
 * The trigger has no intervening "if cast", so it fires on any entry.
 */
val LegionAngel = card("Legion Angel") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Warrior"
    oracleText = "Flying\n" +
        "When this creature enters, you may reveal a card you own named Legion Angel from outside " +
        "the game and put it into your hand."
    power = 4
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Sideboard.wish(GameObjectFilter.Any.named("Legion Angel"))
        description = "When this creature enters, you may reveal a card you own named Legion Angel " +
            "from outside the game and put it into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "PINDURSKI"
        flavorText = "\"We are many, and righteous.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bda5609a-ab22-4f03-988b-8056c97fdc9e.jpg?1783929414"
    }
}
