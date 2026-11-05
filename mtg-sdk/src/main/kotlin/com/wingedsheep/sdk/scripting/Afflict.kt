package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Afflict N (CR 702.130) as pure data — the triggered ability every afflict creature has and none
 * of them prints as a separate line:
 *
 * > "Afflict N" means "Whenever this creature becomes blocked, defending player loses N life."
 * > (CR 702.130a)
 *
 * The engine derives the trigger from the keyword, the same shape as [Bushido] and [Rampage]: the
 * *gate* is the projected bare `AFFLICT` keyword (a creature that has lost all abilities has no
 * afflict), and there is one trigger per printed [KeywordAbility.Numeric] — CR 702.130b: *"If a
 * creature has multiple instances of afflict, each triggers separately."*
 *
 * "Defending player" is `Player.DefendingPlayer`, which keeps resolving to the player the creature
 * attacked even if it is removed from combat before the trigger resolves.
 */
object Afflict {

    /** Ability id prefix; the printed instance's index is appended so multiple instances differ. */
    private const val ABILITY_ID_PREFIX = "afflict"

    /**
     * CR 702.130a — the afflict trigger for one instance of `Afflict [n]`. [instance] only
     * distinguishes the [AbilityId]s of multiple instances; [granted] keeps a granted trigger's id
     * apart from every printed one (the engine's numeric-keyword derivation shares one signature).
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
            // The unfiltered "becomes blocked" trigger fires once however many creatures block (CR 509.3c).
            trigger = EventPattern.BecomesBlockedEvent(filter = null),
            binding = TriggerBinding.SELF,
            activeZones = setOf(Zone.BATTLEFIELD),
            effect = Effects.LoseLife(n, EffectTarget.PlayerRef(Player.DefendingPlayer)),
            descriptionOverride = "Afflict $n",
        )

    /**
     * The N of every printed `Afflict N` on [cardDef], in printed order — a list rather than a sum,
     * because each instance triggers separately (CR 702.130b).
     */
    fun printedCounts(cardDef: CardDefinition): List<Int> =
        cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.AFFLICT }
            .map { it.n }
}
