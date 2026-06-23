package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shapeshifter
 * {6}
 * Artifact Creature — Shapeshifter
 * Power/toughness: star / 7-star
 *
 * As this creature enters, choose a number between 0 and 7.
 * At the beginning of your upkeep, you may choose a number between 0 and 7.
 * Shapeshifter's power is equal to the last chosen number and its toughness is equal to 7 minus
 * that number.
 *
 * Implementation: a [Effects.ChooseNumberForSource] (0–7) records the chosen number durably on the
 * permanent under [ChoiceSlot.CHOSEN_NUMBER] — once as it enters (a true as-enters replacement,
 * [EntersWithChoice]`(ChoiceType.NUMBER)`, CR 614.1c — chosen before it is on the battlefield, so
 * there is no priority window where it sits at its default) and again each upkeep (an optional
 * Triggers.YourUpkeep ability, the "you may", via [Effects.ChooseNumberForSource] into the same
 * slot). The P/T is a [SetBasePowerToughnessDynamicStatic] characteristic-defining ability reading
 * that last choice: power = CHOSEN_NUMBER, toughness = 7 − CHOSEN_NUMBER, so they always sum to 7.
 */
val Shapeshifter = card("Shapeshifter") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Shapeshifter"
    // Printed P/T are star / 7-star; placeholder 0/0 base is overwritten in Layer 7b by the CDA.
    power = 0
    toughness = 0
    oracleText = "As this creature enters, choose a number between 0 and 7.\n" +
        "At the beginning of your upkeep, you may choose a number between 0 and 7.\n" +
        "Shapeshifter's power is equal to the last chosen number and its toughness is equal to 7 " +
        "minus that number."

    // As it enters, choose a number (mandatory) — a true "As ~ enters" replacement (CR 614.1c),
    // resolved before the permanent is on the battlefield.
    replacementEffect(EntersWithChoice(choiceType = ChoiceType.NUMBER, minValue = 0, maxValue = 7))

    // At the beginning of your upkeep, you MAY choose a number. `optional = true` records the
    // printed "you may"; for this no-target/no-else trigger it raises no separate decline prompt,
    // and none is needed — re-choosing the current number is equivalent to declining (the CDA reads
    // the last choice either way), so the prior P/T is retained.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = Effects.ChooseNumberForSource(minValue = 0, maxValue = 7, prompt = "Choose a number between 0 and 7")
    }

    // CDA: power = last chosen number, toughness = 7 − it.
    staticAbility {
        ability = SetBasePowerToughnessDynamicStatic(
            power = DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER),
            toughness = DynamicAmount.Subtract(
                DynamicAmount.Fixed(7),
                DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)
            ),
            filter = GroupFilter.source()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Dan Frazier"
        flavorText = "Born like a Phoenix from the Flame, / But neither Bulk nor Shape the same.\n—Jonathan Swift, \"Vanbrug's House\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc278af4-b60d-41b7-b9d7-36c8aefca1a7.jpg?1592364284"
    }
}
