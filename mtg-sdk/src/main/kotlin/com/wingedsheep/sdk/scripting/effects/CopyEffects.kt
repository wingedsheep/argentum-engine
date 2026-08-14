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
 *   "becomes a copy of that creature until end of turn"). [Duration.UntilNextEndStep] reverts on
 *   entry to the next end step (Niko, Light of Hope), and [Duration.UntilYourNextTurn] reverts
 *   after the untap step of the effect's controller's next turn (Absorbing Man, Taskmaster —
 *   "until your next turn, this becomes a copy of …"). Any other duration falls back to permanent.
 * @property excludeTarget When true, the copy source [target] itself is excluded from the set of
 *   permanents that become copies — for "each **other** creature you control becomes a copy of
 *   that creature" wordings, where the target keeps its own identity (and any counter just placed
 *   on it).
 * @property affected When non-null, the set of permanents that become copies is exactly the single
 *   permanent this [EffectTarget] resolves to (e.g. another `ContextTarget`), rather than a
 *   battlefield scan of [filter]. This models "target permanent A becomes a copy of target
 *   permanent B" (Fleeting Reflection: "target creature you control … becomes a copy of up to one
 *   other target creature"). When set, [filter] / [excludeTarget] are ignored. If the [affected]
 *   target resolves to nothing the effect is a no-op.
 * @property sourceFromAnyZone When true, the copy *source* ([target]) does not have to be on the
 *   battlefield — its copiable characteristics are read from wherever it currently is (e.g. a card
 *   in exile). The affected permanents still have to be on the battlefield. Models "become a copy
 *   of that card" off a card just put into exile — Lazav, Familiar Stranger: "you may exile a card
 *   from a graveyard. If a creature card was exiled this way, you may have Lazav become a copy of
 *   that card until end of turn."
 * @property exceptions The "except …" modifications this copy effect specifies (CR 707.9) — name,
 *   added/removed types, added keywords, base P/T, colors. The shared [CopyExceptions] vocabulary,
 *   applied by the same engine helper the token-copy path uses, so "except it isn't legendary"
 *   (Shuri, Wakandan Inventor) and "except his name is Absorbing Man and he's a legendary 4/4
 *   Human Villain creature in addition to his other types" mean the same thing on both paths.
 *   Exceptions ride the copy's base `CardComponent`, so they persist exactly as long as the copy
 *   does and are themselves copiable (CR 707.2).
 * @property retainActivatingAbility The "and this ability" half of the same exception: the copy
 *   keeps the very activated ability that created it. The copy replaces the permanent's
 *   `CardComponent` wholesale, so the printed ability would otherwise be gone with the rest of the
 *   original card — this re-grants it through the durable granted-activated-ability record
 *   (`Duration.Permanent`), which is keyed by entity and therefore survives further copies. The
 *   grant is idempotent: activating the ability again off a copy re-reads the already-granted
 *   instance rather than stacking a second one. No-op when the effect isn't resolving from an
 *   activated ability. Not a characteristic, so it stays here rather than on [CopyExceptions].
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
    val affected: EffectTarget? = null,
    val sourceFromAnyZone: Boolean = false,
    val exceptions: CopyExceptions = CopyExceptions.None,
    val retainActivatingAbility: Boolean = false,
) : Effect {
    override val description: String = run {
        val durationSuffix = when (duration) {
            Duration.EndOfTurn -> " until end of turn"
            Duration.UntilNextEndStep -> " until the next end step"
            Duration.UntilYourNextTurn -> " until your next turn"
            else -> ""
        }
        val clauses = exceptions.clauses() +
            (if (retainActivatingAbility) listOf("it has this ability") else emptyList())
        val exceptSuffix =
            if (clauses.isEmpty()) "" else ", except ${clauses.joinToString(" and ")}"
        if (affected != null) {
            "${affected.description} becomes a copy of ${target.description}$durationSuffix$exceptSuffix"
        } else {
            "Each ${if (excludeTarget) "other " else ""}${filter.baseFilter.description} becomes a copy of ${target.description}$durationSuffix$exceptSuffix"
        }
    }

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

/**
 * Copy **every** card in the pipeline collection [from] and store all the copies' entity ids in the
 * collection [storeAs]. The collection-wide sibling of [CopyCardIntoCollectionEffect] — for "copy
 * them" over a set of cards rather than one ([CopyCardIntoCollectionEffect] overwrites its
 * collection, so it can't accumulate across a `ForEach`).
 *
 * Per Rule 707.12 each copy is created in the **same zone its original currently occupies** (under
 * the effect's controller), keeping the original's copiable characteristics and tagged as a
 * stack-style copy. So gather/exile the originals first, then copy: a copy of an exiled card is
 * created in exile, ready for [CastAnyNumberFromCollectionWithoutPayingCostEffect] to cast.
 *
 *     // The Tale of Tamiyo IV — exile the targets, copy them, then may cast any number of copies
 *     CompositeEffect(listOf(
 *         ForEachTargetEffect(listOf(MoveToZoneEffect(ContextTarget(0), Zone.EXILE))),
 *         GatherCardsEffect(CardSource.ChosenTargets, storeAs = "exiled"),
 *         CopyCollectionIntoCollectionEffect(from = "exiled", storeAs = "copies"),
 *         CastAnyNumberFromCollectionWithoutPayingCostEffect(from = "copies", payManaCost = true),
 *     ))
 *
 * Copies that are never cast are swept up by the Rule 707.10a state-based action (a copy of a card
 * outside the stack/battlefield ceases to exist), so no explicit cleanup is needed.
 *
 * @property from Pipeline collection of the cards to copy.
 * @property storeAs Pipeline collection under which the new copies' entity ids are stored.
 */
@SerialName("CopyCollectionIntoCollection")
@Serializable
data class CopyCollectionIntoCollectionEffect(
    val from: String,
    val storeAs: String,
) : Effect {
    override val description: String = "Copy those cards"
}

/**
 * The [affected] permanent becomes a copy of a creature card exiled with the effect's source
 * (read from the source's linked exile — see `CardSource.FromLinkedExile`), for as long as the
 * source remains attached to it ([com.wingedsheep.sdk.scripting.Duration.WhileSourceAttachedToAffected]).
 *
 * Assimilation Aegis: "Whenever this Equipment becomes attached to a creature, for as long as this
 * Equipment remains attached to it, that creature becomes a copy of a creature card exiled with
 * this Equipment." [affected] is normally
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget.AttachedToTriggeringPermanent] (the creature
 * the Equipment just attached to).
 *
 * The copy uses only the exiled card's copiable characteristics (CR 707.2). The executor bakes the
 * copy into the affected permanent's [com.wingedsheep.sdk.scripting...] CardComponent and tags it so
 * a state-based check reverts it the moment the source detaches, the source leaves, or the affected
 * permanent leaves (the printed "for as long as … attached" duration). If the source's linked exile
 * holds no creature card, the effect is a no-op.
 */
@SerialName("BecomeCopyOfLinkedExile")
@Serializable
data class BecomeCopyOfLinkedExileEffect(
    val affected: EffectTarget = EffectTarget.AttachedToTriggeringPermanent,
) : Effect {
    override val description: String =
        "${affected.description} becomes a copy of a creature card exiled with this, " +
            "for as long as this remains attached to it"
}
