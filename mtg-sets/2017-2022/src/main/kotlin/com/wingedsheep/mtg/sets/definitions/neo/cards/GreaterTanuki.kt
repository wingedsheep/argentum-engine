package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Greater Tanuki — Kamigawa: Neon Dynasty #189 (canonical printing)
 * {4}{G}{G} · Enchantment Creature — Dog · 6/5
 *
 * Trample
 * Channel — {2}{G}, Discard this card: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 */
val GreaterTanuki = card("Greater Tanuki") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Dog"
    power = 6
    toughness = 5
    oracleText = "Trample\n" +
        "Channel — {2}{G}, Discard this card: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle."

    keywords(Keyword.TRAMPLE)

    // Channel — {2}{G}, Discard this card (from hand): ramp a tapped basic land.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "Channel — {2}{G}, Discard this card: Search your library for a basic land card, " +
            "put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Ilse Gort"
        flavorText = "The kami permit few mortals to dwell in Jukai Forest, but the clever, playful tanuki are welcomed as friends and guardians."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4fbaee3-a10f-4b2d-b07e-d041a96a7e27.jpg?1783923849"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
