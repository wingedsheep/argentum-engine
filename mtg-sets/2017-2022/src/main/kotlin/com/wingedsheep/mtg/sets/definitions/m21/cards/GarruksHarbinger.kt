package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Garruk's Harbinger — Core Set 2021 #185 (canonical printing)
 * {1}{G}{G} · Creature — Beast · 4/3
 *
 * Hexproof from black
 * Whenever this creature deals combat damage to a player or planeswalker, look at that many cards
 * from the top of your library. You may reveal a creature card or Garruk planeswalker card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random
 * order.
 *
 * "That many" is the full combat damage dealt (`triggerDamageAmount`), even past the player's life
 * or the planeswalker's loyalty (ruling) — the Doomskar Warrior dig pipeline.
 */
val GarruksHarbinger = card("Garruk's Harbinger") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 3
    oracleText = "Hexproof from black\n" +
        "Whenever this creature deals combat damage to a player or planeswalker, look at that many cards from the top " +
        "of your library. You may reveal a creature card or Garruk planeswalker card from among them and put it into " +
        "your hand. Put the rest on the bottom of your library in a random order."

    keywordAbility(KeywordAbility.hexproofFrom(Color.BLACK))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmounts.triggerDamageAmount(),
            filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker.withSubtype("Garruk"),
            prompt = "You may reveal a creature card or Garruk planeswalker card to put into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e0fa0b6-5f3f-4669-84e8-2c38c9593d88.jpg?1783930675"
        ruling("2020-06-23", "Hexproof from black means that Garruk's Harbinger can't be the target of black spells your opponents control or abilities your opponents control of black sources.")
        ruling("2020-06-23", "If Garruk's Harbinger deals damage to a planeswalker greater than their loyalty or to a player greater than their life total, you look at a number of cards equal to that full amount of damage dealt, not just that loyalty or life total.")
    }
}
