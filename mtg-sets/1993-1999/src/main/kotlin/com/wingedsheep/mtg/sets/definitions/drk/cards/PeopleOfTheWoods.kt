package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * People of the Woods
 * {G}{G}
 * Creature — Human
 * Power 1, toughness *
 * People of the Woods's toughness is equal to the number of Forests you control.
 *
 * A characteristic-defining ability (CR 604.3): only the toughness is dynamic, so this uses
 * [dynamicToughness] rather than the `*`/`*` [dynamicStats] cycle.
 */
val PeopleOfTheWoods = card("People of the Woods") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    power = 1
    toughness = 0
    oracleText = "People of the Woods's toughness is equal to the number of Forests you control."

    dynamicToughness(
        DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.FOREST))
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Drew Tucker"
        flavorText = "\"Their rain of arrows left only myself alive, cowering within a tree hollow. They did not even come out to loot the bodies.\"\n—Vervamon the Elder"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fb5926f-9988-4bc0-b2b7-e286db208310.jpg?1783947930"
    }
}
