package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Kessig Dire Swine (Shadows over Innistrad #214)
 * {4}{G}{G}
 * Creature — Boar Horror
 * 6/6
 *
 * Delirium — This creature has trample as long as there are four or more card types among cards in
 * your graveyard.
 */
val KessigDireSwine = card("Kessig Dire Swine") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar Horror"
    power = 6
    toughness = 6
    oracleText = "Delirium — This creature has trample as long as there are four or more card types among " +
        "cards in your graveyard."

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Jason Kang"
        flavorText = "In the quiet of the Somberwald, new horrors stir."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9235ac46-dac9-46e6-9c7f-a69caaee1a2e.jpg"
    }
}
