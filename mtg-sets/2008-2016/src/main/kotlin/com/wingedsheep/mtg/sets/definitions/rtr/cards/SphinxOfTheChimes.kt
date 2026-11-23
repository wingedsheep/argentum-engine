package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sphinx of the Chimes
 * {4}{U}{U}
 * Creature — Sphinx
 * 5/6
 *
 * Flying
 * Discard two nonland cards with the same name: Draw four cards.
 */
val SphinxOfTheChimes = card("Sphinx of the Chimes") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Sphinx"
    power = 5
    toughness = 6
    oracleText = "Flying\nDiscard two nonland cards with the same name: Draw four cards."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Discard(GameObjectFilter.Nonland, count = 2, sameName = true)
        effect = Effects.DrawCards(4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Greg Staples"
        flavorText = "\"I've dreamt of the future, Isperia. Danger is on the horizon. You must heed my warning.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31a0cae5-925f-4d00-a4da-1db8bae5511b.jpg?1783940365"
    }
}
