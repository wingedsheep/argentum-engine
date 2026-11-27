package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Lone Revenant
 * {3}{U}{U}
 * Creature — Spirit
 * 4/4
 *
 * Hexproof
 * Whenever this creature deals combat damage to a player, if you control no other creatures, look at the
 * top four cards of your library. Put one of them into your hand and the rest on the bottom of your
 * library in any order.
 *
 * Intervening-if `YouControl(Creature, negate, excludeSelf)` — checked on trigger and again on
 * resolution, so a Revenant that has left by then still pays off (2012-05-01 ruling).
 */
val LoneRevenant = card("Lone Revenant") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "Whenever this creature deals combat damage to a player, if you control no other creatures, look at the " +
        "top four cards of your library. Put one of them into your hand and the rest on the bottom of your " +
        "library in any order."

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.YouControl(GameObjectFilter.Creature, negate = true, excludeSelf = true)
        description = "Whenever this creature deals combat damage to a player, if you control no other creatures, " +
            "look at the top four cards of your library. Put one of them into your hand and the rest on the " +
            "bottom of your library in any order."
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 4,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e12186e-9c93-4136-9ea3-e8d2ae1ee2e5.jpg?1783940715"
        ruling("2012-05-01", "You must put one of the cards into your hand, even if you'd rather put all four on the bottom of your library.")
        ruling("2012-05-01", "If you control Lone Revenant and any other creature (including another Lone Revenant), its ability won't trigger.")
        ruling("2012-05-01", "Check whether Lone Revenant's ability triggers after combat damage is dealt but before any creatures that are destroyed due to combat damage leave the battlefield. For example, if you control Lone Revenant and another creature, and that other creature takes lethal damage in the same combat damage step that Lone Revenant deals damage to a player, Lone Revenant's ability won't trigger.")
        ruling("2012-05-01", "If you control no creatures when Lone Revenant's ability resolves, you'll still look at the top four cards and put one into your hand.")
    }
}
