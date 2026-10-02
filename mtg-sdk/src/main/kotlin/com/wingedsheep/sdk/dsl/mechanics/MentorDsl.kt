package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/** The printed Mentor reminder text (Ravnica Allegiance / Modern Horizons 3 wording). */
private const val MENTOR_REMINDER =
    "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature " +
        "with lesser power.)"

/**
 * The single triggered ability that *is* Mentor (CR 702.134a): "Whenever this creature attacks, put
 * a +1/+1 counter on target attacking creature with lesser power."
 *
 * "Lesser power" is `powerLessThanEntity(EffectTarget.Self)`, compared as the trigger is put on the
 * stack and again as it resolves (CR 608.2b). If the mentor creature has left the battlefield by
 * then, the comparison uses its power as it last existed there — the predicate reads the source's
 * last-known information, so a mentor creature killed in response still mentors a creature smaller
 * than it was.
 *
 * Exposed as a standalone builder so an Aura or Equipment can grant the identical ability —
 * Nyxborn Unicorn's "Enchanted creature … has mentor" is `GrantKeyword(Keyword.MENTOR)` for the
 * badge plus `GrantTriggeredAbility(mentorTriggeredAbility())` for the behavior. A granted copy's
 * `Self` is the creature it is granted to. Each instance is its own `TriggeredAbility`, so multiple
 * instances trigger separately (CR 702.134b).
 */
fun mentorTriggeredAbility(): TriggeredAbility =
    TriggeredAbility.create(
        trigger = Triggers.self.attacks(),
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        targetRequirement = TargetObject(
            filter = TargetFilter(GameObjectFilter.Creature.attacking().powerLessThanEntity(EffectTarget.Self))
        ),
        descriptionOverride = MENTOR_REMINDER,
    )

/**
 * Add Mentor (CR 702.134, Ravnica Allegiance) — keyword + the triggered ability.
 *
 * The keyword is display-only; the behavior is [mentorTriggeredAbility]. Calling this twice
 * installs two independent triggers (CR 702.134b).
 */
fun CardBuilder.mentor() {
    keywordSet.add(Keyword.MENTOR)
    triggeredAbilities.add(mentorTriggeredAbility())
}
