package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Spikewheel Acrobat
 * {3}{R}
 * Creature — Human Rogue
 * 5/2
 * Spectacle {2}{R} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 */
val SpikewheelAcrobat = card("Spikewheel Acrobat") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    oracleText = "Spectacle {2}{R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)"
    power = 5
    toughness = 2

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{2}{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58f70b8a-62ec-4ae6-97d2-3c6ae86a3602.jpg?1783933675"
    }
}
