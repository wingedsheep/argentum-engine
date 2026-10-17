package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Halfdane
 * {1}{W}{U}{B}
 * Legendary Creature — Shapeshifter
 * 3/3
 *
 * At the beginning of your upkeep, Halfdane's base power and toughness become equal to the power
 * and toughness of target creature other than Halfdane until the end of your next upkeep.
 *
 * The target's power and toughness are read once, as the ability resolves (a layer-7b set, not a
 * continuously re-read copy). Each upkeep's new setting overlaps the previous one for the rest of
 * that upkeep and wins on timestamp; the previous one then ends with the step.
 */
val Halfdane = card("Halfdane") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "BUW"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your upkeep, Halfdane's base power and toughness become equal to " +
        "the power and toughness of target creature other than Halfdane until the end of your next upkeep."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.SetBasePowerAndToughness(
            power = DynamicAmounts.powerOf(creature),
            toughness = DynamicAmounts.toughnessOf(creature),
            target = EffectTarget.Self,
            duration = Duration.EndOfYourNextUpkeep,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Melissa A. Benson"
        flavorText = "Hail from Tolaria the ever changing 'Dane."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e939761-3542-4044-9038-d1d30c6a38fc.jpg?1783948039"
    }
}
