package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Moorland Drifter (Shadows over Innistrad #27)
 * {1}{W}
 * Creature — Spirit
 * 2/2
 *
 * Delirium — This creature has flying as long as there are four or more card types among cards in
 * your graveyard.
 */
val MoorlandDrifter = card("Moorland Drifter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Delirium — This creature has flying as long as there are four or more card types among " +
        "cards in your graveyard."

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Ryan Yee"
        flavorText = "It has no apparent purpose or direction. It simply drifts, buoyed by breezes both " +
            "tangible and otherworldly."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/5839942c-0ad1-4f77-97a0-142b8ef2266c.jpg"
    }
}
