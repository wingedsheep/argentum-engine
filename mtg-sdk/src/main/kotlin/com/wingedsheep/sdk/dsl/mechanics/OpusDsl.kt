package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withId
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** "Opus" pays off at five or more mana spent on the triggering spell. */
private const val OPUS_THRESHOLD = 5

/**
 * Add Opus (Secrets of Strixhaven).
 *
 * "Opus — Whenever you cast an instant or sorcery spell, <base>. If five or more mana was spent to
 * cast that spell, <bonus> [instead]."
 *
 * **Opus is an ability word** — flavor only, no rules meaning of its own (CR 207.2c), so it adds no
 * keyword. The whole mechanic is one spell-cast triggered ability whose 5+ mana tier is gated by a
 * [Compare] of [ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL] `>= 5` — the mana spent on the
 * *triggering* spell (distinct from [DynamicAmount.TotalManaSpent], which reads the resolving
 * object's own cast).
 *
 * The set's Opus cards come in two shapes; pick exactly one bonus setter:
 *  - [insteadIfFiveOrMore] — the bonus **replaces** [effect] when 5+ mana was spent ("… mills three
 *    cards. If five or more mana was spent to cast that spell, that player mills ten cards instead.").
 *    Lowers to `ConditionalEffect(5+ → bonus, otherwise → base)`.
 *  - [alsoIfFiveOrMore] — the bonus runs **in addition** to [effect] ("… gets +1/+1 until end of
 *    turn. If five or more mana was spent to cast that spell, this creature also gains double
 *    strike …"). Lowers to `base then ConditionalEffect(5+ → bonus)`.
 *
 * Declare a [target] inside the block (like `triggeredAbility { }`) and reference the returned
 * handle from *both* the base and bonus effects so the single chosen target carries across the tier
 * (Exhibition Tidecaller's "target player mills three … mills ten instead").
 */
fun CardBuilder.opus(init: OpusBuilder.() -> Unit) {
    triggeredAbilities.add(OpusBuilder().apply(init).build())
}

@CardDsl
class OpusBuilder {
    /** The base effect — happens whenever you cast an instant or sorcery spell. */
    var effect: Effect? = null

    /** Bonus that **replaces** [effect] when 5+ mana was spent (rendered "… <bonus> instead"). */
    var insteadIfFiveOrMore: Effect? = null

    /** Bonus that runs **in addition** to [effect] when 5+ mana was spent. */
    var alsoIfFiveOrMore: Effect? = null

    /**
     * Optional full override of the rendered ability text (the entire "Opus — Whenever you cast …"
     * string). Leave null to auto-compose it from the base/bonus effect descriptions.
     */
    var description: String? = null

    private val namedTargets = mutableListOf<Pair<String, TargetRequirement>>()

    /**
     * Declare a target for this Opus ability and get a handle to reference from the base and bonus
     * effects (so both tiers act on the same chosen object). Mirrors `triggeredAbility { target() }`.
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    internal fun build(): TriggeredAbility {
        val base = requireNotNull(effect) { "opus { } requires a base `effect`" }
        val instead = insteadIfFiveOrMore
        val also = alsoIfFiveOrMore
        require((instead == null) != (also == null)) {
            "opus { } needs exactly one of `insteadIfFiveOrMore` or `alsoIfFiveOrMore`"
        }
        val bonus = instead ?: also!!
        val replaces = instead != null

        val fiveOrMoreManaSpent = Compare(
            left = DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(OPUS_THRESHOLD),
        )
        val combined = if (replaces) {
            ConditionalEffect(condition = fiveOrMoreManaSpent, effect = bonus, elseEffect = base)
        } else {
            base then ConditionalEffect(condition = fiveOrMoreManaSpent, effect = bonus)
        }

        val targets = namedTargets.map { it.second }
        val trigger = Triggers.YouCastInstantOrSorcery
        return TriggeredAbility.create(
            trigger = trigger.event,
            binding = trigger.binding,
            effect = combined,
            targetRequirement = targets.firstOrNull(),
            additionalTargetRequirements = if (targets.size > 1) targets.drop(1) else emptyList(),
            descriptionOverride = description ?: renderText(base, bonus, replaces),
        )
    }

    private fun renderText(base: Effect, bonus: Effect, replaces: Boolean): String {
        val insteadSuffix = if (replaces) " instead" else ""
        return "Opus — Whenever you cast an instant or sorcery spell, ${clause(base)}. " +
            "If five or more mana was spent to cast that spell, ${clause(bonus)}$insteadSuffix."
    }

    /** Lowercase the leading char and drop a trailing period so effect text reads inline. */
    private fun clause(effect: Effect): String =
        effect.description.trimEnd().trimEnd('.').replaceFirstChar { it.lowercase() }
}
