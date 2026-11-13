package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unified Front
 * {3}{W}
 * Sorcery
 * Converge — Create a 1/1 white Kor Ally creature token for each color of mana spent to cast this spell.
 *
 * Converge: the token count is [DynamicAmounts.colorsOfManaSpent], read while the spell resolves
 * (the Infuse with the Elements / Snarl Song shape). Token art is BFZ's Kor Ally (Retreat to Emeria).
 */
val UnifiedFront = card("Unified Front") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Converge — Create a 1/1 white Kor Ally creature token for each color of mana spent " +
        "to cast this spell."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.colorsOfManaSpent(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Kor", "Ally"),
            imageUri = "https://cards.scryfall.io/normal/front/b/e/be224180-a482-4b94-8a9d-3a92ee0eb34b.jpg?1783938120",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Dan Murayama Scott"
        flavorText = "The kor's pride in their independence has given way to a strong sense of community " +
            "that embraces all of Zendikar."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53472387-6909-462e-8183-02fd1d45126e.jpg?1783938214"
    }
}
