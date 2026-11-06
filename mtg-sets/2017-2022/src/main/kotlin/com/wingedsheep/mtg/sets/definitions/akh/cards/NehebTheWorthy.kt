package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Neheb, the Worthy
 * {1}{B}{R}
 * Legendary Creature — Minotaur Warrior
 * 2/2
 * First strike
 * Other Minotaurs you control have first strike.
 * As long as you have one or fewer cards in hand, Minotaurs you control get +2/+0.
 * Whenever Neheb deals combat damage to a player, each player discards a card.
 *
 * The first-strike grant excludes Neheb (it has the keyword itself); the hand-size anthem does not,
 * so Neheb pumps itself too (2017-04-18 ruling). The anthem is a live static condition, re-read
 * whenever the hand changes, not a check made once when Neheb enters.
 */
val NehebTheWorthy = card("Neheb, the Worthy") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Minotaur Warrior"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Other Minotaurs you control have first strike.\n" +
        "As long as you have one or fewer cards in hand, Minotaurs you control get +2/+0.\n" +
        "Whenever Neheb deals combat damage to a player, each player discards a card."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.MINOTAUR).youControl(),
                excludeSelf = true,
            ),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.MINOTAUR).youControl()),
            ),
            condition = Conditions.CardsInHandAtMost(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.EachPlayerDiscards(1)
        description = "Whenever Neheb deals combat damage to a player, each player discards a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7593d5fb-c6b3-4d24-b9d3-97a4378161fd.jpg?1783936461"
        ruling(
            "2017-04-18",
            "Neheb is affected by its own power-modifying ability once you have one or zero cards in hand."
        )
        ruling(
            "2017-04-18",
            "When Neheb's triggered ability resolves, first the player whose turn it is chooses a card to " +
                "discard, then each other player in turn order chooses a card to discard, then all of those " +
                "cards are discarded simultaneously. No one sees what the other players are discarding before " +
                "deciding which card to discard."
        )
    }
}
