package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cosmos Elixir
 * {4}
 * Artifact
 * At the beginning of your end step, draw a card if your life total is greater than your starting
 * life total. Otherwise, you gain 2 life.
 *
 * Not an intervening-if: the trigger always goes on the stack and the comparison is made as it
 * resolves (2021-02-05 ruling), so it's a resolution-time [Effects.If] with an `otherwise` branch.
 */
val CosmosElixir = card("Cosmos Elixir") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your end step, draw a card if your life total is greater than your starting life total. Otherwise, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.lifeTotal(Player.You),
                operator = ComparisonOperator.GT,
                right = DynamicAmounts.startingLifeTotal(Player.You),
            ),
            then = Effects.DrawCards(1),
            otherwise = Effects.GainLife(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Volkan Baǵa"
        flavorText = "The Skoti owe their immortality to the elixir, and the manner of its creation is their most closely guarded secret."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bf084fe-7762-49c5-974a-cdecc10666b3.jpg?1783928186"
        ruling(
            "2021-02-05",
            "You compare your current life total to your starting life total as Cosmos Elixir's ability resolves to determine whether you draw a card or gain 2 life. There are no situations in which you'll do both.",
        )
    }
}
