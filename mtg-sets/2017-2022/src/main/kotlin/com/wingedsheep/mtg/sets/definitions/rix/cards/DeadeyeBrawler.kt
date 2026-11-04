package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Deadeye Brawler — Rivals of Ixalan #155
 * {2}{U}{B} · Creature — Human Pirate · 2/4
 *
 * Deathtouch
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Whenever this creature deals combat damage to a player, if you have the city's blessing, draw a card.
 *
 * The Ascend keyword plus a combat-damage trigger gated by the intervening-if
 * [Conditions.YouHaveCitysBlessing].
 */
val DeadeyeBrawler = card("Deadeye Brawler") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 4
    oracleText = "Deathtouch\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Whenever this creature deals combat damage to a player, if you have the city's blessing, draw a card."

    keywords(Keyword.DEATHTOUCH, Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.YouHaveCitysBlessing
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acb1ab66-f2ce-4094-b2dd-1fc63d78eea2.jpg?1783935278"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
        ruling(
            "2018-01-19",
            "Some cards have triggered abilities with an intervening \"if\" clause that checks whether you have the city's blessing. " +
                "You must already have the city's blessing in order for these abilities to trigger; otherwise they do nothing."
        )
    }
}
