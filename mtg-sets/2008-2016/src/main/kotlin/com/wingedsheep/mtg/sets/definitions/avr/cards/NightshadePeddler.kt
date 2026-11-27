package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nightshade Peddler — Avacyn Restored #187 (canonical printing)
 * {1}{G} · Creature — Human Druid · 1/1
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have deathtouch.
 *
 * [Wingcrafter] with deathtouch: [GrantKeyword] over `GroupFilter.soulbondPair()`, empty while unpaired.
 */
val NightshadePeddler = card("Nightshade Peddler") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have deathtouch."
    power = 1
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d3de66c-2283-458f-9d0d-943027520aa2.jpg?1783940665"
    }
}
