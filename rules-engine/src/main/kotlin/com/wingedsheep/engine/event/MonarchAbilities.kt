package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The two inherent triggered abilities of being the monarch (CR 725.2).
 *
 * *"At the beginning of the monarch's end step, that player draws a card"* and *"Whenever a creature
 * deals combat damage to the monarch, its controller becomes the monarch."*
 *
 * The rule gives them **no source** and makes them controlled by whoever was the monarch when they
 * triggered — the same shape as the inherent speed ability, so they are modelled the same way (see
 * [SpeedAbilities]): ordinary [TriggeredAbility]s whose `sourceId` *is* the monarch's player entity,
 * synthesized by [TriggerDetector] for `GameState.monarch` only. `Player.You` in their patterns then
 * reads the monarch, and both go on the stack like any other trigger, so they can be responded to.
 *
 * The [AbilityId]s are stable constants: pending triggers are keyed by ability identity, so the many
 * copies the detector hands out must compare equal.
 */
object MonarchAbilities {

    val END_STEP_DRAW_ID: AbilityId = AbilityId("inherent_monarch_end_step_draw")
    val COMBAT_DAMAGE_STEAL_ID: AbilityId = AbilityId("inherent_monarch_combat_damage_steal")

    /** "At the beginning of the monarch's end step, that player draws a card." */
    val endStepDraw: TriggeredAbility = TriggeredAbility(
        id = END_STEP_DRAW_ID,
        trigger = EventPattern.StepEvent(Step.END, Player.You),
        binding = TriggerBinding.ANY,
        effect = Effects.DrawCards(1, EffectTarget.Controller),
        descriptionOverride = "At the beginning of the monarch's end step, that player draws a card."
    )

    /**
     * "Whenever a creature deals combat damage to the monarch, its controller becomes the monarch."
     *
     * One trigger per damaging creature (not batched): two players' creatures connecting at once
     * give the monarch two triggers to order, and the last to resolve keeps the crown. "Its
     * controller" is read on resolution with last-known information, so a creature that died in the
     * same damage step still hands the title to the player who controlled it.
     */
    val combatDamageSteal: TriggeredAbility = Triggers.a(GameObjectFilter.Creature)
        .dealsCombatDamage(Recipient.You)
        .let { spec ->
            TriggeredAbility(
                id = COMBAT_DAMAGE_STEAL_ID,
                trigger = spec.event,
                binding = spec.binding,
                effect = Effects.BecomeMonarch(EffectTarget.ControllerOfTriggeringEntity),
                descriptionOverride = "Whenever a creature deals combat damage to the monarch, its " +
                    "controller becomes the monarch."
            )
        }
}
