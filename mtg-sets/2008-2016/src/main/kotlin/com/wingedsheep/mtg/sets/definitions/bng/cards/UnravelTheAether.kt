package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unravel the Aether
 * {1}{G}
 * Instant
 *
 * Choose target artifact or enchantment. Its owner shuffles it into their library.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val UnravelTheAether = card("Unravel the Aether") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose target artifact or enchantment. Its owner shuffles it into their library."

    spell {
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.ShuffleIntoLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Jung Park"
        flavorText = "Thaumaturges of Theros have learned to weave their magic into the world around them, but it can be pulled apart strand by strand."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5584c26f-62ac-4d0b-9142-5159f2a05734.jpg?1783939527"
        ruling(
            "2014-02-01",
            "If the artifact or enchantment is a token, it will be shuffled into its owner's library when Unravel the Aether resolves, then will cease to exist. For practical purposes, whatever item is representing the token should not actually be put into the library, though the library is still shuffled."
        )
    }
}
