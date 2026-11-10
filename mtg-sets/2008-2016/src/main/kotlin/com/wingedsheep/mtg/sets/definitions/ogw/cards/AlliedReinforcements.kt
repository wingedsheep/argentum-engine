package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Allied Reinforcements
 * {3}{W}
 * Sorcery
 * Create two 2/2 white Knight Ally creature tokens.
 */
val AlliedReinforcements = card("Allied Reinforcements") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 2/2 white Knight Ally creature tokens."

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight", "Ally"),
            count = 2,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Matt Stewart"
        flavorText = "\"Our formation was scattered after Sea Gate fell. If you're fighting the Eldrazi, consider us your newest recruits.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd7d7c93-5622-45ca-9359-110685d9162c.jpg"
    }
}
