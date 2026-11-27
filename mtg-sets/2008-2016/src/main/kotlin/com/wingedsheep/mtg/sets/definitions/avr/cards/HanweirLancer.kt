package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hanweir Lancer — Avacyn Restored #138 (canonical printing)
 * {2}{R} · Creature — Human Knight · 2/2
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have first strike.
 *
 * Wingcrafter's shape: [GrantKeyword] over `GroupFilter.soulbondPair()`, which is empty while
 * unpaired.
 */
val HanweirLancer = card("Hanweir Lancer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have first strike."
    power = 2
    toughness = 2

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73884fd1-5be9-4ad5-8c72-c0fbe27ad4c1.jpg?1783940684"
    }
}
