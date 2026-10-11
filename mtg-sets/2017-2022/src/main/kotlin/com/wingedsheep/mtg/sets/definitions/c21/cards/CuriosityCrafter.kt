package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Curiosity Crafter
 * {3}{U}
 * Creature — Bird Wizard
 * 3/3
 * Flying
 * You have no maximum hand size.
 * Whenever a creature token you control deals combat damage to a player, draw a card.
 */
val CuriosityCrafter = card("Curiosity Crafter") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    oracleText = "Flying\nYou have no maximum hand size.\nWhenever a creature token you control deals combat damage to a player, draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    staticAbility {
        ability = NoMaximumHandSize
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().token()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0452c73b-4522-4808-8f97-0e84e2944ee9.jpg?1783927604"
    }
}
