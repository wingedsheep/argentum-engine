package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cantankerous Captain — Star Trek #141
 * {2}{R} · Creature — Tellarite Officer · 2/3
 *
 * At the beginning of combat on your turn, target creature you control gets +1/+0 until end of
 * turn. Then that creature gains menace until end of turn if creatures you control have total
 * power 8 or greater.
 *
 * "Then … if" is checked on resolution, after the +1/+0 lands — so the pump itself can tip the
 * total to 8 — hence an `Effects.If` sequenced after the stat change rather than a trigger gate.
 */
val CantankerousCaptain = card("Cantankerous Captain") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Tellarite Officer"
    oracleText = "At the beginning of combat on your turn, target creature you control gets +1/+0 " +
        "until end of turn. Then that creature gains menace until end of turn if creatures you " +
        "control have total power 8 or greater. (It can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                ComparisonOperator.GTE,
                8
            ),
            then = Effects.GrantKeyword(Keyword.MENACE, creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Edouard"
        flavorText = "\"Are you the best crew in the fleet or not?!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c053ec5-d1ea-4526-8a0d-e4dfafb8ecdf.jpg?1786005671"
    }
}
