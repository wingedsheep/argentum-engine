package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cauldron's Gift
 * {4}{B}
 * Sorcery
 * Adamant — If at least three black mana was spent to cast this spell, mill four cards.
 * You may choose a creature card in your graveyard. If you do, return it to the battlefield with
 * an additional +1/+1 counter on it.
 *
 * Adamant reads this spell's own cast payment ([DynamicAmounts.manaOfColorSpent], as Unexplained
 * Vision does). The creature is chosen on resolution — no "target" — after the mill, so a freshly
 * milled creature is a legal pick; `chooseUpTo(1)` models "you may". The +1/+1 counter rides the
 * zone change itself (`addCounterType`, as Generous Revival does).
 */
val CauldronsGift = card("Cauldron's Gift") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Adamant — If at least three black mana was spent to cast this spell, mill four cards.\n" +
        "You may choose a creature card in your graveyard. If you do, return it to the battlefield " +
        "with an additional +1/+1 counter on it."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.If(
                    Conditions.CompareAmounts(
                        DynamicAmounts.manaOfColorSpent(Color.BLACK),
                        ComparisonOperator.GTE,
                        3,
                    ),
                    Patterns.Library.mill(4),
                )
            )
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Creature))
            val chosen = chooseUpTo(
                1,
                from = creatures,
                showAllCards = true,
                prompt = "You may return a creature card from your graveyard to the battlefield",
                selectedLabel = "Return to battlefield",
                remainderLabel = "Leave in graveyard",
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5db19a94-1170-45a0-9f06-893bf58b7233.jpg?1783932642"
    }
}
