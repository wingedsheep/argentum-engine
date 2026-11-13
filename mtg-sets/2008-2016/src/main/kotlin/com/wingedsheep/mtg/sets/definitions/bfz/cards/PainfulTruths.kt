package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Painful Truths
 * {2}{B}
 * Sorcery
 * Converge — You draw X cards and lose X life, where X is the number of colors of mana spent to cast
 * this spell.
 *
 * Converge: X = [DynamicAmounts.colorsOfManaSpent], read while the spell resolves (the Brilliant
 * Spectrum shape), feeding both the draw and the life loss. A copy, or a free cast, has X = 0.
 */
val PainfulTruths = card("Painful Truths") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Converge — You draw X cards and lose X life, where X is the number of colors of mana " +
        "spent to cast this spell."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.colorsOfManaSpent()) then
            Effects.LoseLife(DynamicAmounts.colorsOfManaSpent(), EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Winona Nelson"
        flavorText = "As the Eldrazi spread, no secret that could give Zendikar's defenders an advantage " +
            "was considered too dangerous to seek."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46a3afdb-aeb2-4699-91f9-5c42284b3a4a.jpg?1783938200"
    }
}
