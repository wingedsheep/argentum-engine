package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx's Insight
 * {2}{W}{U}
 * Instant
 * Draw two cards.
 * Addendum — If you cast this spell during your main phase, you gain 2 life.
 */
val SphinxsInsight = card("Sphinx's Insight") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Draw two cards.\n" +
        "Addendum — If you cast this spell during your main phase, you gain 2 life."

    spell {
        effect = Effects.DrawCards(2) then Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
            then = Effects.GainLife(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Scott Murphy"
        flavorText = "\"Do not think me blind to your true mission...or your true master, Grand Arbiter.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d71c2435-0312-4279-a9e1-fab7432756b7.jpg?1783933634"
    }
}
