package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Evangel of Synthesis
 * {U}{B}
 * Creature — Phyrexian Human Cleric
 * 2/3
 * When this creature enters, draw a card, then discard a card.
 * As long as you've drawn two or more cards this turn, this creature gets +1/+0 and has menace.
 *
 * Trench Stalker's shape: source-scoped statics gated on [Conditions.YouDrewCardsThisTurn] (2).
 */
val EvangelOfSynthesis = card("Evangel of Synthesis") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Phyrexian Human Cleric"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, draw a card, then discard a card.\n" +
        "As long as you've drawn two or more cards this turn, this creature gets +1/+0 and has menace."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.MENACE, GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Denman Rooke"
        flavorText = "\"Forge the future in metal and oil!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8b60003-a987-49b0-a0f8-bb825c97da4d.jpg?1783920031"
    }
}
