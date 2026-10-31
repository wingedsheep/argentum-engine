package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shepherd of the Flock // Usher to Safety
 * {1}{W}
 * Creature — Human Peasant
 * 3/1
 *
 * Adventure: Usher to Safety — {W}, Instant — Adventure
 * Return target permanent you control to its owner's hand.
 */
val ShepherdOfTheFlock = card("Shepherd of the Flock") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    power = 3
    toughness = 1
    oracleText = ""

    adventure("Usher to Safety") {
        manaCost = "{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Return target permanent you control to its owner's hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val permanent = target(TargetFilter.PermanentYouControl)
            effect = Effects.ReturnToHand(permanent)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Drew Baker"
        flavorText = "Not all heroes carry swords."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0b4f0ce-0d18-4546-803d-94a2f4f30951.jpg?1783932668"
    }
}
