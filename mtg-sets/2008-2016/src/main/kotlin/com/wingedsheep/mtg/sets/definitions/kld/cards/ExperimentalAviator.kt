package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Experimental Aviator
 * {3}{U}{U}
 * Creature — Human Artificer
 * 0/3
 * Flying
 * When this creature enters, create two 1/1 colorless Thopter artifact creature tokens with flying.
 */
val ExperimentalAviator = card("Experimental Aviator") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    oracleText = "Flying\n" +
        "When this creature enters, create two 1/1 colorless Thopter artifact creature tokens with flying."
    power = 0
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Jesper Ejsing"
        flavorText = "\"The little thopters seemed lonely up here, so I thought I'd join them.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/962dc573-612d-434b-82fb-af9c3e3c9aca.jpg?1783937221"
    }
}
