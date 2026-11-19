package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessAllDefendersBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tromokratis
 * {5}{U}{U}
 * Legendary Creature — Kraken
 * 8/8
 *
 * Tromokratis has hexproof unless it's attacking or blocking.
 * Tromokratis can't be blocked unless all creatures defending player controls block it. (If any
 * creature that player controls doesn't block this creature, it can't be blocked.)
 */
val Tromokratis = card("Tromokratis") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Kraken"
    power = 8
    toughness = 8
    oracleText = "Tromokratis has hexproof unless it's attacking or blocking.\n" +
        "Tromokratis can't be blocked unless all creatures defending player controls block it. " +
        "(If any creature that player controls doesn't block this creature, it can't be blocked.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.Not(Conditions.Any(Conditions.SourceIsAttacking, Conditions.SourceIsBlocking))
        )
    }

    staticAbility {
        ability = CantBeBlockedUnlessAllDefendersBlock()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69fcad7f-2740-49ff-a448-4b232c456e16.jpg?1783939564"
    }
}
