package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Moria Scavenger — Tales of Middle-earth Commander #63 (canonical printing)
 * {1}{B}{R} · Creature — Orc Rogue · 1/4
 *
 * Deathtouch, haste
 * {T}, Discard a card: Draw a card. If the discarded card was a creature card, amass Orcs 1.
 *
 * The discard is a cost, so the card is already in the graveyard when the ability resolves; the
 * activation records it and `Conditions.DiscardedCardMatches` reads its characteristics there.
 */
val MoriaScavenger = card("Moria Scavenger") {
    manaCost = "{1}{B}{R}"
    typeLine = "Creature — Orc Rogue"
    power = 1
    toughness = 4
    oracleText = "Deathtouch, haste\n" +
        "{T}, Discard a card: Draw a card. If the discarded card was a creature card, amass Orcs 1. " +
        "(Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't control an Army, " +
        "create a 0/0 black Orc Army creature token first.)"

    keywords(Keyword.DEATHTOUCH, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.DiscardedCardMatches(GameObjectFilter.Creature),
                then = Effects.Amass(1, "Orc"),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea926187-ac8e-4197-8fd4-ac6b8d818d83.jpg?1783916017"
        ruling(
            "2023-06-16",
            "If you don't control an Army, the Orc Army token you create enters the battlefield as a 0/0 " +
                "creature before receiving counters.",
        )
    }
}
