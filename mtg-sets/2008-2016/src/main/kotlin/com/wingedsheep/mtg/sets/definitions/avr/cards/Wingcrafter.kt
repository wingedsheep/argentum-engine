package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wingcrafter — Avacyn Restored #83 (canonical printing)
 * {U} · Creature — Human Wizard · 1/1
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have flying.
 *
 * Spectral Gateguards' shape: [GrantKeyword] over `GroupFilter.soulbondPair()`, which is empty
 * while unpaired.
 */
val Wingcrafter = card("Wingcrafter") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have flying."
    power = 1
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a3059f-92f2-4163-b79a-154118a4e36d.jpg?1783940708"
    }
}
