package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enlisted Wurm
 * {4}{G}{W}
 * Creature — Wurm
 * 5/5
 * Cascade
 *
 * [Keyword.CASCADE] is display-only; the cascade itself is the `Triggers.self.isCast()` trigger
 * feeding [Effects.Cascade] — the same shape as `arb/cards/BloodbraidElf.kt`.
 */
val EnlistedWurm = card("Enlisted Wurm") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Wurm"
    power = 5
    toughness = 5
    oracleText = "Cascade (When you cast this spell, exile cards from the top of your library until you " +
        "exile a nonland card that costs less. You may cast it without paying its mana cost. Put " +
        "the exiled cards on the bottom in a random order.)"

    keywords(Keyword.CASCADE)

    // Cascade — the cast trigger the keyword abbreviates.
    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Cascade
        description = "Cascade"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Steve Prescott"
        flavorText = "A match for any army—even its own."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66605149-3959-4a51-8400-350e6fac2ab2.jpg?1783942427"
    }
}
