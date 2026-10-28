package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nimana Skitter-Sneak — Zendikar Rising #116
 * {3}{B} · Creature — Human Rogue · 3/4
 *
 * As long as an opponent has eight or more cards in their graveyard, this creature gets +1/+0 and
 * has menace.
 *
 * One printed ability spanning two layers (P/T and keywords), bundled in a [CompositeStaticAbility]
 * under one [ConditionalStaticAbility]. "An opponent has eight or more" is Expedition Lookout's
 * per-opponent test: the greatest graveyard among opponents, not a sum across them.
 */
val NimanaSkitterSneak = card("Nimana Skitter-Sneak") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    oracleText = "As long as an opponent has eight or more cards in their graveyard, this creature gets " +
        "+1/+0 and has menace. (It can't be blocked except by two or more creatures.)"
    power = 3
    toughness = 4

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.source()),
                    GrantKeyword(Keyword.MENACE, GroupFilter.source())
                )
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                    Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                8
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Campbell White"
        flavorText = "\"When navigating dangerous terrain, one hundred legs are better than two.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea080141-6262-4595-8058-b2969b3f016b.jpg?1783929371"
        ruling(
            "2020-09-25",
            "Once a single creature has blocked Nimana Skitter-Sneak, causing it to gain menace won't " +
                "cause it to become unblocked or remove that blocking creature from combat."
        )
        ruling(
            "2020-09-25",
            "An ability that offers a bonus if an opponent has eight or more cards in their graveyard " +
                "won't provide additional benefits if more than one opponent has eight or more cards in " +
                "their graveyard. There are also no additional benefits no matter how many cards are in " +
                "an opponent's graveyard, as long as there are at least eight."
        )
    }
}
