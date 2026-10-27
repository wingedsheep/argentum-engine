package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Olivia's Wrath
 * {4}{B}
 * Sorcery
 * Each non-Vampire creature gets -X/-X until end of turn, where X is the number of Vampires you
 * control.
 *
 * X is counted as the spell resolves and locked in for each affected creature; the -X/-X doesn't
 * touch Vampires, so the count can't move mid-resolution.
 */
val OliviasWrath = card("Olivia's Wrath") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each non-Vampire creature gets -X/-X until end of turn, where X is the number of " +
        "Vampires you control."

    spell {
        val vampires = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE)
        ).count()
        effect = Patterns.Group.modifyStatsForAll(
            -vampires,
            -vampires,
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.VAMPIRE))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"These worthless cattle think they can sneak unnoticed into my wedding? " +
            "Dispose of them.\"\n—Olivia Voldaren"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98893cc1-f502-4ca6-b6c1-e09fa1f4ef7a.jpg?1783925002"
    }
}
