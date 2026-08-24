package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stolen Grain
 * {4}{B}{B}
 * Sorcery
 * Stolen Grain deals 5 damage to target opponent or planeswalker. You gain 5 life.
 */
val StolenGrain = card("Stolen Grain") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Stolen Grain deals 5 damage to target opponent or planeswalker. You gain 5 life."

    spell {
        val t = target("target", Targets.OpponentOrPlaneswalker)
        effect = Effects.Composite(
            Effects.DealDamage(5, t),
            Effects.GainLife(5)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "LHQ"
        flavorText = "At the battle of Guandu, Cao Cao defeated Yuan Shao by raiding his grain depot, leaving him with no way to feed his troops."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1f08862-5107-46a0-8c14-a961a5c4b135.jpg"
    }
}
