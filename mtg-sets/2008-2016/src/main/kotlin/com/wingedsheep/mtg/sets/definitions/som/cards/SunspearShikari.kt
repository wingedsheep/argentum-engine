package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sunspear Shikari — Scars of Mirrodin #23
 * {1}{W} · Creature — Cat Soldier · 2/2
 *
 * As long as this creature is equipped, it has first strike and lifelink.
 *
 * Same shape as Armory Veteran / Leonin Den-Guard: one static per keyword, each gated on
 * [Conditions.SourceMatches] over `equipped()`, re-evaluated during projection.
 */
val SunspearShikari = card("Sunspear Shikari") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 2
    toughness = 2
    oracleText = "As long as this creature is equipped, it has first strike and lifelink. " +
        "(It deals combat damage before creatures without first strike. " +
        "Damage dealt by it also causes you to gain that much life.)"

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
    }

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Allen Williams"
        flavorText = "Left without their leader Raksha, the leonin split into two prides: " +
            "one side supported the regent kha, while the other rebelled in fury."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20ac29ef-02e1-4500-bb83-5987beeaa849.jpg?1783941741"
    }
}
