package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Silverblade Paladin — Avacyn Restored #36 (canonical printing)
 * {1}{W}{W} · Creature — Human Knight · 2/2
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have double strike.
 *
 * Wingcrafter's shape: [GrantKeyword] over `GroupFilter.soulbondPair()`, which is empty while unpaired.
 */
val SilverbladePaladin = card("Silverblade Paladin") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have double strike."
    power = 2
    toughness = 2

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16298ca0-80d4-4299-a550-500b7ef6ac67.jpg?1783940729"
    }
}
