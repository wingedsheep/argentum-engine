package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hell Mongrel — Modern Horizons 2 #88
 * {3}{B} · Creature — Nightmare Dog · 4 / 3
 *
 * Discard a card: This creature gets +1/+1 until end of turn.
 * Madness {2}{B} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)
 *
 * The Wild Mongrel shape: a free-to-activate pump whose only cost is a card. [Costs.DiscardCard]
 * is "discard *a* card" (any card from hand) — not [Costs.DiscardSelf], which is the cycling-style
 * "discard this card". Paying it while holding a madness card is the intended synergy: the discard
 * is a cost, so it happens on activation and the madness trigger goes on the stack above the pump.
 *
 * `CardBuilder.build()` derives the printed `Keyword.MADNESS` from `madness(...)`, so the bare
 * keyword is not written alongside it.
 */
val HellMongrel = card("Hell Mongrel") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Dog"
    power = 4
    toughness = 3
    oracleText = "Discard a card: This creature gets +1/+1 until end of turn.\n" +
        "Madness {2}{B} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    madness("{2}{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Robbie Trevino"
        flavorText = "It feeds on shadows, fear, and blood."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7da32a3-8e33-4603-abd2-8db144062f6a.jpg?1783926860"
    }
}
