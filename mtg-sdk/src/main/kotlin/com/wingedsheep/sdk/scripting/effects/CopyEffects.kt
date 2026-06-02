package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Each permanent matching [filter] becomes a copy of the targeted permanent.
 *
 * Used by Mirrorform and similar group-copy effects. At resolution the executor
 * replaces each matching permanent's `CardComponent` with a copy of the target's
 * (per Rule 707 — copiable values only; counters, tapped state, attached auras/
 * equipment, and non-copy effects on the target are ignored). A `CopyOfComponent`
 * is attached to track the original identity.
 *
 * @property target The permanent whose characteristics are copied (usually `ContextTarget(0)`).
 * @property filter Which permanents become copies (defaults to all nonland permanents you control).
 * @property duration How long the copy persists. [Duration.Permanent] (default) bakes the copy
 *   into base state for good (Mirrorform). [Duration.EndOfTurn] makes a temporary copy that the
 *   end-of-turn cleanup reverts to each permanent's pre-copy identity (Naga Fleshcrafter's renew —
 *   "becomes a copy of that creature until end of turn"). Only `Permanent` and `EndOfTurn` are
 *   supported; other durations fall back to permanent.
 * @property excludeTarget When true, the copy source [target] itself is excluded from the set of
 *   permanents that become copies — for "each **other** creature you control becomes a copy of
 *   that creature" wordings, where the target keeps its own identity (and any counter just placed
 *   on it).
 */
@SerialName("EachPermanentBecomesCopyOfTarget")
@Serializable
data class EachPermanentBecomesCopyOfTargetEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val filter: GroupFilter = GroupFilter(
        com.wingedsheep.sdk.scripting.GameObjectFilter.NonlandPermanent.youControl()
    ),
    val duration: Duration = Duration.Permanent,
    val excludeTarget: Boolean = false,
) : Effect {
    override val description: String =
        "Each ${if (excludeTarget) "other " else ""}${filter.baseFilter.description} becomes a copy of ${target.description}" +
            if (duration == Duration.EndOfTurn) " until end of turn" else ""

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Copy a single card referenced by [source] and store the copy's entity id in the pipeline
 * collection [storeAs].
 *
 * Per Rule 707.12 the copy is created in the **same zone the original card currently is in**
 * (under the effect's controller). The copy keeps the original's copiable characteristics
 * (name, mana cost, type line, colors, P/T, keywords, spell effect) via a cloned
 * `CardComponent`, and is tagged as a stack-style copy (a `CopyOfComponent` with no
 * pre-copy snapshot) so that — once cast — it becomes a token if it's a permanent spell and
 * ceases to exist if it's an instant/sorcery (Rule 707.10).
 *
 * This is the zone-side half of the "copy a card, then cast the copy" pattern: pair it with
 * [CastFromCollectionWithoutPayingCostEffect] (wrapped in `MayEffect` for "you may cast")
 * reading the same [storeAs] collection. A copy that is never cast is removed by the
 * Rule 707.10a state-based action (a copy of a card outside the stack/battlefield ceases to
 * exist), so no explicit cleanup step is needed.
 *
 *     // Shiko, Paragon of the Way — exile a graveyard card, copy it, then may cast the copy
 *     CompositeEffect(listOf(
 *         MoveToZoneEffect(target, Zone.EXILE),
 *         CopyCardIntoCollectionEffect(target, storeAs = "copy"),
 *         MayEffect(CastFromCollectionWithoutPayingCostEffect("copy")),
 *     ))
 *
 * @property source The card to copy (e.g. `ContextTarget(0)` for a targeted graveyard card).
 * @property storeAs Pipeline collection key under which the new copy's entity id is stored.
 */
@SerialName("CopyCardIntoCollection")
@Serializable
data class CopyCardIntoCollectionEffect(
    val source: EffectTarget = EffectTarget.ContextTarget(0),
    val storeAs: String,
) : Effect {
    override val description: String = "Copy ${source.description}"
}
