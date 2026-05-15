package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val SakuraTribeElder = card("Sakura-Tribe Elder") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Shaman"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = EffectPatterns.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3696297a-b068-4e89-a62c-6cd5ad437618.jpg?1721429366"
    }
}
