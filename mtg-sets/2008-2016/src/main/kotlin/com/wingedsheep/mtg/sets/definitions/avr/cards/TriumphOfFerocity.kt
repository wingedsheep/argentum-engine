package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Triumph of Ferocity — Avacyn Restored #198
 * {2}{G} · Enchantment
 *
 * At the beginning of your upkeep, draw a card if you control the creature with the greatest power
 * or tied for the greatest power.
 *
 * **Not an intervening-if**: the "if" trails the effect, so the ability triggers every upkeep and the
 * condition is checked only on resolution (ruling). "You control the creature with the greatest power
 * or tied for it" holds exactly when you control a creature and no creature another player controls
 * has power strictly greater than the greatest power among yours — a tie with an opposing creature
 * still draws.
 */
val TriumphOfFerocity = card("Triumph of Ferocity") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, draw a card if you control the creature with the " +
        "greatest power or tied for the greatest power."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
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
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "James Ryman"
        flavorText = "\"Rid me of this curse, witch, or die with me.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bb41fa6-0cc6-43e5-9aa8-fcd9c781f4ce.jpg?1783940661"
        ruling(
            "2012-05-01",
            "Triumph of Ferocity's ability will trigger at the beginning of each of your upkeeps regardless " +
                "of who controls the creature with the greatest power. The ability checks whether you'll draw " +
                "a card when it resolves."
        )
    }
}
