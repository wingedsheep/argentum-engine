package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Primoc Escapee
 * {6}{U}
 * Creature — Bird Beast
 * 4/4
 * Flying
 * Cycling {2}
 */
val PrimocEscapee = card("Primoc Escapee") {
    manaCost = "{6}{U}"
    typeLine = "Creature — Bird Beast"
    power = 4
    toughness = 4
    oracleText = "Flying\nCycling {2}"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Tony Szczudlo"
        flavorText = "Though a completely artificial species, primocs are a natural fit for the skies of Otaria."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6cb3e72-bb64-4b1e-a54b-1fe4fb4ad4c9.jpg?1562941357"
    }
}
