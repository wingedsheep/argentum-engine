package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Summons
 * {1}{W}
 * Sorcery
 * Manifest the top card of your library.
 */
val SoulSummons = card("Soul Summons") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Manifest the top card of your library. (Put it onto the battlefield face down as a 2/2 " +
        "creature. Turn it face up any time for its mana cost if it's a creature card.)"

    spell {
        effect = Patterns.Library.manifest()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Johann Bodin"
        flavorText = "Ugin's magic reaches beyond the dragons. The clans have adapted it for war."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb884ac2-c990-4ab0-8610-e07d93da2f13.jpg?1783938709"
    }
}
