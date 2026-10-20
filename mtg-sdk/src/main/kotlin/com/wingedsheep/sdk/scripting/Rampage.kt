package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Rampage N (CR 702.23) as pure data — the triggered ability every rampage creature has and none
 * of them prints as a separate line:
 *
 * > "Rampage N" means "Whenever this creature becomes blocked, it gets +N/+N until end of turn for
 * > each creature blocking it beyond the first." (CR 702.23a)
 *
 * The engine derives the trigger from the keyword, the same shape as [Bushido]: the *gate* is the
 * projected keyword (a creature that has lost all abilities has no rampage), and the *N* comes
 * from two places —
 *
 * - **printed** — one trigger per printed [KeywordAbility.Numeric] (CR 702.23c: *"If a creature
 *   has multiple instances of rampage, each triggers separately."*), gated on the projected bare
 *   `RAMPAGE` keyword;
 * - **granted** — "gains rampage N" ([com.wingedsheep.sdk.dsl.Effects.GrantRampage], Rapid Fire)
 *   floats the projected keyword [grantedKeyword] `RAMPAGE_<n>`. Projection sums repeated grants
 *   into one `RAMPAGE_<total>`, so the granted instances trigger once for their total — the same
 *   +N/+N per extra blocker, one stack object instead of several.
 *
 * The bonus is counted on resolution (CR 702.23b: blockers added or removed after the trigger
 * don't change it once it has resolved), so a creature blocked by just one creature gets +0/+0.
 */
object Rampage {

    /** Ability id prefix; the printed instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "rampage"

    /** The projected keyword a granted "rampage [n]" floats — `RAMPAGE_<n>`. */
    fun grantedKeyword(n: Int): String = "${Keyword.RAMPAGE.name}_$n"

    /**
     * CR 702.23a — the rampage trigger for one instance of `Rampage [n]`. [instance] only
     * distinguishes the [AbilityId]s of multiple instances; [granted] keeps the granted trigger's
     * id apart from every printed one.
     */
    fun trigger(n: Int, instance: Int = 0, granted: Boolean = false): TriggeredAbility {
        val perBlockerBeyondFirst = DynamicAmount.Multiply(
            DynamicAmount.Subtract(
                DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.BlockerCount),
                DynamicAmount.Fixed(1),
            ),
            n,
        )
        return TriggeredAbility(
            id = AbilityId(
                when {
                    granted -> "${ABILITY_ID_PREFIX}_granted"
                    instance == 0 -> ABILITY_ID_PREFIX
                    else -> "${ABILITY_ID_PREFIX}_$instance"
                }
            ),
            // The unfiltered "becomes blocked" trigger fires once however many creatures block.
            trigger = EventPattern.BecomesBlockedEvent(filter = null),
            binding = TriggerBinding.SELF,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = ModifyStatsEffect(perBlockerBeyondFirst, perBlockerBeyondFirst, EffectTarget.Self),
            descriptionOverride = "Rampage $n",
        )
    }

    /**
     * The N of every printed `Rampage N` on [cardDef], in printed order — a list rather than a
     * sum, because each instance triggers separately (CR 702.23c).
     */
    fun printedCounts(cardDef: CardDefinition): List<Int> =
        cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.RAMPAGE }
            .map { it.n }
}
