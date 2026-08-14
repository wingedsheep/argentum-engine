package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val RiskyResearch = card("Risky Research") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Surveil 2, then draw two cards. You lose 2 life. (To surveil 2, look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = Patterns.Library.surveil(2) then Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Rafater"
        flavorText = "\"Though others fear radiation, I alone am able to make it my servant!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f8aa705-6177-42e9-95cb-e7f880c186e3.jpg?1757377145"
    }
}
