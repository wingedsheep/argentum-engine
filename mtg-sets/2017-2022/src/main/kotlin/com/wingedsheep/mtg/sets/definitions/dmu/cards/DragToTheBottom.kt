package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Drag to the Bottom
 * {2}{B}{B}
 * Sorcery
 * Domain — Each creature gets -X/-X until end of turn, where X is 1 plus the number of basic
 * land types among lands you control.
 */
val DragToTheBottom = card("Drag to the Bottom") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Domain — Each creature gets -X/-X until end of turn, where X is 1 plus the number of basic land types among lands you control."

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -(DynamicAmounts.domain() + 1),
            toughness = -(DynamicAmounts.domain() + 1),
            filter = GroupFilter.AllCreatures,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Nino Is"
        flavorText = "Windgrace's gladehunters hibernate in the quagmires of Urborg until the scent of Phyrexian technology awakens their hunger."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d243099-94b1-4571-96af-4c5a34241911.jpg?1783921333"
        ruling("2022-09-09", "Domain abilities count the number of basic land types among lands you control, not how many lands you control or how many of any type.")
        ruling("2022-09-09", "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Land types other than basic land types (such as Desert) don't contribute to domain abilities.")
    }
}
