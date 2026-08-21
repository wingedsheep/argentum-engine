package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spectral Procession
 * {2/W}{2/W}{2/W}
 * Sorcery
 *
 * Create three 1/1 white Spirit creature tokens with flying.
 *
 * - Monocoloured hybrid ("twobrid") goes into `manaCost` verbatim; the three {2/W} symbols give
 *   this a mana value of 6, which the parser derives — nothing is overridden.
 * - One [Effects.CreateToken] with `count = 3` rather than three separate effects: the tokens are
 *   identical, and a single effect keeps "create three" as one event for triggers that count them.
 */
val SpectralProcession = card("Spectral Procession") {
    manaCost = "{2/W}{2/W}{2/W}"
    typeLine = "Sorcery"
    oracleText = "Create three 1/1 white Spirit creature tokens with flying."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            count = 3
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Jeremy Enecio"
        flavorText = "\"The dead have it easy. They suffer no more. If breaking their rest helps the living, so be it.\"\n" +
            "—Olka, mistmeadow witch"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e26bd0c-5ec7-4653-8e63-747157c20af1.jpg?1783942765"
    }
}
