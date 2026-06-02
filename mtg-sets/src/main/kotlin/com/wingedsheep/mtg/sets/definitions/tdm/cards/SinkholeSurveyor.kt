package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sinkhole Surveyor — Tarkir: Dragonstorm #93
 * {1}{B} · Creature — Bird Scout · 1/3
 *
 * Flying
 * Whenever this creature attacks, you lose 1 life and this creature endures 1.
 * (Put a +1/+1 counter on it or create a 1/1 white Spirit creature token.)
 */
val SinkholeSurveyor = card("Sinkhole Surveyor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird Scout"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, you lose 1 life and this creature endures 1. " +
        "(Put a +1/+1 counter on it or create a 1/1 white Spirit creature token.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                Effects.LoseLife(1, EffectTarget.Controller),
                Effects.Endure(1)
            )
        )
        description = "Whenever this creature attacks, you lose 1 life and this creature endures 1."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37cb5599-7d2c-48e9-978b-902a01a74bde.jpg?1743204333"
    }
}
