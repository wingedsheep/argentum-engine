package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk Secretkeeper // Venture Deeper
 * {U}
 * Creature — Merfolk Wizard
 * 0/4
 *
 * Adventure: Venture Deeper — {U}, Sorcery — Adventure
 * Target player mills four cards.
 */
val MerfolkSecretkeeper = card("Merfolk Secretkeeper") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 0
    toughness = 4
    oracleText = ""

    adventure("Venture Deeper") {
        manaCost = "{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target player mills four cards. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Library.mill(4, player)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Jana Schirmer"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ceb7308d-608c-4ede-9496-d795fc5bb271.jpg?1783932659"
    }
}
