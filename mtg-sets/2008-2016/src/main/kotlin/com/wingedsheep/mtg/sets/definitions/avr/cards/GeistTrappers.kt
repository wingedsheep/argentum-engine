package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Geist Trappers — Avacyn Restored #179 (canonical printing)
 * {4}{G} · Creature — Human Warrior · 3/5
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have reach.
 *
 * [Wingcrafter] with reach: [GrantKeyword] over `GroupFilter.soulbondPair()`, empty while unpaired.
 */
val GeistTrappers = card("Geist Trappers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have reach."
    power = 3
    toughness = 5

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26f00a18-0ff7-42e4-be16-aa34fe27093b.jpg?1783940667"
    }
}
