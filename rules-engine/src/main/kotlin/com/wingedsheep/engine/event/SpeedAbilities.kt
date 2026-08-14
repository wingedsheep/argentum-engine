package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The inherent speed triggered ability (Aetherdrift, CR 702.179d).
 *
 * *"Whenever one or more opponents lose life during your turn, if your speed is less than 4, your
 * speed increases by 1. This ability triggers only once each turn."*
 *
 * The rule gives this ability **no source** and makes it controlled by the player, and every player
 * with 1 or more speed has exactly one. It's modelled as a normal [TriggeredAbility] whose `sourceId`
 * *is* the player entity — which is what makes the rest work for free:
 *
 * - `oncePerTurn` is enforced by the engine's generic
 *   [com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent],
 *   which is keyed on `(sourceId, abilityId)` and stamped when the trigger goes on the stack. Since
 *   the source is the player entity, one stamp per player per turn — and cleanup's reset loop already
 *   walks every entity, players included.
 * - `Player.You` in the intervening-if resolves to the ability's controller, i.e. the player whose
 *   speed it is, in both the fire-time and resolution checks (CR 603.4).
 * - The trigger goes on the stack like any other, so it can be responded to.
 *
 * The [AbilityId] is a stable constant, not generated: the trigger system keys pending triggers and
 * the once-per-turn tracker by ability identity, so it has to be equal across the many times
 * [SpeedTriggerDetection] hands the ability out.
 */
object SpeedAbilities {

    /** Stable identity for the one inherent speed ability every player with speed has. */
    val INHERENT_SPEED_ABILITY_ID: AbilityId = AbilityId("inherent_speed_increase")

    /**
     * Stack/log label for the sourceless inherent ability. The rule gives it no source object
     * (CR 702.179d), so it can't borrow a permanent's name the way granted abilities do.
     */
    const val SOURCE_NAME: String = "Speed"

    /**
     * "Whenever one or more opponents lose life during your turn, if your speed is less than 4, your
     * speed increases by 1. This ability triggers only once each turn."
     *
     * `LifeLossEvent(EachOpponent)` matches life loss by any opponent of the controller from any
     * source, damage included — which is what the rule wants: CR 702.179d says "lose life", and
     * damage causes life loss. The "one or more" batching and the "only once each turn" clause are the
     * same guarantee here, and `oncePerTurn` delivers both (it also collapses two opponents losing
     * life simultaneously into a single fire).
     */
    val inherentSpeedIncrease: TriggeredAbility = TriggeredAbility(
        id = INHERENT_SPEED_ABILITY_ID,
        trigger = EventPattern.LifeLossEvent(Player.EachOpponent),
        binding = TriggerBinding.ANY,
        effect = Effects.IncreaseSpeed(target = EffectTarget.Controller),
        triggerCondition = AllConditions(
            listOf(
                Conditions.IsYourTurn,
                Conditions.SpeedBelowMax()
            )
        ),
        oncePerTurn = true,
        descriptionOverride = "Whenever one or more opponents lose life during your turn, if your " +
            "speed is less than ${Speed.MAX}, your speed increases by 1. This ability triggers " +
            "only once each turn."
    )
}
