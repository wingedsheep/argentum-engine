package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * H.M.S. Bounty, Unlikely Timeship
 * {4}{U}
 * Legendary Artifact — Spacecraft
 * 3/4
 *
 * When H.M.S. Bounty enters, you may search your library for a Whale card, reveal it, put it into
 * your hand, then shuffle.
 * Station
 * 8+ | Flying
 * Whenever H.M.S. Bounty deals combat damage to a player, draw a card. If you control a Whale and
 * haven't begun an extra turn this game, take an extra turn after this one.
 *
 * - "Haven't begun an extra turn this game" counts extra turns that actually started, not ones
 *   merely created: `ExtraTurnsBegunThisGame` is stamped as the extra turn begins.
 * - Both halves of the condition are checked as the trigger resolves; the card is drawn either way.
 */
val HMSBountyUnlikelyTimeship = card("H.M.S. Bounty, Unlikely Timeship") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 3
    toughness = 4
    oracleText = "When H.M.S. Bounty enters, you may search your library for a Whale card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n" +
        "8+ | Flying\n" +
        "Whenever H.M.S. Bounty deals combat damage to a player, draw a card. If you control a Whale " +
        "and haven't begun an extra turn this game, take an extra turn after this one."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Whale"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    station()

    val charge8 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 8)

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1) then Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Whale")),
                Conditions.CompareAmounts(DynamicAmounts.extraTurnsBegunThisGame(), ComparisonOperator.EQ, 0),
            ),
            then = Effects.TakeExtraTurn(),
        )
        description = "Whenever H.M.S. Bounty deals combat damage to a player, draw a card. If you " +
            "control a Whale and haven't begun an extra turn this game, take an extra turn after this one."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Jetfreak-7"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d75f0991-39a3-474e-8c01-9b3363c41a9f.jpg?1793013750"
    }
}
