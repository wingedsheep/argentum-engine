package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Risky Research
 * {2}{B}
 * Sorcery
 * Surveil 2, then draw two cards, then you lose 2 life.
 */
val RiskyResearch = card("Risky Research") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Surveil 2, then draw two cards, then you lose 2 life."

    spell {
        effect = EffectPatterns.surveil(2) then Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c2bc8a4-cd50-4f3e-a09f-0a6a93736ae7.jpg?1757377863"
    }
}
