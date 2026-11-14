package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aspiring Aeronaut
 * {3}{U}
 * Creature — Human Artificer
 * 1/2
 * Flying
 * When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 */
val AspiringAeronaut = card("Aspiring Aeronaut") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)\n" +
        "When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying."

    keywords(Keyword.FLYING)

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
        collectorNumber = "46"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ae6c87f-003b-44b7-96fd-ab8fca9af6f1.jpg?1783938354"
    }
}
