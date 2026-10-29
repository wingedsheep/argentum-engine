package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kaervek, the Spiteful — Core Set 2021 #106 (canonical printing)
 * {2}{B}{B} · Legendary Creature — Human Warlock · 3/2
 *
 * Other creatures get -1/-1.
 *
 * A layer-7c static over every other creature on the battlefield, either controller's.
 */
val KaervekTheSpiteful = card("Kaervek, the Spiteful") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "Other creatures get -1/-1."

    staticAbility {
        ability = ModifyStats(-1, -1, GroupFilter.AllOtherCreatures)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Daarken"
        flavorText = "\"Defy me and I will burn the flesh from your bones. Betray me and I will steal the breath from your lungs.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0fd1009-cd3d-4b53-b9f1-dbc47e8708ab.jpg?1783930706"
        ruling("2020-06-23", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to creatures may become lethal if Kaervek enters the battlefield during that turn.")
        ruling("2020-06-23", "If you control more than one Kaervek, each will give each other creature -1/-1. If these effects cause a creature's toughness to become less than or equal to 0 or the amount of damage marked on it, those creatures die at the same time that the \"legend rule\" remove your excess Kaerveks.")
    }
}
