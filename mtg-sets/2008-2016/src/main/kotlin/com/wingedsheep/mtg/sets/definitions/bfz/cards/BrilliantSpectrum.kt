package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brilliant Spectrum
 * {3}{U}
 * Sorcery
 * Converge — Draw X cards, where X is the number of colors of mana spent to cast this spell.
 * Then discard two cards.
 *
 * Converge: X = [DynamicAmounts.colorsOfManaSpent], read while the spell resolves (the Infuse with
 * the Elements / Unified Front shape). The discard of two is unconditional, so a copy (X = 0) still
 * makes its controller discard two.
 */
val BrilliantSpectrum = card("Brilliant Spectrum") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Converge — Draw X cards, where X is the number of colors of mana spent to cast this " +
        "spell. Then discard two cards."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.colorsOfManaSpent()) then Effects.Discard(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Winona Nelson"
        flavorText = "There are more sights on Zendikar than the eyes can see."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fc4013b-fb4f-4bbe-a15c-488f89545695.jpg?1783938210"
    }
}
