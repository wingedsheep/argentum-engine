package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Market Gnome — The Lost Caverns of Ixalan #22
 * {W} · Artifact Creature — Gnome · 0/3 · Uncommon
 * Artist: Gaboleps
 *
 * When this creature dies, you gain 1 life and draw a card.
 * When this creature is exiled from the battlefield while you're activating a craft ability,
 * you gain 1 life and draw a card.
 *
 * Ability 1 — [Triggers.Dies] (battlefield → graveyard). [Effects.Composite] of
 *   [Effects.GainLife] (1) then [Effects.DrawCards] (1), both defaulting to the controller.
 *
 * Ability 2 — [Triggers.ExiledAsCraftMaterial]: a SELF exile trigger gated on the craft-material
 *   fact stamped by the Craft cost payment (CR 702.167), so it fires only when this creature is
 *   exiled as a material to pay a craft ability's cost — not on removal-style exile, and never
 *   alongside the dies trigger (exile is not death). Same [Effects.Composite] payoff.
 */
val MarketGnome = card("Market Gnome") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Gnome"
    oracleText = "When this creature dies, you gain 1 life and draw a card.\n" +
        "When this creature is exiled from the battlefield while you're activating a craft " +
        "ability, you gain 1 life and draw a card."

    power = 0
    toughness = 3

    // When this creature dies, you gain 1 life and draw a card.
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Composite(
            Effects.GainLife(1),
            Effects.DrawCards(1)
        )
        description = "When Market Gnome dies, you gain 1 life and draw a card."
    }

    // When this creature is exiled from the battlefield while you're activating a craft ability,
    // you gain 1 life and draw a card.
    triggeredAbility {
        trigger = Triggers.ExiledAsCraftMaterial
        effect = Effects.Composite(
            Effects.GainLife(1),
            Effects.DrawCards(1)
        )
        description = "When Market Gnome is exiled from the battlefield while you're activating a " +
            "craft ability, you gain 1 life and draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Gaboleps"
        flavorText = "\"Maize, sweet and fresh! Tamal, hot and filling! Water, still or sparkling!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36dafcd6-ce4e-4a27-bd8c-fa1a3bffc99e.jpg?1782694594"
    }
}
