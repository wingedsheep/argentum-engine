package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Excavation Mole
 * {2}{G}
 * Creature — Mole
 * 3/3
 *
 * Trample
 * When this creature enters, mill three cards.
 */
val ExcavationMole = card("Excavation Mole") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mole"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, mill three cards."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Lars Grant-West"
        flavorText = "It makes molehills out of mountains."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a1712dc-8f4c-407f-83ea-3a1c59c437d1.jpg"
    }
}
