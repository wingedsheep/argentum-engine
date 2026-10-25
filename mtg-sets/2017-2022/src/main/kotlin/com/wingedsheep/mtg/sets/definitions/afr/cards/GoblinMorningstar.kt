package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Morningstar
 * {1}{R}
 * Artifact — Equipment
 * Equipped creature gets +1/+0 and has trample.
 * Equip {1}{R}
 * When this Equipment enters, roll a d20.
 * 1—9 | Create a 1/1 red Goblin creature token.
 * 10—20 | Create a 1/1 red Goblin creature token, then attach this Equipment to it.
 *
 * The 10—20 row is the Citizen's Crowbar shell inside the d20 table: the token is published to
 * [CREATED_TOKENS] and [Effects.AttachEquipment] reads it back.
 */
val GoblinMorningstar = card("Goblin Morningstar") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has trample.\n" +
        "Equip {1}{R}\n" +
        "When this Equipment enters, roll a d20.\n" +
        "1—9 | Create a 1/1 red Goblin creature token.\n" +
        "10—20 | Create a 1/1 red Goblin creature token, then attach this Equipment to it."

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    equipAbility("{1}{R}")

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun goblin() = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                imageUri = "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342",
            )
            ifThen(
                Conditions.All(
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 1),
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9),
                )
            ) {
                run(goblin())
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10)) {
                run(goblin() then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0)))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Kari Christensen"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/998f3785-c806-4848-adcb-d89ea51159c5.jpg?1783926479"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
