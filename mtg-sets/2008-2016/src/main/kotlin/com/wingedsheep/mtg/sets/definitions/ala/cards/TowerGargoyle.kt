package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tower Gargoyle
 * {1}{W}{U}{B}
 * Artifact Creature — Gargoyle
 * 4 / 4
 *
 * Flying
 *
 * A single evergreen keyword, so the whole script is one [Keyword.FLYING] entry on the card's
 * keyword set — the builder derives the `Simple` keyword ability from it, and no triggered,
 * activated or static ability is needed.
 */
val TowerGargoyle = card("Tower Gargoyle") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Artifact Creature — Gargoyle"
    power = 4
    toughness = 4
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Matt Cavotta"
        flavorText = "\"Esper, like any work of art, can be truly appreciated only from a distance.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10504d1b-8d3e-411a-bf40-51a8e7a863a0.jpg"
    }
}
