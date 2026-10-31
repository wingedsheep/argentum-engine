package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Garenbrig Carver // Shield's Might
 * {3}{G}
 * Creature — Human Warrior
 * 3/2
 *
 * Adventure: Shield's Might — {1}{G}, Instant — Adventure
 * Target creature gets +2/+2 until end of turn.
 */
val GarenbrigCarver = card("Garenbrig Carver") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = ""

    adventure("Shield's Might") {
        manaCost = "{1}{G}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets +2/+2 until end of turn. (Then exile this card. You may cast " +
            "the creature later from exile.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, creature)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Lucas Graciano"
        flavorText = "Countless knights of Garenbrig owe their lives to his peerless craftsmanship."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/194b7a1c-291a-470e-9a40-61b72a46793b.jpg?1783932612"
    }
}
