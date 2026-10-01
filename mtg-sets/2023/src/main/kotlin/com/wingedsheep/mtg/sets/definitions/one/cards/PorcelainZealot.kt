package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Porcelain Zealot
 * {3}{W}
 * Creature — Phyrexian Soldier
 * 2/3
 *
 * At the beginning of combat on your turn, target creature you control gets +1/+1 until end of
 * turn. If that creature has toxic, instead it gets +2/+2 until end of turn.
 *
 * "Instead" is an either/or on resolution: `Effects.If(otherwise = …)` with the same "has toxic"
 * check as [CompleatDevotion] — `withKeyword(TOXIC)` matches any toxic N, printed or granted, off
 * the projected `TOXIC_<n>` keyword.
 */
val PorcelainZealot = card("Porcelain Zealot") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Soldier"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, target creature you control gets +1/+1 " +
        "until end of turn. If that creature has toxic, instead it gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(Filters.Creature.withKeyword(Keyword.TOXIC), creature),
            then = Effects.ModifyStats(2, 2, creature),
            otherwise = Effects.ModifyStats(1, 1, creature),
        )
        description = "At the beginning of combat on your turn, target creature you control gets " +
            "+1/+1 until end of turn. If that creature has toxic, instead it gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Krharts"
        flavorText = "\"There is a single, glorious truth, etched in the hearts of the faithful. Join in unity, or be washed away.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0409bacf-b728-473d-bf9b-41fdf364e783.jpg?1783918074"
    }
}
