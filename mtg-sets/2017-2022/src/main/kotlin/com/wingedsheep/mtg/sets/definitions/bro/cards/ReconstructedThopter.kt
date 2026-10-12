package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity

/**
 * Reconstructed Thopter
 * {3}
 * Artifact Creature — Thopter
 * 2/1
 * Flying
 * Unearth {2}
 */
val ReconstructedThopter = card("Reconstructed Thopter") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Thopter"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Unearth {2} ({2}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    keywords(Keyword.FLYING)

    unearth("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Dan Murayama Scott"
        flavorText = "From above, the brothers noticed strange glyphs etched into the desert sand."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcc8a8d8-6d8e-44b3-a29b-5fa4a851a25b.jpg?1783920015"
    }
}
