package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sunblade Samurai — Kamigawa: Neon Dynasty #39 (canonical printing)
 * {4}{W} · Enchantment Creature — Human Samurai · 4/4
 *
 * Vigilance
 * Channel — {2}, Discard this card: Search your library for a basic Plains card, reveal it, put it
 * into your hand, then shuffle. You gain 2 life.
 *
 * The channel half is the NEO channel shape (activated from hand, discard as cost) over the
 * Clay-Fired Bricks search-then-gain-life composition.
 */
val SunbladeSamurai = card("Sunblade Samurai") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Samurai"
    power = 4
    toughness = 4
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "Channel — {2}, Discard this card: Search your library for a basic Plains card, reveal it, " +
        "put it into your hand, then shuffle. You gain 2 life."

    keywords(Keyword.VIGILANCE)

    // Channel — {2}, Discard this card (from hand): fetch a basic Plains, then gain 2 life.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        ) then Effects.GainLife(2)
        description = "Channel — {2}, Discard this card: Search your library for a basic Plains card, " +
            "reveal it, put it into your hand, then shuffle. You gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6c577bb-5e21-4a3e-b0a7-713a4ff7dce6.jpg?1783923911"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
