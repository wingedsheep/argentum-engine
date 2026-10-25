package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Armory Veteran — Adventures in the Forgotten Realms #130
 * {1}{R} · Creature — Orc Warrior · 2/2
 *
 * As long as this creature is equipped, it has menace.
 *
 * Same shape as Leonin Den-Guard: a static gated on [Conditions.SourceMatches] over `equipped()`,
 * re-evaluated during projection, so attaching or unattaching Equipment toggles menace immediately.
 */
val ArmoryVeteran = card("Armory Veteran") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Warrior"
    power = 2
    toughness = 2
    oracleText = "As long as this creature is equipped, it has menace. (It can't be blocked except by two or more creatures.)"

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Caio Monteiro"
        flavorText = "\"This one is for goblins, this one is for owlbears, this one is for toast, this one is for . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4177f57e-c239-47e8-ae50-5f593f6d54ec.jpg?1783926485"
    }
}
