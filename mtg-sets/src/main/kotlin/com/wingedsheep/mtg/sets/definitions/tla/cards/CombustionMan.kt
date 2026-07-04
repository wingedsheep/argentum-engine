package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Combustion Man
 * {3}{R}{R}
 * Legendary Creature — Human Assassin
 * 4/6
 *
 * Whenever Combustion Man attacks, destroy target permanent unless its controller has
 * Combustion Man deal damage to them equal to his power.
 *
 * Modeling notes:
 * - "Destroy ... unless its controller [accepts an avoidance]" is a pay-or-suffer choice routed to
 *   the TARGET PERMANENT'S controller, not Combustion Man's controller. Composed as a
 *   [ChooseActionEffect] whose `player` is [EffectTarget.TargetController] (the controller of the
 *   chosen permanent). Both options are always feasible, so the targeted permanent's controller is
 *   always asked to choose between:
 *     • taking the avoidance damage (Combustion Man deals damage equal to his power to them), or
 *     • losing the permanent (it is destroyed).
 * - The avoidance is a self-damage instance dealt BY Combustion Man (`damageSource = Self`) to that
 *   player, equal to Combustion Man's CURRENT power via [DynamicAmounts.sourcePower]. Pumping
 *   Combustion Man scales the offered damage.
 */
val CombustionMan = card("Combustion Man") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Assassin"
    power = 4
    toughness = 6
    oracleText = "Whenever Combustion Man attacks, destroy target permanent unless its controller " +
        "has Combustion Man deal damage to them equal to his power."

    triggeredAbility {
        trigger = Triggers.Attacks
        val permanent = target("target permanent", Targets.Permanent)
        effect = ChooseActionEffect(
            choices = listOf(
                EffectChoice(
                    label = "Have Combustion Man deal damage to you equal to his power",
                    effect = Effects.DealDamage(
                        amount = DynamicAmounts.sourcePower(),
                        target = EffectTarget.TargetController,
                        damageSource = EffectTarget.Self
                    )
                ),
                EffectChoice(
                    label = "Let the permanent be destroyed",
                    effect = Effects.Destroy(permanent)
                )
            ),
            // The choice is made by the targeted permanent's controller.
            player = EffectTarget.TargetController
        )
        description = "Whenever Combustion Man attacks, destroy target permanent unless its " +
            "controller has Combustion Man deal damage to them equal to his power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Pisukev"
        flavorText = "\"The Avatar's alive. I want you to find him, and end him.\"\n—Zuko"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86f7399d-6876-4e85-ba34-ff1f97dc144a.jpg?1764120875"
    }
}
