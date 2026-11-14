package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghirapur Gearcrafter
 * {2}{R}
 * Creature — Human Artificer
 * 2/1
 * When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 */
val GhirapurGearcrafter = card("Ghirapur Gearcrafter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying. " +
        "(A creature with flying can't be blocked except by creatures with flying or reach.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Victor Adame Minguez"
        flavorText = "The metal must be pure. The hand must be steady. The product must be perfect."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f05cafb6-8812-42bb-ad14-3dde4bcb7034.jpg?1783938329"
    }
}
