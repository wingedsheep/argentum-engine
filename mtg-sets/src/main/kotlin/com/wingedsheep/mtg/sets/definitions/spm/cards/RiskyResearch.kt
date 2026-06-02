package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

val RiskyResearch = card("Risky Research") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Surveil 2, then draw two cards, then you lose 2 life."

    spell {
        effect = LibraryPatterns.surveil(2) then Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f8aa705-6177-42e9-95cb-e7f880c186e3.jpg?1757377145"
    }
}
