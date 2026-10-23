package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Illustrious Historian — Strixhaven: School of Mages #109 (canonical printing)
 * {1}{R} · Creature — Human Shaman · 2/1
 *
 * {5}, Exile this card from your graveyard: Create a tapped 3/2 red and white Spirit creature
 * token.
 *
 * A graveyard-activated ability (`activateFromZone = GRAVEYARD`) paying {5} plus
 * [Costs.ExileSelf], the same shape as scavenge but at instant speed. The token is
 * [Effects.CreateToken] with `tapped = true`; its art resolves through STX's synced token
 * printings, as for Spirit Summoning.
 */
val IllustriousHistorian = card("Illustrious Historian") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 1
    oracleText = "{5}, Exile this card from your graveyard: Create a tapped 3/2 red and white " +
        "Spirit creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Spirit"),
            tapped = true
        )
        description = "{5}, Exile this card from your graveyard: Create a tapped 3/2 red and " +
            "white Spirit creature token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Brian Valeza"
        flavorText = "The most distinguished Lorehold professors are eligible to receive extended " +
            "tenure, even beyond the limits of their own mortality."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56cc4e7d-3b06-4938-9023-d903912350c0.jpg?1783927351"
    }
}
