package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Grand Ball Guest
 * {1}{R}
 * Creature — Human Peasant
 * 2/2
 *
 * Celebration — This creature gets +1/+1 and has trample as long as two or more nonland permanents
 * entered the battlefield under your control this turn.
 *
 * The static half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning), the
 * same two-layer shape as [TuinvaleGuide]: one printed ability that touches layer 7c (the +1/+1)
 * *and* layer 6 (trample), so it's a single [CompositeStaticAbility] rather than two independent
 * statics. Removing the ability removes both halves together, and the shared
 * [Conditions.Celebration] gate is re-evaluated every projection — the pump and the trample appear
 * and vanish in lockstep with the turn's second nonland permanent.
 *
 * Per the WOE rulings, Celebration is a pure past-events check: the two permanents needn't still be
 * on the battlefield or still be yours, and a third one adds nothing.
 */
val GrandBallGuest = card("Grand Ball Guest") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 2
    oracleText = "Celebration — This creature gets +1/+1 and has trample as long as two or more " +
        "nonland permanents entered the battlefield under your control this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = Filters.Self),
                    GrantKeyword(Keyword.TRAMPLE, Filters.Self),
                )
            ),
            condition = Conditions.Celebration,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Leanna Crossan"
        flavorText = "It takes a lot to pull a master blacksmith from their work, but Maurice " +
            "couldn't pass up an invitation to a lavish Delverhaugh feast."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6e75228-16af-42b0-8441-ed253a660cc9.jpg?1783915093"

        ruling(
            "2023-09-01",
            "Celebration abilities only care if two or more nonland permanents entered the " +
                "battlefield under your control in a turn. They won't get more powerful if more " +
                "than two permanents entered the battlefield under your control in a turn."
        )
        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield or " +
                "under your control. Celebration abilities are checking for past events, not the " +
                "current game state."
        )
    }
}
