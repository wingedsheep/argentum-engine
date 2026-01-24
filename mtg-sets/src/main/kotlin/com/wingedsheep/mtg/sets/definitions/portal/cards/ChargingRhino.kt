package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan

/**
 * Charging Rhino
 * {3}{G}{G}
 * Creature — Rhino
 * 4/4
 * Charging Rhino can't be blocked by more than one creature.
 */
val ChargingRhino = card("Charging Rhino") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Rhino"
    power = 4
    toughness = 4

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Una Fricker"
        flavorText = "Physical and mental might are like thunder and lightning. One comes from the other."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5461a48-d1a8-4116-8a9c-de5223cb5e02.jpg"
    }
}
