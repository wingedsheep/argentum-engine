package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Darkblade Agent
 * {1}{U}{B}
 * Creature — Human Assassin
 * 2/3
 * As long as you've surveilled this turn, this creature has deathtouch and "Whenever this creature
 * deals combat damage to a player, you draw a card."
 */
val DarkbladeAgent = card("Darkblade Agent") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Assassin"
    oracleText = "As long as you've surveilled this turn, this creature has deathtouch and " +
        "\"Whenever this creature deals combat damage to a player, you draw a card.\""
    power = 2
    toughness = 3

    // Two conditional statics rather than one composite: a self-granted triggered ability is read
    // off a ConditionalStaticAbility wrapping the GrantTriggeredAbility directly.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.SurveiledThisTurn,
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                    effect = Effects.DrawCards(1)
                    description = "Whenever this creature deals combat damage to a player, you draw a card."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.SurveiledThisTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Joe Slucher"
        flavorText = "\"I've seen your house, and you've been a lovely, if unwitting, host. But at last it's time for farewells.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0b610c5-61e5-43df-a87b-3eaaa915402b.jpg?1783934142"
    }
}
