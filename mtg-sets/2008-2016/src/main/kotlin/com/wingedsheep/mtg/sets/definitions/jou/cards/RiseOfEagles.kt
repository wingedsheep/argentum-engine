package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rise of Eagles
 * {4}{U}{U}
 * Sorcery
 *
 * Create two 2/2 blue Bird enchantment creature tokens with flying. Scry 1.
 *
 * The Birds are *enchantment* creatures (`enchantmentToken = true`). Journey into Nyx printed no
 * Bird token; Scryfall links Rise of Eagles to Born of the Gods' 2/2 blue Bird enchantment creature
 * token, so that art is named explicitly.
 */
val RiseOfEagles = card("Rise of Eagles") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create two 2/2 blue Bird enchantment creature tokens with flying. Scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/c/a/ca6ce21f-2a29-480e-91f9-b089232863ff.jpg?1783939516",
            enchantmentToken = true,
        ) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Noah Bradley"
        flavorText = "An offering to the sea brings an insight from the stars."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3b5b278-b883-4703-9bfa-bab5a6da4660.jpg?1783939443"
    }
}
