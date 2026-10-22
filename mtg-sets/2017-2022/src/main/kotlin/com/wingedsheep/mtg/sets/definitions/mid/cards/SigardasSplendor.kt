package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Sigarda's Splendor
 * {2}{W}{W}
 * Enchantment
 *
 * As this enchantment enters, note your life total.
 * At the beginning of your upkeep, draw a card if your life total is greater than or equal to the
 * last noted life total for this enchantment. Then note your life total.
 * Whenever you cast a white spell, you gain 1 life.
 *
 * The note lives on the permanent ([Effects.NoteNumberForSource] under
 * [ChoiceSlot.NOTED_NUMBER]) and is read back with `CastChoice`. The upkeep check is not an
 * intervening "if": the ability always triggers and always re-notes, drawing or not.
 */
val SigardasSplendor = card("Sigarda's Splendor") {
    manaCost = "{2}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, note your life total.\n" +
        "At the beginning of your upkeep, draw a card if your life total is greater than or equal to " +
        "the last noted life total for this enchantment. Then note your life total.\n" +
        "Whenever you cast a white spell, you gain 1 life."

    replacementEffect(OnEnterRun(Effects.NoteNumberForSource(DynamicAmounts.yourLifeTotal())))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.yourLifeTotal(),
                ComparisonOperator.GTE,
                DynamicAmounts.castChoice(ChoiceSlot.NOTED_NUMBER)
            ),
            Effects.DrawCards(1)
        ) then Effects.NoteNumberForSource(DynamicAmounts.yourLifeTotal())
        description = "At the beginning of your upkeep, draw a card if your life total is greater than or " +
            "equal to the last noted life total for this enchantment. Then note your life total."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.WHITE))
        effect = Effects.GainLife(1)
        description = "Whenever you cast a white spell, you gain 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb22626d-84ef-40d1-a4f2-771b45198eaa.jpg?1783925652"
    }
}
