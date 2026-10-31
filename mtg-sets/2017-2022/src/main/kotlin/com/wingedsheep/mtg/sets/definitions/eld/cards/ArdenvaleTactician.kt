package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ardenvale Tactician // Dizzying Swoop
 * {1}{W}{W}
 * Creature — Human Knight
 * 2/3
 *
 * Flying
 *
 * Adventure: Dizzying Swoop — {1}{W}, Instant — Adventure
 * Tap up to two target creatures.
 */
val ArdenvaleTactician = card("Ardenvale Tactician") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 3
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Dizzying Swoop") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Tap up to two target creatures. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            targets(TargetFilter.Creature, count = 2, optional = true)
            effect = Effects.TapEachTarget()
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Jason Rainville"
        flavorText = "In a castle that prizes loyalty above all, the bond between knight and griffin is unmatched."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7d5e394-8e41-442e-ae97-a478a61e1b9d.jpg?1783932677"
    }
}
