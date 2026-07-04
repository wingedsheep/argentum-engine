package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitBendEventEffect
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * The attack-triggered ability that *is* Firebending N (Avatar: The Last Airbender, CR 702.189):
 * "Whenever this creature attacks, add N {R}. Until end of combat, you don't lose this mana as
 * steps and phases end." No separate Firebending handler exists — the behavior lives entirely in
 * this ordinary triggered ability (it uses the stack and can be responded to; it is not a mana
 * ability). The effect is an [AddManaEffect] producing N red mana with [ManaExpiry.END_OF_COMBAT],
 * which the pool keeps through combat and discards once combat ends.
 *
 * Extracted so both the printed [firebending] keyword and the runtime
 * [com.wingedsheep.sdk.dsl.Effects.GrantFirebending] grant install the *identical* behavior — a
 * granted firebending must add combat-duration red mana on attack exactly like the printed one.
 */
fun firebendingAttackTrigger(n: Int): TriggeredAbility =
    TriggeredAbility.create(
        trigger = Triggers.Attacks.event,
        binding = Triggers.Attacks.binding,
        // CR 702.189b: firebending fires "whenever you firebend" when this ability resolves —
        // the red mana and the bend notification resolve together as one triggered ability.
        effect = CompositeEffect(listOf(
            AddManaEffect(Color.RED, n, expiry = ManaExpiry.END_OF_COMBAT),
            EmitBendEventEffect(BendType.FIRE),
        )),
        descriptionOverride = "Whenever this creature attacks, add ${"{R}".repeat(n)}. " +
            "Until end of combat, you don't lose this mana as steps and phases end."
    )

/**
 * Add Firebending N (Avatar: The Last Airbender, CR 702.189) — keyword ability +
 * triggered ability.
 *
 * The keyword ability is display-only (no separate Firebending handler exists); the behavior lives
 * entirely in the attack-triggered ability wired by [firebendingAttackTrigger].
 */
fun CardBuilder.firebending(n: Int) {
    keywordAbilityList.add(KeywordAbility.firebending(n))
    triggeredAbilities.add(firebendingAttackTrigger(n))
}
