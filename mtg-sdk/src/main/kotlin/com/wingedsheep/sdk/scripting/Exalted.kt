package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exalted (CR 702.83) as pure data — the triggered ability every exalted permanent has and none
 * of them prints as a separate line:
 *
 * > "Exalted" means "Whenever a creature you control attacks alone, that creature gets +1/+1 until
 * > end of turn." (CR 702.83a)
 *
 * The engine derives the trigger from the projected [Keyword.EXALTED], the same shape as bushido
 * and renown, with one trigger per *instance* — each instance is its own triggered ability, so
 * each resolves separately. Instances come from two places —
 *
 * - **printed** — `keywords(Keyword.EXALTED)` on the card, lost with all abilities;
 * - **exalted counters** — each [com.wingedsheep.sdk.core.CounterType.EXALTED] is its own instance
 *   (CR 122.1b; Emissary of Soulfire's ruling: "a creature with multiple exalted counters will have
 *   that many instances of exalted"), and survives "loses all abilities" like every keyword counter.
 *
 * A static grant ("other creatures you control have exalted") floats the bare keyword, which
 * projection can't count, so a granted-only permanent has exactly one instance.
 *
 * "Attacks alone" is [AttackPredicate.Alone]: the only creature declared as an attacker that combat
 * (CR 506.5), so a creature that enters attacking later never re-triggers or un-triggers it.
 */
object Exalted {

    /** Ability id prefix; the instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "exalted"

    /** CR 702.83a — the exalted trigger for one instance. [instance] only distinguishes ids. */
    fun trigger(instance: Int = 0): TriggeredAbility {
        val spec = Triggers.a(GameObjectFilter.Creature.youControl()).attacks(setOf(AttackPredicate.Alone))
        return TriggeredAbility(
            id = AbilityId(if (instance == 0) ABILITY_ID_PREFIX else "${ABILITY_ID_PREFIX}_$instance"),
            trigger = spec.event,
            binding = spec.binding,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = ModifyStatsEffect(1, 1, EffectTarget.TriggeringEntity),
            descriptionOverride = "Exalted",
        )
    }
}
