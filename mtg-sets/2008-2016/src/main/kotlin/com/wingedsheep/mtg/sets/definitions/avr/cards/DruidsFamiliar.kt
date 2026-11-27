package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Druid's Familiar — Avacyn Restored #175 (canonical printing)
 * {3}{G} · Creature — Bear · 2/2
 *
 * Soulbond
 * As long as this creature is paired with another creature, each of those creatures gets +2/+2.
 *
 * [LightningMauler]'s shape with [ModifyStats] in place of a keyword grant:
 * `GroupFilter.soulbondPair()` covers "each of those creatures" and is empty while unpaired.
 */
val DruidsFamiliar = card("Druid's Familiar") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, each of those creatures gets +2/+2."
    power = 2
    toughness = 2

    soulbond()

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d8c794a-7964-41f0-bc9c-27af7cf87aaa.jpg?1783940669"
    }
}
