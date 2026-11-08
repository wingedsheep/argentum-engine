package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Gnarlwood Dryad (Eldritch Moon #159)
 * {G}
 * Creature — Dryad Horror
 * 1/1
 *
 * Deathtouch
 * Delirium — This creature gets +2/+2 as long as there are four or more card types among cards
 * in your graveyard.
 */
val GnarlwoodDryad = card("Gnarlwood Dryad") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad Horror"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\n" +
        "Delirium — This creature gets +2/+2 as long as there are four or more card types among " +
        "cards in your graveyard."

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Raymond Swanland"
        flavorText = "As Emrakul's influence radiated across Innistrad, evacuation from Kessig's remote " +
            "villages became increasingly difficult."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa506387-e538-4b2c-94df-29866eb74a71.jpg"
    }
}
