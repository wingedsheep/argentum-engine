package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Megaton's Fate — Fallout #61
 * {5}{R} · Sorcery · Rare
 *
 * Choose one —
 * • Disarm — Destroy target artifact. Create four Treasure tokens.
 * • Detonate — Megaton's Fate deals 8 damage to each creature. Each player gets four rad counters.
 *
 * "Each player" includes the caster: `ForEachPlayer` rebinds `Controller` to every player in turn
 * (Disorder's shape), so each of them gets four rad counters.
 */
val MegatonsFate = card("Megaton's Fate") {
    manaCost = "{5}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Disarm — Destroy target artifact. Create four Treasure tokens.\n" +
        "• Detonate — Megaton's Fate deals 8 damage to each creature. Each player gets four rad counters."

    spell {
        effect = Effects.Modal(
            modes = listOf(
                mode("Disarm — Destroy target artifact. Create four Treasure tokens.") {
                    val artifact = target(TargetFilter.Artifact)
                    effect = Effects.Destroy(artifact) then Effects.CreateTreasure(
                        count = 4,
                        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d74d012-66ca-4a0c-9e90-e31e96730d4d.jpg?1783912040"
                    )
                },
                Mode.noTarget(
                    effect = Effects.ForEachInGroup(
                        filter = GroupFilter.AllCreatures,
                        effect = Effects.DealDamage(8, EffectTarget.IterationEntity)
                    ) then Effects.ForEachPlayer(
                        Player.Each,
                        Effects.AddCounters(CounterType.RAD, 4, EffectTarget.Controller)
                    ),
                    description = "Detonate — Megaton's Fate deals 8 damage to each creature. " +
                        "Each player gets four rad counters."
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Chris Cold"
        flavorText = "Do the right thing—or the wrong thing."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8492127-888d-4da0-b49f-fba6a356b0a9.jpg?1783912403"
        ruling(
            "2024-03-08",
            "Rad counters are a kind of counter that a player may have. They're not associated with any " +
                "specific permanents."
        )
    }
}
