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
 * Initiates of the Ebon Hand
 * {B}
 * Creature — Cleric
 * 1/1
 * {1}: Add {B}. If this ability has been activated four or more times this turn, sacrifice this
 * creature at the beginning of the next end step.
 *
 * The burnout clause reads a tally rather than imposing a limit, so the ability opts into
 * per-turn activation bookkeeping (`trackActivations`) and reads it back with
 * [Conditions.ThisAbilityActivatedThisTurnAtLeast]. The count includes the activation that is
 * resolving, so the fourth one schedules the sacrifice; further activations schedule it again,
 * which is harmless — the creature is sacrificed once at the next end step either way.
 */
val InitiatesOfTheEbonHand = card("Initiates of the Ebon Hand") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cleric"
    oracleText = "{1}: Add {B}. If this ability has been activated four or more times this " +
        "turn, sacrifice this creature at the beginning of the next end step."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        trackActivations = true
        effect = Effects.AddMana(Color.BLACK).then(
            ConditionalEffect(
                condition = Conditions.ThisAbilityActivatedThisTurnAtLeast(4),
                effect = CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = SacrificeSelfEffect,
                )
            )
        )
        description = "{1}: Add {B}. If this ability has been activated four or more times this turn, sacrifice this creature at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39a"
        artist = "Heather Hudson"
        flavorText = "\"We are no longer Nature's children, but her masters . . . .\"\n—Oath of the Ebon Hand"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5be87527-3b8f-4529-afdb-a61ad4e787e1.jpg?1783947902"
    }
}
