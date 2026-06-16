package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gate to Phyrexia
 * {B}{B}
 * Enchantment
 * Sacrifice a creature: Destroy target artifact. Activate only during your upkeep and only
 * once each turn.
 *
 * Sacrifice-a-creature cost ([Costs.Sacrifice]); destroys a target artifact. The activation
 * window is restricted to the controller's upkeep ([ActivationRestriction.OnlyDuringYourTurn]
 * + [ActivationRestriction.DuringStep] UPKEEP) and capped at one activation per turn
 * ([ActivationRestriction.OncePerTurn]).
 */
val GateToPhyrexia = card("Gate to Phyrexia") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Sacrifice a creature: Destroy target artifact. Activate only during your " +
        "upkeep and only once each turn."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        val artifact = target("target artifact", Targets.Artifact)
        effect = Effects.Destroy(artifact)
        restrictions = listOf(
            ActivationRestriction.OncePerTurn,
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Sandra Everingham"
        flavorText = "\"The warm rain of grease on my face immediately made it clear I had entered Phyrexia.\" —Jarsyl, Diary"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f372950-6693-4838-80ef-8fd9aa3e0349.jpg?1592364259"
    }
}
