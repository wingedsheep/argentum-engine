package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Diffusion Sliver
 * {1}{U}
 * Creature — Sliver
 * 1/1
 * Whenever a Sliver creature you control becomes the target of a spell or ability an opponent
 * controls, counter that spell or ability unless its controller pays {2}.
 *
 * An observer becomes-target trigger over Sliver creatures you control — `Triggers.a` watches
 * Diffusion Sliver itself too (Thunderbreak Regent's shape) — narrowed to opponents' spells and
 * abilities. The effect is Valkmira's printed-ward counter: it counters the spell or ability that
 * made the trigger fire (not the targeted Sliver) unless its controller pays {2}. Like the other
 * Magic 2015 Slivers it only watches *your* Slivers.
 */
val DiffusionSliver = card("Diffusion Sliver") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "Whenever a Sliver creature you control becomes the target of a spell or ability an " +
        "opponent controls, counter that spell or ability unless its controller pays {2}."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Sliver").youControl())
            .becomesTarget(byOpponent = true)
        effect = Effects.CounterTargetingSourceUnlessPays(WardCost.Mana("{2}"))
        description = "Whenever a Sliver creature you control becomes the target of a spell or ability " +
            "an opponent controls, counter that spell or ability unless its controller pays {2}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Trevor Claxton"
        flavorText = "\"The hive shimmered, and its walls seemed a living mirror.\"\n—Hastric, Thunian scout"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb110a55-c8f9-4627-82c2-edb10db4f380.jpg?1783939194"
        ruling("2014-07-18", "The ability applies to any spell (including Aura spells), activated ability, or triggered ability that's controlled by an opponent that targets a Sliver you control.")
        ruling("2014-07-18", "Slivers in this set affect only Sliver creatures you control. They don't grant bonuses to your opponents' Slivers.")
    }
}
