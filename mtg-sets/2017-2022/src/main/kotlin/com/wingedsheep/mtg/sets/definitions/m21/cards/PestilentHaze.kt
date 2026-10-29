package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pestilent Haze — Core Set 2021 #118
 * {1}{B}{B} · Sorcery
 *
 * Choose one —
 * • All creatures get -2/-2 until end of turn.
 * • Remove two loyalty counters from each planeswalker.
 *
 * A choose-one `modal`: an Infest-shaped group shrink over the creatures on the battlefield as it
 * resolves (later arrivals are unaffected, per the ruling), or a per-planeswalker loyalty removal
 * (a planeswalker with fewer than two loses all it has; one left at 0 dies to SBAs).
 */
val PestilentHaze = card("Pestilent Haze") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• All creatures get -2/-2 until end of turn.\n" +
        "• Remove two loyalty counters from each planeswalker."

    spell {
        modal(chooseCount = 1) {
            mode("All creatures get -2/-2 until end of turn.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreatures,
                    Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
                )
            }
            mode("Remove two loyalty counters from each planeswalker.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Planeswalker),
                    Effects.RemoveCounters(CounterType.LOYALTY, 2, EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Your crops will rot. The air itself will turn against you. You will suffer as I have suffered.\"\n—Karaz, outcast"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08b78aa8-a63a-4aa2-bb82-3fbf2595ed7c.jpg?1783930701"
        ruling("2020-06-23", "Pestilent Haze's first mode affects only creatures on the battlefield at the time it resolves. Creatures that enter the battlefield later in the turn won't get -2/-2.")
    }
}
