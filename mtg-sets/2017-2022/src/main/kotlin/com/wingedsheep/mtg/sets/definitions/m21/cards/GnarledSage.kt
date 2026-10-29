package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gnarled Sage
 * {3}{G}{G}
 * Creature — Treefolk Druid
 * 4/4
 * Reach
 * As long as you've drawn two or more cards this turn, this creature gets +0/+2 and has vigilance.
 *
 * Two source-scoped statics gated on [Conditions.YouDrewCardsThisTurn] (threshold 2), the
 * Foggy Swamp Hunters / Messenger Hawk shape.
 */
val GnarledSage = card("Gnarled Sage") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk Druid"
    power = 4
    toughness = 4
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "As long as you've drawn two or more cards this turn, this creature gets +0/+2 and has " +
        "vigilance. (Attacking doesn't cause it to tap.)"

    keywords(Keyword.REACH)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 0, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f22116a-8b6a-4bbe-999f-7329e1e2b2d9.jpg?1783930675"
        ruling("2020-06-23", "If a spell or ability causes you to put a card into your hand without specifically using the word \"draw,\" it's not a card drawn.")
        ruling("2020-06-23", "Gnarled Sage gets +0/+2 just once, no matter how many cards you've drawn past the second.")
        ruling("2020-06-23", "If Gnarled Sage gains vigilance after it has attacked, it won't become untapped.")
    }
}
