package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gala Greeters
 * {1}{G}
 * Creature — Elf Druid
 * 1/1
 *
 * Alliance — Whenever another creature you control enters, choose one that hasn't been chosen this turn —
 * • Put a +1/+1 counter on this creature.
 * • Create a tapped Treasure token.
 * • You gain 2 life.
 *
 * "Choose one that hasn't been chosen this turn" is [ModalEffect.chooseOneNotYetChosenThisTurn]
 * (per-source, turn-scoped memory; the mode is announced as each trigger goes on the stack, so
 * simultaneous entries must pick different modes, and a fourth trigger in a turn has no legal mode).
 */
val GalaGreeters = card("Gala Greeters") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "Alliance — Whenever another creature you control enters, choose one that hasn't been chosen this turn —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Create a tapped Treasure token.\n" +
        "• You gain 2 life."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            Mode.noTarget(
                Effects.CreateTreasure(tapped = true),
                "Create a tapped Treasure token",
            ),
            Mode.noTarget(
                Effects.GainLife(2),
                "You gain 2 life",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Bud Cook"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c1baaa2-bd0b-4627-b93a-0753e0acd0f2.jpg?1783923104"

        ruling("2022-04-29", "If multiple creatures enter the battlefield simultaneously, you must still choose different modes for each instance of the triggered ability that's put onto the stack. If more than three creatures enter the battlefield simultaneously, that choice is made only for the first three.")
    }
}
