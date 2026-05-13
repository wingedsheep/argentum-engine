package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val LotusCobra = card("Lotus Cobra") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, add one mana of any color."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.AddAnyColorMana(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Sam Rowan"
        flavorText = "Its hood blossoms as a warning: receive your gift, but stray no closer."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e9cc3c4-82e2-43ba-892d-91836e6ac6fc.jpg?1721429332"
    }
}
