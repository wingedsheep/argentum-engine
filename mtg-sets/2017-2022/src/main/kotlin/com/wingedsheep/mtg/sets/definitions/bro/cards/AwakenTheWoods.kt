package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.DynamicAmounts

val AwakenTheWoods = card("Awaken the Woods") {
    manaCost = "{X}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Create X 1/1 green Forest Dryad land creature tokens. (They're affected by summoning sickness.)"

    spell {
        effect = Effects.CreateForestDryad(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "170"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c95f8b8-faba-4412-8d8f-093e2ec903f0.jpg?1783920050"
    }
}
