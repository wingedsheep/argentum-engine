package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vesuva
 * Land
 * You may have this land enter tapped as a copy of any land on the battlefield.
 *
 * An optional battlefield-sourced [EntersAsCopy] restricted to lands, with the `tappedIfCopied`
 * rider. Declining (or having no land to copy) leaves it untapped as its printed self, with no
 * abilities — it can't tap for mana.
 */
val Vesuva = card("Vesuva") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "You may have this land enter tapped as a copy of any land on the battlefield."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Land,
            tappedIfCopied = true,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "It is everywhere you've ever been."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82fc9498-7397-4857-87fe-7c9010944ed8.jpg?1783943192"
        ruling("2021-03-19", "Vesuva copies exactly what is printed on the land it's copying and nothing else (unless it's copying a land that's copying something else). It doesn't copy whether a land is tapped or untapped, counters, Auras, or non-copy effects that changed its types.")
        ruling("2021-03-19", "If Vesuva somehow enters the battlefield at the same time as another land, Vesuva can't become a copy of that land. You may choose only a land that's already on the battlefield.")
        ruling("2021-03-19", "If you don't choose a land on the battlefield, Vesuva enters the battlefield untapped as itself, and will not be able to tap for mana.")
    }
}
