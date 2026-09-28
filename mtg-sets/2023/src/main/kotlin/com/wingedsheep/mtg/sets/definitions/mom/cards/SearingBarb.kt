package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Searing Barb
 * {2}{R}
 * Sorcery
 * Searing Barb deals 2 damage to any target. If it's a creature, it can't block this turn. Incubate 1.
 */
val SearingBarb = card("Searing Barb") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Searing Barb deals 2 damage to any target. If it's a creature, it can't block this turn. " +
        "Incubate 1. (Create an Incubator token with a +1/+1 counter on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)"

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature, t),
                then = Effects.CantBlock(t),
            ) then
            Effects.Incubate(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Tiffany Turrill"
        flavorText = "\"You always were such a sharp student. Shame it took you so long to get the point.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69f156b6-68c2-4787-b3f0-6d1079bb576f.jpg?1783916981"
    }
}
