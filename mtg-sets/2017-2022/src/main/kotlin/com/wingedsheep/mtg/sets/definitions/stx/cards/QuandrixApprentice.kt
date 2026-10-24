package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Quandrix Apprentice — Strixhaven: School of Mages #216 (canonical printing)
 * {G}{U} · Creature — Human Wizard · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, look at the top three cards
 * of your library. You may reveal a land card from among them and put that card into your hand.
 * Put the rest on the bottom of your library in any order.
 */
val QuandrixApprentice = card("Quandrix Apprentice") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, look at the top " +
        "three cards of your library. You may reveal a land card from among them and put that card " +
        "into your hand. Put the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Land,
            prompt = "You may reveal a land card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, look at " +
            "the top three cards of your library. You may reveal a land card from among them and put " +
            "that card into your hand. Put the rest on the bottom of your library in any order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Ryan Alexander Lee"
        flavorText = "\"The secrets of our world will be mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0291c909-bb87-47cb-9b06-5e409e654f15.jpg?1783927303"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
