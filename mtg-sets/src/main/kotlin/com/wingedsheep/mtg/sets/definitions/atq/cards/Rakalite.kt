package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakalite
 * {6}
 * Artifact
 * {2}: Prevent the next 1 damage that would be dealt to any target this turn.
 * Return this artifact to its owner's hand at the beginning of the next end step.
 *
 * Both clauses are a single activated ability: the prevention shield (like Amulet of
 * Kroog) composed with a delayed trigger that bounces Rakalite to its owner's hand at
 * the beginning of the next end step ([CreateDelayedTriggerEffect] at [Step.END]).
 */
val Rakalite = card("Rakalite") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}: Prevent the next 1 damage that would be dealt to any target this turn. " +
        "Return this artifact to its owner's hand at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Mana("{2}")
        val t = target("any target", Targets.Any)
        effect = Effects.Composite(listOf(
            Effects.PreventNextDamage(1, EffectTarget.ContextTarget(0)),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.ReturnToHand(EffectTarget.Self)
            )
        ))
        description = "{2}: Prevent the next 1 damage that would be dealt to any target this turn. " +
            "Return this artifact to its owner's hand at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Christopher Rush"
        flavorText = "Urza was the first to understand that the war would not be lost for lack of power, but for lack of troops."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd7c711-3ff4-4691-914f-242e6737066c.jpg?1562898310"
    }
}
