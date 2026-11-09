package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Moldgraf Scavenger (Shadows over Innistrad #218)
 * {1}{G}
 * Creature — Fungus
 * 0/4
 *
 * Delirium — This creature gets +3/+0 as long as there are four or more card types among cards in
 * your graveyard.
 */
val MoldgrafScavenger = card("Moldgraf Scavenger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    power = 0
    toughness = 4
    oracleText = "Delirium — This creature gets +3/+0 as long as there are four or more card types among " +
        "cards in your graveyard."

    staticAbility {
        ability = ModifyStats(3, 0, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Tomasz Jedruszek"
        flavorText = "\"When I was a child, we worried about picking the wrong mushrooms for the cook pot. Now " +
            "we must worry about being picked ourselves.\"\n—Halana, Kessig ranger"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76755763-7c02-451f-b799-2106a3a4973b.jpg"
    }
}
