package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rise of the Ants
 * {4}{G}{G}
 * Sorcery
 * Create two 3/3 green Insect creature tokens. You gain 2 life.
 * Flashback {6}{G}{G}
 */
val RiseOfTheAnts = card("Rise of the Ants") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create two 3/3 green Insect creature tokens. You gain 2 life.\n" +
        "Flashback {6}{G}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d53e20ee-b43c-45aa-9921-2c6f7ddc27fb.jpg?1783925222"
        ) then Effects.GainLife(2)
    }

    keywordAbility(KeywordAbility.flashback("{6}{G}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3143fadd-0162-4e0c-89fc-89dc3501d13c.jpg"
    }
}
