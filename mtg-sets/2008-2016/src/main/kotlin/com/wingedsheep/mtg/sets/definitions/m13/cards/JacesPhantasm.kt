package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace's Phantasm
 * {U}
 * Creature — Illusion
 * 1/1
 * Flying
 * This creature gets +4/+4 as long as an opponent has ten or more cards in their graveyard.
 *
 * "An opponent has ten or more" is the greatest graveyard among opponents (one opponent must reach
 * ten on their own, not a sum), as Soaring Thought-Thief / Relic Golem test it.
 */
val JacesPhantasm = card("Jace's Phantasm") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    oracleText = "Flying\n" +
        "This creature gets +4/+4 as long as an opponent has ten or more cards in their graveyard."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(4, 4, Filters.Self),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                    Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                10
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Johann Bodin"
        flavorText = "In the abstract memories of the Iquati, Jace found interesting ideas to improve upon."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16829504-385c-4154-8e6d-f3fbaf273890.jpg?1783940507"
        ruling("2017-11-17", "The last ability of Jace's Phantasm constantly monitors each opponent's graveyard to see if the bonus applies. If the stated condition becomes no longer true, the bonus immediately stops applying.")
        ruling("2017-11-17", "If Jace's Phantasm is dealt lethal damage at the same time as an opponent's creature, and that creature will become that opponent's tenth card in their graveyard, Jace's Phantasm will be destroyed before it gets +4/+4. This is because all creatures that have been dealt lethal damage are destroyed at the same time.")
        ruling("2017-11-17", "In a multiplayer game, a single opponent must have ten or more cards in their graveyard for the bonus to apply, although this opponent may change over the course of the game. You don't have to pick a single opponent; the ability will monitor each opponent's graveyard.")
    }
}
