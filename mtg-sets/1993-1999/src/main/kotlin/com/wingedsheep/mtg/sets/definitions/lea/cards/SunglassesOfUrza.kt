package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SpendManaAsColor

val SunglassesOfUrza = card("Sunglasses of Urza") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "You may spend white mana as though it were red mana."
    staticAbility { ability = SpendManaAsColor(Color.WHITE, Color.RED) }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "271"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0d433a4-76c0-4f27-836d-4c0c13a511fb.jpg?1783948661"
    }
}
