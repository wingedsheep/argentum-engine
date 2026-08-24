package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Farrelite Priest
 * {1}{W}{W}
 * Creature — Human Cleric
 * 1/3
 * {1}: Add {W}. If this ability has been activated four or more times this turn, sacrifice this
 * creature at the beginning of the next end step.
 *
 * The burnout clause reads a tally rather than imposing a limit, so the ability opts into
 * per-turn activation bookkeeping (`trackActivations`) and reads it back with
 * [Conditions.ThisAbilityActivatedThisTurnAtLeast]. The count includes the activation that is
 * resolving, so the fourth one schedules the sacrifice; further activations schedule it again,
 * which is harmless — the creature is sacrificed once at the next end step either way.
 */
val FarrelitePriest = card("Farrelite Priest") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "{1}: Add {W}. If this ability has been activated four or more times this " +
        "turn, sacrifice this creature at the beginning of the next end step."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        trackActivations = true
        effect = Effects.AddMana(Color.WHITE).then(
            ConditionalEffect(
                condition = Conditions.ThisAbilityActivatedThisTurnAtLeast(4),
                effect = CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = SacrificeSelfEffect,
                )
            )
        )
        description = "{1}: Add {W}. If this ability has been activated four or more times this turn, sacrifice this creature at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Phil Foglio"
        flavorText = "Although their methods were often brutal, Farrel's followers believed in the preservation of justice and virtue."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e11bf79b-a951-4d0c-acdf-d8ba5290a648.jpg?1783947921"
    }
}
