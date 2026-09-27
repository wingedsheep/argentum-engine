package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Zurgo and Ojutai
 * {2}{U}{R}{W}
 * Legendary Creature — Orc Dragon
 * 4/4
 *
 * Flying, haste
 * Zurgo and Ojutai has hexproof as long as it entered this turn.
 * Whenever one or more Dragons you control deal combat damage to a player or battle, look at the
 * top three cards of your library. Put one of them into your hand and the rest on the bottom of
 * your library in any order. You may return one of those Dragons to its owner's hand.
 */
val ZurgoAndOjutai = card("Zurgo and Ojutai") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Orc Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\n" +
        "Zurgo and Ojutai has hexproof as long as it entered this turn.\n" +
        "Whenever one or more Dragons you control deal combat damage to a player or battle, look at " +
        "the top three cards of your library. Put one of them into your hand and the rest on the " +
        "bottom of your library in any order. You may return one of those Dragons to its owner's hand."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.SourceEnteredThisTurn
        )
    }

    // Once per player or battle hit; "those Dragons" are the ones that hit it (the captured batch).
    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype("Dragon"))
            .dealCombatDamageToAPlayerOrBattle()
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        ) then Effects.Pipeline {
            val dragons = filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.BATTLEFIELD))
            val chosen = chooseUpTo(1, from = dragons, prompt = "You may return one of those Dragons to its owner's hand")
            move(chosen, CardDestination.ToZone(Zone.HAND))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "258"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eacbcd82-36e6-424c-bd4e-ec3a584836c5.jpg?1783916936"

        ruling(
            "2023-04-14",
            "The triggered ability will trigger once for each player or battle dealt combat damage by " +
                "Dragons you control, no matter how many Dragons were involved. For example, if you attack " +
                "a player with three Dragons and a battle that player protects with two Dragons, and they " +
                "all connect, the ability will trigger twice."
        )
        ruling(
            "2023-04-14",
            "Because creatures with first strike deal combat damage before creatures without first " +
                "strike, it's also possible to have the ability trigger twice in the same combat by your " +
                "Dragons dealing combat damage to one player at different times. This can also happen if " +
                "an attacking Dragon you control has double strike."
        )
        ruling(
            "2023-04-14",
            "You choose whether to return one of the Dragons that dealt combat damage to the player or " +
                "battle as the triggered ability is resolving. This doesn't target any of those Dragons."
        )
    }
}
