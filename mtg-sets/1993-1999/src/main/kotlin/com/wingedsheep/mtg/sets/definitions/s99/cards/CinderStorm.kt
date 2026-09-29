package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

// Oracle's "any target" includes planeswalkers and battles as well as creatures and players.
val CinderStorm = card("Cinder Storm") {
    manaCost = "{6}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Cinder Storm deals 7 damage to any target."

    spell {
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(7, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Mark Tedin"
        flavorText = "When the sky's rain has turned to fire, what will put it out?"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e2d16c1-6226-438f-be1e-eaab3df687e1.jpg?1783946031"
    }
}
