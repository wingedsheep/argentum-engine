package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bushido N (CR 702.45) as pure data — the triggered ability every bushido creature has and none
 * of them prints as a separate line:
 *
 * > "Bushido N" means "Whenever this creature blocks or becomes blocked, it gets +N/+N until end
 * > of turn." (CR 702.45a)
 *
 * The engine derives the trigger from the keyword, the same shape as fabricate and renown: the
 * *gate* is the projected keyword (a creature that has lost all abilities has no bushido), and the
 * *N* comes from two places —
 *
 * - **printed** — one trigger per printed [KeywordAbility.Numeric] (CR 702.45b: *"If a creature
 *   has multiple instances of bushido, each triggers separately."*), gated on the projected bare
 *   `BUSHIDO` keyword;
 * - **granted** — "gains bushido N" ([com.wingedsheep.sdk.dsl.Effects.GrantBushido], Sensei
 *   Golden-Tail) floats the projected keyword [grantedKeyword] `BUSHIDO_<n>`, the same
 *   `<KEYWORD>_<n>` encoding granted toxic uses. Projection sums repeated grants into one
 *   `BUSHIDO_<total>`, so the granted instances trigger once for their total — the same +N/+N by
 *   the end of the combat step, one stack object instead of several.
 *
 * `EntityNumericProperty.KeywordValue(BUSHIDO)` reads the same two sources, so "for each point of
 * bushido it has" (Takeno, Samurai General) counts a granted bushido too.
 */
object Bushido {

    /** Ability id prefix; the printed instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "bushido"

    /** The projected keyword a granted "bushido [n]" floats — `BUSHIDO_<n>`. */
    fun grantedKeyword(n: Int): String = "${Keyword.BUSHIDO.name}_$n"

    /**
     * CR 702.45a — the bushido trigger for one instance of `Bushido [n]`. [instance] only
     * distinguishes the [AbilityId]s of multiple instances; [granted] keeps the granted trigger's
     * id apart from every printed one.
     */
    fun trigger(n: Int, instance: Int = 0, granted: Boolean = false): TriggeredAbility =
        TriggeredAbility(
            id = AbilityId(
                when {
                    granted -> "${ABILITY_ID_PREFIX}_granted"
                    instance == 0 -> ABILITY_ID_PREFIX
                    else -> "${ABILITY_ID_PREFIX}_$instance"
                }
            ),
            // Partner-less "blocks or becomes blocked": one trigger per combat, however many
            // creatures it blocks or is blocked by.
            trigger = EventPattern.BlocksOrBecomesBlockedByEvent(partnerFilter = null, oncePerCombat = true),
            binding = TriggerBinding.SELF,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = ModifyStatsEffect(n, n, EffectTarget.Self),
            descriptionOverride = "Bushido $n",
        )

    /**
     * The N of every printed `Bushido N` on [cardDef], in printed order — a list rather than a
     * sum, because each instance triggers separately (CR 702.45b).
     */
    fun printedCounts(cardDef: CardDefinition): List<Int> =
        cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.BUSHIDO }
            .map { it.n }
}
