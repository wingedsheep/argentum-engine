package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanguimancy
 * {4}{B}
 * Sorcery
 *
 * You draw X cards and you lose X life, where X is your devotion to black.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing. Devotion counts only
 * permanents, so the cards drawn can't change X between the draw and the life loss.
 */
val Sanguimancy = card("Sanguimancy") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw X cards and you lose X life, where X is your devotion to black. (Each {B} in " +
        "the mana costs of permanents you control counts toward your devotion to black.)"

    spell {
        val devotion = DynamicAmounts.devotionTo(Color.BLACK)
        effect = Effects.DrawCards(devotion) then Effects.LoseLife(devotion, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb129def-4055-4b12-9944-39406b451553.jpg?1783939551"
    }
}
