package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nuclear Fallout — Fallout #47
 * {X}{B}{B} · Sorcery · Rare
 *
 * Each creature gets twice -X/-X until end of turn. Each player gets X rad counters.
 *
 * "Twice -X/-X" is `-(X * 2)` over [DynamicAmounts.xValue], applied to every creature
 * (Exude Toxin's shape). "Each player" includes the caster: `ForEachPlayer` rebinds `Controller`
 * to every player in turn (Megaton's Fate's shape), and the X paid survives into each iteration.
 */
val NuclearFallout = card("Nuclear Fallout") {
    manaCost = "{X}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Each creature gets twice -X/-X until end of turn. Each player gets X rad counters."

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -(DynamicAmounts.xValue() * 2),
            toughness = -(DynamicAmounts.xValue() * 2),
            filter = GroupFilter.AllCreatures
        ) then Effects.ForEachPlayer(
            Player.Each,
            Effects.AddDynamicCounters(CounterType.RAD, DynamicAmounts.xValue(), EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Jason Rainville"
        flavorText = "The end of the world occurred as predicted: too many humans, not enough space or " +
            "resources to go around. The details are trivial and pointless. The reasons, as always, " +
            "purely human ones."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e717e4b0-a8c1-4411-b18a-1d17e7225503.jpg?1783912408"
        ruling(
            "2024-03-08",
            "Rad counters are a kind of counter that a player may have. They're not associated with any " +
                "specific permanents."
        )
    }
}
