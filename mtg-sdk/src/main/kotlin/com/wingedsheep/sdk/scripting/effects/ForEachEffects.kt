package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Unified iteration: ForEachEffect over an IterationSpace
// =============================================================================

/**
 * What a [ForEachEffect] iterates over, and how the body's execution context is bound
 * per iteration. One sealed axis instead of one effect type per iteration source —
 * the same unification move as `Gate`/`GatedEffect` and `CostAtom`.
 *
 * Every space is snapshotted when the effect starts executing (entities destroyed or
 * added mid-iteration don't change the list), and every space is pause-safe: if the
 * body pauses for a decision mid-iteration, the engine resumes the remaining
 * iterations after the decision (one shared continuation frame for all spaces).
 *
 * Each variant documents its per-iteration context binding and whether pipeline
 * `storedCollections` are wiped per iteration (wiping is right for "fresh sub-pipeline
 * per player/target", wrong for "apply to each gathered card").
 */
@Serializable
sealed interface IterationSpace {

    /**
     * Iterate the targets chosen for the spell/ability. Per iteration, the body sees
     * only the current target as `ContextTarget(0)` and a fresh (wiped)
     * `storedCollections`, so a per-target sub-pipeline starts clean (Kaboom!).
     */
    @SerialName("IterationSpace.Targets")
    @Serializable
    data object Targets : IterationSpace

    /**
     * Iterate players matching [players] (e.g. `Player.Each`, `Player.EachOpponent`,
     * `Player.ActivePlayerFirst`). Per iteration, the body's `controllerId` is rebound
     * to the current player (so `Player.You` resolves to them), `opponentId` is
     * recomputed relative to that player, and `storedCollections` are wiped
     * (Winds of Change, Bend or Break).
     */
    @SerialName("IterationSpace.Players")
    @Serializable
    data class Players(val players: Player) : IterationSpace

    /**
     * Iterate the entities in a named pipeline collection. Per iteration, the body runs
     * with `pipeline.iterationTarget` set to the current entity — so a single-target
     * body referencing `EffectTarget.Self` applies to it. The outer `storedCollections`
     * are preserved (the body usually needs the very collection being iterated;
     * Fight or Flight).
     */
    @SerialName("IterationSpace.Collection")
    @Serializable
    data class Collection(val collection: String) : IterationSpace

    /**
     * Iterate battlefield permanents matching [filter], snapshotted before any
     * iteration runs (simultaneous semantics — entities destroyed during iteration
     * stay in the list). Same `iterationTarget` binding as [Collection]; outer
     * `storedCollections` are preserved.
     *
     * @property filter Which entities are affected
     * @property noRegenerate If true, all matched entities are marked "can't be
     *   regenerated" before any iteration executes
     */
    @SerialName("IterationSpace.Group")
    @Serializable
    data class Group(
        val filter: GroupFilter,
        val noRegenerate: Boolean = false
    ) : IterationSpace

    /**
     * Iterate the colors of a referenced entity in canonical WUBRG order, exposing the
     * current color through the chosen-color context — the same channel
     * `ChooseColorThen` feeds — so per-color atoms (`GrantProtectionFromChosenColor`,
     * `GrantHexproofFromChosenColor`, …) compose inside the body.
     *
     * The entity's colors are read from projected state while it is on the battlefield
     * (Layer-5 color changes and Devoid are honored), else from its printed colors as
     * last-known information. A colorless source produces zero iterations (colorless
     * is not a color, CR 105.2). Outer `storedCollections` are preserved.
     */
    @SerialName("IterationSpace.ColorsOf")
    @Serializable
    data class ColorsOf(val source: EntityReference) : IterationSpace

    fun applyTextReplacement(replacer: TextReplacer): IterationSpace = when (this) {
        is Group -> {
            val newFilter = filter.applyTextReplacement(replacer)
            if (newFilter !== filter) copy(filter = newFilter) else this
        }
        else -> this
    }

    companion object {
        /**
         * Well-known [Collection] name under which a batch trigger seeds the entities it captured
         * (the matching members of a `PermanentsEnteredEvent` batch). A payoff iterates them with
         * `ForEachInCollectionEffect(IterationSpace.TRIGGER_CAPTURED_COLLECTION, body)` and a body
         * that uses `EffectTarget.Self` — "for each of them, create a tapped copy of it" (Kambal,
         * Profiteering Mayor). The engine seeds this collection when the triggered ability resolves.
         */
        const val TRIGGER_CAPTURED_COLLECTION = "trigger.captured"
    }
}

/**
 * Run [body] once per item in [space]. The single compiled form behind the
 * `ForEachTargetEffect` / `ForEachPlayerEffect` / `ForEachInCollectionEffect` /
 * `ForEachInGroupEffect` / `ForEachColorOfEffect` lowering facades — one executor,
 * one continuation, every iteration source pause/resume-safe.
 *
 * The body is a single [Effect]; multi-step iterations compose a [CompositeEffect]
 * (the lowering facades do this for `List<Effect>` call sites).
 */
