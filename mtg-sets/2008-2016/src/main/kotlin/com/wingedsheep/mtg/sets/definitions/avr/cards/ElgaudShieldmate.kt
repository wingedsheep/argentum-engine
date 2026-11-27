package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Elgaud Shieldmate — Avacyn Restored #50 (canonical printing)
 * {3}{U} · Creature — Human Soldier · 2/3
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have hexproof.
 *
 * [Wingcrafter]'s shape with hexproof: [GrantKeyword] over `GroupFilter.soulbondPair()`, which is
 * empty while unpaired.
 */
val ElgaudShieldmate = card("Elgaud Shieldmate") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have hexproof. " +
        "(They can't be the targets of spells or abilities your opponents control.)"
    power = 2
    toughness = 3

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7d376ef-c900-4abb-9a0b-5eb9369f5739.jpg?1783940722"
    }
}
