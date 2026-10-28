package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Renewed Solidarity — Aetherdrift Commander #7 (canonical printing)
 * {2}{W} · Enchantment
 *
 * As this enchantment enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+0.
 * At the beginning of your end step, for each token you control of the chosen type that entered
 * this turn, create a token that's a copy of it.
 *
 * The end-step group is snapshotted before any copy is made, so the new copies (which also
 * entered this turn) are never copied again by the same resolution.
 */
val RenewedSolidarity = card("Renewed Solidarity") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +1/+0.\n" +
        "At the beginning of your end step, for each token you control of the chosen type that " +
        "entered this turn, create a token that's a copy of it."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Permanent.token().youControl().withChosenSubtype().enteredThisTurn()
            ),
            effect = Effects.CreateTokenCopyOfTarget(EffectTarget.IterationEntity)
        )
        description = "At the beginning of your end step, for each token you control of the chosen type " +
            "that entered this turn, create a token that's a copy of it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Julie Dillon"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a72b230-dbb0-4d38-ba7a-fb0bd03b6c47.jpg?1783907752"
        ruling("2025-02-07", "When this enchantment's last ability resolves, for each applicable token you control that entered this turn, create a token that copies the original characteristics of that first token as stated by the effect that created it (unless that first token is copying something else). It doesn't copy whether that token is tapped or untapped, whether it has any counters on it or Auras or Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2025-02-07", "If the last ability creates a copy of a token that is copying something else, then the new token enters as a copy of whatever the original token copied.")
    }
}