@SerialName("ForEach")
@Serializable
data class ForEachEffect(
    val space: IterationSpace,
    val body: Effect
) : Effect {
    override val description: String = render { it.description }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String =
        render { it.runtimeDescription(resolver) }

    /**
     * Compose the per-space lead-in with the body text, preserving the exact phrasing
     * the five pre-unification effect types produced (these strings surface on the
     * stack and in oracle-ish UI text).
     */
    private fun render(text: (Effect) -> String): String {
        // A plain lowered Composite reads as one clause per sub-effect, each
        // lowercased — matching the former List<Effect> joins. A Composite with a
        // hand-written override (or any other body) is treated as one clause.
        fun bodyClauses(): String =
            if (body is CompositeEffect && body.descriptionOverride == null) {
                body.effects.joinToString(". ") { text(it).replaceFirstChar { c -> c.lowercase() } }
            } else {
                text(body).replaceFirstChar { it.lowercase() }
            }

        return when (space) {
            is IterationSpace.Targets -> "For each target, ${bodyClauses()}"
            is IterationSpace.Players -> "For each player, ${bodyClauses()}"
            is IterationSpace.Collection -> "For each permanent in ${space.collection}, ${bodyClauses()}"
            is IterationSpace.Group -> renderGroupDescription(text(body), space.filter, space.noRegenerate)
            is IterationSpace.ColorsOf -> "for each color of ${space.source.description}, ${text(body)}"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newSpace = space.applyTextReplacement(replacer)
        val newBody = body.applyTextReplacement(replacer)
        return if (newSpace !== space || newBody !== body)
            copy(space = newSpace, body = newBody) else this
    }
}

/**
 * Compose the inner effect's text with the group filter so the result reads
 * naturally on the stack and in oracle text. The inner effect describes a
 * single iteration target as "this creature" (i.e. `EffectTarget.Self`); we
 * rewrite that mention to "each [filter]" so e.g. "Deal X damage to this
 * creature" + `AllCreaturesOpponentsControl` renders as "Deal X damage to
 * each creature an opponent controls" rather than the broken concatenation
 * "Deal X damage to this creature all creatures an opponent controls".
 */
private fun renderGroupDescription(innerText: String, filter: GroupFilter, noRegenerate: Boolean): String {
    val capitalizedInner = innerText.replaceFirstChar { it.uppercase() }
    // Strip the leading "all " from the filter description so we can splice it in
    // after "each".
    val filterNoun = filter.description.removePrefix("all ").trimStart()
    val rewritten = when {
        capitalizedInner.contains(" to this creature") ->
            capitalizedInner.replace(" to this creature", " to each $filterNoun")
        capitalizedInner.endsWith(" this creature") ->
            capitalizedInner.removeSuffix(" this creature") + " each $filterNoun"
        capitalizedInner.contains(" this creature ") ->
            capitalizedInner.replace(" this creature ", " each $filterNoun ")
        else -> "$capitalizedInner ${filter.description.replaceFirstChar { it.lowercase() }}"
    }
    return if (noRegenerate) "$rewritten. They can't be regenerated" else rewritten
}

private fun List<Effect>.asBody(): Effect =
    singleOrNull() ?: CompositeEffect(this)

// =============================================================================
// Lowering facades — the five pre-unification names, kept so card source (and the
// mtgish emitter's rendered DSL) is unchanged. Same precedent as IfYouDoEffect →
// GatedEffect: only the compiled/serialized representation moved to `ForEach`.
// =============================================================================

/**
 * Execute sub-effects once per target in the context. Each target gets a context with
 * only that target as `ContextTarget(0)` plus fresh storedCollections (Kaboom!).
 * Lowers to [ForEachEffect] over [IterationSpace.Targets].
 */
@Suppress("FunctionName")
fun ForEachTargetEffect(effects: List<Effect>): ForEachEffect =
    ForEachEffect(IterationSpace.Targets, effects.asBody())

/**
 * Execute sub-effects once per player matching [players], with `controllerId` rebound
 * per iteration (so `Player.You` resolves to the current player) plus fresh
 * storedCollections (Winds of Change). Lowers to [ForEachEffect] over
 * [IterationSpace.Players].
 */
@Suppress("FunctionName")
fun ForEachPlayerEffect(players: Player, effects: List<Effect>): ForEachEffect =
    ForEachEffect(IterationSpace.Players(players), effects.asBody())

/**
 * Run [effect] once per entity in a named pipeline collection, with
 * `pipeline.iterationTarget` bound so `EffectTarget.Self` applies to the current
 * entity (Fight or Flight). Lowers to [ForEachEffect] over [IterationSpace.Collection].
 */
@Suppress("FunctionName")
fun ForEachInCollectionEffect(collection: String, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.Collection(collection), effect)

/**
 * Apply [effect] to every battlefield entity matching [filter] (snapshotted before any
 * iteration). Lowers to [ForEachEffect] over [IterationSpace.Group].
 */
@Suppress("FunctionName")
fun ForEachInGroupEffect(
    filter: GroupFilter,
    effect: Effect,
    noRegenerate: Boolean = false
): ForEachEffect =
    ForEachEffect(IterationSpace.Group(filter, noRegenerate), effect)

/**
 * Run [effect] once per color of the referenced entity, exposing the current color via
 * the chosen-color context (Éowyn, Fearless Knight). Lowers to [ForEachEffect] over
 * [IterationSpace.ColorsOf].
 */
@Suppress("FunctionName")
fun ForEachColorOfEffect(source: EntityReference, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.ColorsOf(source), effect)
