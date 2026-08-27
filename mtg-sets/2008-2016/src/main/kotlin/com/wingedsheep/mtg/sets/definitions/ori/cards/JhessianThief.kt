package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jhessian Thief
 * {2}{U}
 * Creature — Human Rogue
 * 1/3
 * Prowess
 * Whenever this creature deals combat damage to a player, draw a card.
 */
val JhessianThief = card("Jhessian Thief") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nWhenever this creature deals combat damage to a player, draw a card."

    keywords(Keyword.PROWESS)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Miles Johnston"
        flavorText = "\"Where's the fun in an escape if it's not at least a little daring?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33b8553d-d326-4280-bc3a-2fffdd377cd2.jpg?1783938350"

        ruling("2015-06-22", "Any spell you cast that doesn't have the type creature will cause prowess to trigger. If a spell has multiple types, and one of those types is creature (such as an artifact creature), casting it won't cause prowess to trigger. Playing a land also won't cause prowess to trigger.")
        ruling("2015-06-22", "Prowess goes on the stack on top of the spell that caused it to trigger. It will resolve before that spell.")
        ruling("2015-06-22", "Once it triggers, prowess isn't connected to the spell that caused it to trigger. If that spell is countered, prowess will still resolve.")
    }
}
