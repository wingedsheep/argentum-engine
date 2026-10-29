package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.BecomeRenownedEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Renown N (CR 702.112) as pure data — the triggered ability every renown creature has and none of
 * them prints as a separate line.
 *
 * A card carrying `Renown 1` shows one keyword line plus its reminder text; CR 702.112a gives it
 * one triggered ability:
 *
 * > "Renown N" means "When this creature deals combat damage to a player, if it isn't renowned,
 * > put N +1/+1 counters on it and it becomes renowned."
 *
 * Three details of that sentence are load-bearing and each maps to one part of
 * [combatDamageTrigger]:
 *
 *  - **"if it isn't renowned" is an intervening-`if`**, not a resolution-time check. CR 603.4 has
 *    it tested both as the ability would trigger and again as it resolves, which is exactly what
 *    CR 702.112c relies on: a creature with two instances of renown puts *two* abilities on the
 *    stack, the first to resolve makes it renowned, and the second then does nothing because its
 *    intervening-`if` no longer holds. Modelling the check as a
 *    [com.wingedsheep.sdk.dsl.Effects.If] inside the effect would get the
 *    same end result for the common case but would let the ability trigger at all while renowned,
 *    which the rules do not.
 *  - **The counters and the designation are one effect**, applied in printed order — the counters
 *    first, then [BecomeRenownedEffect]. Reversing them would make the second instance's
 *    intervening-`if` fail *before* the first instance's counters landed.
 *  - **"renowned" is a designation, not an ability and not a copiable value** (CR 702.112b). It
 *    lives in the engine's `RenownedComponent`, is read back through
 *    [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsRenowned], and lasts until the
 *    permanent leaves the battlefield — a copy of a renowned creature is not itself renowned.
 *
 * **Printed and granted renown derive separately.** A printed `Renown N` is one of the "read N off
 * the printed [KeywordAbility.Numeric]" family, the same shape as [Vanishing] and [Fabricate]: the
 * engine gates the derivation on the *projected* bare `RENOWN` keyword — so a creature that has
 * lost all abilities stops being renown-eligible — and reads N from the printed keyword ability.
 * A *granted* renown (Aragorn, Hornburg Hero: "Attacking creatures you control have first strike
 * and renown 1") floats its own N as the projected keyword [grantedKeyword] — `RENOWN_<n>`, via
 * `GrantKeyword(KeywordAbility.grantedRenown(n), …)` — so it needs no printed source. Unlike
 * granted bushido or toxic, projection never sums renown grants: each grant is a separate
 * instance (CR 702.112c), and projection tallies identical ones so two "have renown 1" grants
 * still derive two triggers.
 */
object Renown {

    /** Ability id prefix; the printed instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "renown"

    /** The projected keyword prefix a granted renown floats under; see [grantedKeyword]. */
    private val GRANTED_PREFIX = "${Keyword.RENOWN.name}_"

    /**
     * The projected keyword string a *granted* "renown [n]" floats — `RENOWN_<n>`. Cards spell it
     * [KeywordAbility.grantedRenown].
     */
    fun grantedKeyword(n: Int): String = "$GRANTED_PREFIX$n"

    /** The N of a projected granted-renown keyword string, or null when [keyword] isn't one. */
    fun grantedN(keyword: String): Int? =
        if (keyword.startsWith(GRANTED_PREFIX)) keyword.substring(GRANTED_PREFIX.length).toIntOrNull()?.takeIf { it > 0 }
        else null

    /** "it isn't renowned" — the CR 702.112a intervening-`if` gate. */
    private val isNotRenowned = NotCondition(
        EntityMatches(EffectTarget.Self, GameObjectFilter.Any.renowned())
    )

    /**
     * CR 702.112a — the renown trigger for a single printed or granted `Renown [n]`.
     *
     * @param n the N of that instance.
     * @param instance the zero-based index of this instance among the creature's printed renown
     *   abilities — or, when [granted], among its granted ones. It only distinguishes the
     *   [AbilityId]s, so that a creature with two instances (CR 702.112c) puts two distinct
     *   abilities on the stack rather than two copies of one id.
     * @param granted whether this instance comes from a granted `RENOWN_<n>` rather than the
     *   printed keyword; granted ids live in their own namespace so they never collide with
     *   printed ones.
     */
    fun combatDamageTrigger(n: Int, instance: Int = 0, granted: Boolean = false): TriggeredAbility {
        val counterWord = if (n == 1) "counter" else "counters"
        return TriggeredAbility(
            id = AbilityId(
                when {
                    granted -> "${ABILITY_ID_PREFIX}_granted_$instance"
                    instance == 0 -> ABILITY_ID_PREFIX
                    else -> "${ABILITY_ID_PREFIX}_$instance"
                }
            ),
            trigger = EventPattern.DealsDamageEvent(
                damageType = DamageType.Combat,
                recipient = Recipient.AnyPlayer,
            ),
            binding = TriggerBinding.SELF,
            activeZones = setOf(Zone.BATTLEFIELD),
            interveningIf = isNotRenowned,
            effect = CompositeEffect(
                effects = listOf(
                    AddCountersEffect(CounterType.PLUS_ONE_PLUS_ONE, n, EffectTarget.Self),
                    BecomeRenownedEffect(EffectTarget.Self),
                ),
                descriptionOverride = "put $n +1/+1 $counterWord on this creature and it becomes " +
                    "renowned",
            ),
            descriptionOverride = "When this creature deals combat damage to a player, if it " +
                "isn't renowned, put $n +1/+1 $counterWord on it and it becomes renowned.",
        )
    }

    /**
     * The N of every printed `Renown N` on a card, one entry per instance.
     *
     * Not summed, unlike [Vanishing.printedCount]: CR 702.112c makes each instance of renown
     * trigger separately, so a creature with renown 1 and renown 2 gets two abilities on the
     * stack — and because the first to resolve makes it renowned, the second is then inert. That
     * is a materially different outcome from one fused renown 3, which is why the instances stay
     * apart.
     */
    fun printedCounts(cardDef: CardDefinition): List<Int> =
        cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.RENOWN }
            .map { it.n }
            .filter { it > 0 }
}
