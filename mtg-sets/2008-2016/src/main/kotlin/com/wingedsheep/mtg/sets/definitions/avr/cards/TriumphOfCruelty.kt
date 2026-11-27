package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Triumph of Cruelty — Avacyn Restored #122
 * {2}{B} · Enchantment
 *
 * At the beginning of your upkeep, target opponent discards a card if you control the creature with
 * the greatest power or tied for the greatest power.
 *
 * **Not an intervening-if**: the "if" trails the effect, so the ability triggers every upkeep and the
 * condition is checked only on resolution (ruling). Same condition as Triumph of Ferocity: you control
 * a creature and no creature another player controls has power strictly greater than the greatest
 * power among yours.
 */
val TriumphOfCruelty = card("Triumph of Cruelty") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, target opponent discards a card if you control the " +
        "creature with the greatest power or tied for the greatest power."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val opponent = target(Targets.Opponent)
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Creature),
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(
                        Player.EachOtherPlayer,
                        GameObjectFilter.Creature.compareNumericProperty(
                            CardNumericProperty.POWER,
                            ComparisonOperator.GT,
                            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
                        ),
                    ).count(),
                    ComparisonOperator.EQ,
                    0,
                ),
            ),
            then = Patterns.Hand.discardCards(1, opponent),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Izzy"
        flavorText = "\"I've seen corpses prettier than you, beastmage.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/906618e2-2638-4017-9d6e-e6f282967a81.jpg?1783940690"
        ruling(
            "2012-05-01",
            "Triumph of Cruelty's ability will trigger at the beginning of each of your upkeeps regardless " +
                "of who controls the creature with the greatest power. The ability checks whether the opponent " +
                "will discard a card when it resolves."
        )
    }
}
