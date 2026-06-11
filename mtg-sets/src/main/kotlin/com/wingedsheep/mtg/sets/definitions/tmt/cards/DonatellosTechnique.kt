package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Donatello's Technique
 * {2}{U}
 * Sorcery
 *
 * Sneak {U} (You may cast this spell for {U} if you also return an unblocked
 * attacker you control to hand during the declare blockers step.)
 * Draw two cards.
 */
val DonatellosTechnique = card("Donatello's Technique") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Sneak {U} (You may cast this spell for {U} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nDraw two cards."

    sneak("{U}")

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Andreas Zafiratos"
        flavorText = "\"I'll need my bag of tricks.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e570082f-8129-44d2-b471-ec7f46a98dbd.jpg?1771586794"
    }
}
