package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Goliath Hatchery
 * {4}{G}{G}
 * Enchantment
 * When this enchantment enters, create two 3/3 green Phyrexian Beast creature tokens with toxic 1.
 * Corrupted — At the beginning of your upkeep, if an opponent has three or more poison counters,
 * choose a creature you control, then draw cards equal to its total toxic value.
 *
 * "Choose" is not "target": the creature is picked on resolution from the creatures you control,
 * and its total toxic value is `KeywordValue(TOXIC)` — every toxic instance summed (CR 702.164b).
 * The corrupted clause is an intervening "if" (CR 603.4), re-checked on resolution.
 */
val GoliathHatchery = card("Goliath Hatchery") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create two 3/3 green Phyrexian Beast creature tokens with toxic 1. " +
        "(Players dealt combat damage by them also get a poison counter.)\n" +
        "Corrupted — At the beginning of your upkeep, if an opponent has three or more poison counters, " +
        "choose a creature you control, then draw cards equal to its total toxic value."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Phyrexian", "Beast"),
            numericKeywords = listOf(KeywordAbility.toxic(1)),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/9/1/919381b0-2d23-4794-b4ff-923c23e18196.jpg?1783918168",
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Corrupted
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
            val chosen = chooseExactly(
                1,
                from = creatures,
                chooser = Chooser.Controller,
                prompt = "Choose a creature you control — draw cards equal to its total toxic value",
                useTargetingUI = true,
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.DrawCards(
                        DynamicAmounts.propertyOf(
                            EffectTarget.IterationEntity,
                            EntityNumericProperty.KeywordValue(Keyword.TOXIC),
                        )
                    ),
                )
            )
        }
        description = "Corrupted — At the beginning of your upkeep, if an opponent has three or more poison " +
            "counters, choose a creature you control, then draw cards equal to its total toxic value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "408"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e1141ca-78c0-46e5-99f8-28069d69f23a.jpg?1783917918"
    }
}
