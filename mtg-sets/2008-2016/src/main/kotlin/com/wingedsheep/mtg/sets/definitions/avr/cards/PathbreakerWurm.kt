package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pathbreaker Wurm — Avacyn Restored #188 (canonical printing)
 * {4}{G}{G} · Creature — Wurm · 6/4
 *
 * Soulbond
 * As long as this creature is paired with another creature, both creatures have trample.
 *
 * [Wingcrafter] with trample: [GrantKeyword] over `GroupFilter.soulbondPair()`, empty while unpaired.
 */
val PathbreakerWurm = card("Pathbreaker Wurm") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have trample."
    power = 6
    toughness = 4

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe65eded-37ef-4cf0-b55c-390d34aab7b8.jpg?1783940663"
    }
}
