package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mangara, the Diplomat — Core Set 2021 #27
 * {3}{W} · Legendary Creature — Human Cleric · 2/4
 *
 * The attack trigger's `minAttackers = 2` over "you and/or planeswalkers you control" is the
 * trigger-time check. The resolution re-check is deliberately not modelled as a battlefield
 * recount: per the 2020-06-23 ruling, attackers that left the battlefield still count by
 * last-known information, and a recount would miss them. The approximation: an attacker
 * removed from combat (without leaving) before resolution is still counted.
 */
val MangaraTheDiplomat = card("Mangara, the Diplomat") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Cleric"
    oracleText = "Lifelink\n" +
        "Whenever an opponent attacks with creatures, if two or more of those creatures are " +
        "attacking you and/or planeswalkers you control, draw a card.\n" +
        "Whenever an opponent casts their second spell each turn, draw a card."
    power = 2
    toughness = 4

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.isAttacked(2, includePlaneswalkers = true)
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent attacks with creatures, if two or more of those " +
            "creatures are attacking you and/or planeswalkers you control, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.castsNth(2)
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent casts their second spell each turn, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b4e628f-5fc5-4c17-a07d-448d361d7e7c.jpg?1783930737"
        ruling("2020-06-23", "If your opponent attacks you with one creature and your planeswalker with another, you draw a card.")
        ruling("2020-06-23", "If some of the attacking creatures leave the battlefield while Mangara's triggered ability is on the stack, use the player or planeswalker they were attacking before they left to determine whether you draw a card. On the other hand, if they're removed from combat without leaving the battlefield, use their current information to determine that they're not attacking you or a planeswalker you control anymore.")
        ruling("2020-06-23", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-06-23", "You draw just one card, no matter how many creatures are attacking you and your planeswalkers beyond the second.")
    }
}
