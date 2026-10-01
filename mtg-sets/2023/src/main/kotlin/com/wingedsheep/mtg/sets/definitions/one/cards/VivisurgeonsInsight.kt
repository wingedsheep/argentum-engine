package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vivisurgeon's Insight
 * {3}{U}{U}
 * Sorcery
 * Draw three cards. Proliferate.
 */
val VivisurgeonsInsight = card("Vivisurgeon's Insight") {
    manaCost = "{3}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        effect = Effects.DrawCards(3) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Adam Burn"
        flavorText = "\"Experimental Log 290: Leaving the subject's pain receptors active appears to yield an " +
            "unprecedented combination of incoherence and hyperawareness.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd244359-781e-4e1d-946d-243664f40c7c.jpg?1783918053"
    }
}
