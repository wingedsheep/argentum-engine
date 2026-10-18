package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.ITERATION_ENTITY_NOUN
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
     * Iterate the targets chosen for **one** target declaration — the requirement whose handle
     * id is [group] — while the spell or ability also has other targets: "target creature you
     * control deals damage equal to its power to **each of X target creatures**" (Spinning Wheel
     * Kick). Per iteration the current target is bound as [EffectTarget.IterationEntity]; the
     * context's targets, named targets and `storedCollections` are left untouched, so the body
     * still reads the other declarations through their handles. Only still-legal targets are
     * visited (CR 608.2b): one that became illegal before resolution is skipped.
     *
     * Closest neighbours: [Targets] rebinds the whole target list to the visited target (the
     * other declarations' handles would still resolve, but it visits every declaration's
     * targets, the creature you control included), and [Collection] iterates a pipeline
     * collection that no step fills with a declaration's targets.
     *
     * @property group The binding id the DSL minted for the declaration (`targets(…)` handles
     *   are `group[0]`, `group[1]`, …) — build it with `Effects.ForEachTargetOf(handles, …)`.
     */
    @SerialName("IterationSpace.TargetsOf")
    @Serializable
    data class TargetsOf(val group: String) : IterationSpace

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
     * with the current entity bound as [EffectTarget.IterationEntity] — so a single-target
     * body referencing it applies to that entity, while `EffectTarget.Self` stays the
     * source. The outer `storedCollections` are preserved (the body usually needs the very
     * collection being iterated; Fight or Flight).
     */
    @SerialName("IterationSpace.Collection")
    @Serializable
    data class Collection(val collection: String) : IterationSpace

    /**
     * Iterate battlefield permanents matching [filter], snapshotted before any
     * iteration runs (simultaneous semantics — entities destroyed during iteration
     * stay in the list). Same [EffectTarget.IterationEntity] binding as [Collection]; outer
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
    data class ColorsOf(val source: EffectTarget.SingleEntity) : IterationSpace

    /**
     * Iterate each **kind of counter** among the battlefield permanents matching [filter] —
     * "for each kind of counter among creatures you control, put a counter of that kind on …"
     * (Exotic Pets). A kind counts once however many matching permanents carry it, and only
     * while at least one counter of it is on one of them. The kinds are snapshotted before
     * the first iteration (in canonical id order, for deterministic event sequencing) and
     * each is exposed through the chosen-counter-kind context, which
     * `AddCountersOfChosenKind(kindFrom = CounterKindSource.ITERATION)` reads. Outer
     * `storedCollections` are preserved, so the body can choose among objects an earlier
     * step created. No matching counters → zero iterations.
     *
     * The counter-kind analogue of [ColorsOf]; [Group] iterates the permanents themselves.
     */
    @SerialName("IterationSpace.CounterKindsAmong")
    @Serializable
    data class CounterKindsAmong(val filter: GroupFilter) : IterationSpace

    /**
     * Iterate each **kind of counter** on one referenced permanent — "for each kind of counter
     * on target permanent, put another counter of that kind on it or remove one from it"
     * (Dramatist's Puppet). The single-object sibling of [CounterKindsAmong]: the kinds are read
     * off [entity] (its current counters, only kinds with at least one counter), snapshotted
     * before the first iteration in canonical id order, and each is exposed through the
     * chosen-counter-kind context exactly as [CounterKindsAmong] exposes it — so
     * `AddCountersOfChosenKind(kindFrom = ITERATION)` / `RemoveCountersOfIteratedKind`, a
     * `{counter}` placeholder in a body prompt or a `ChooseAction` label, all read the visited
     * kind. An unresolvable entity or one without counters → zero iterations.
     */
    @SerialName("IterationSpace.CounterKindsOn")
    @Serializable
    data class CounterKindsOn(val entity: EffectTarget.SingleEntity) : IterationSpace

    fun applyTextReplacement(replacer: TextReplacer): IterationSpace = when (this) {
        is Group -> {
            val newFilter = filter.applyTextReplacement(replacer)
            if (newFilter !== filter) copy(filter = newFilter) else this
        }
        is CounterKindsAmong -> {
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
         * that uses `EffectTarget.IterationEntity` — "for each of them, create a tapped copy of it"
         * (Kambal, Profiteering Mayor). The engine seeds this collection when the triggered ability resolves.
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
    val body: Effect,
    /**
     * Per-iteration collection outputs to append into an outer result collection.
     * The key is the collection written by [body]; the value is the collection
     * published after every iteration completes.
     *
     * Player and target iterations still start with fresh collections. This reducer
     * is the explicit bridge for effects such as "each opponent discards ...; draw
     * for each card discarded this way", where every opponent makes an independent
     * choice but the following effect needs the combined set of moved cards.
     */
    val collectCollections: Map<String, String> = emptyMap()
) : Effect {
    override val description: String = render { it.description }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int?): String =
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
            is IterationSpace.TargetsOf -> {
                val inner = text(body)
                if (inner.contains(ITERATION_ENTITY_NOUN, ignoreCase = true)) {
                    inner.replace(ITERATION_ENTITY_NOUN, "each of those targets", ignoreCase = true)
                        .replaceFirstChar { it.uppercase() }
                } else "For each of those targets, ${bodyClauses()}"
            }
            is IterationSpace.Players -> "For each player, ${bodyClauses()}"
            is IterationSpace.Collection -> "For each permanent in ${space.collection}, ${bodyClauses()}"
            is IterationSpace.Group -> renderGroupDescription(text(body), space.filter, space.noRegenerate)
            is IterationSpace.ColorsOf -> "for each color of ${space.source.description}, ${text(body)}"
            is IterationSpace.CounterKindsAmong ->
                "For each kind of counter among ${space.filter.description.removePrefix("all ").trimStart()}, ${bodyClauses()}"
            is IterationSpace.CounterKindsOn ->
                "For each kind of counter on ${space.entity.description}, ${bodyClauses()}"
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
 * naturally on the stack and in oracle text. The inner effect describes the
 * visited entity as [ITERATION_ENTITY_NOUN] (`EffectTarget.IterationEntity`); we
 * rewrite that mention to "each [filter]" so e.g. "Deal X damage to that
 * permanent" + `AllCreaturesOpponentsControl` renders as "Deal X damage to
 * each creature an opponent controls" rather than the broken concatenation
 * "Deal X damage to that permanent all creatures an opponent controls".
 */
private fun renderGroupDescription(innerText: String, filter: GroupFilter, noRegenerate: Boolean): String {
    // Strip the leading "all " from the filter description so we can splice it in
    // after "each".
    val each = "each ${filter.description.removePrefix("all ").trimStart()}"
    val rewritten = if (innerText.contains(ITERATION_ENTITY_NOUN, ignoreCase = true)) {
        innerText.replace(ITERATION_ENTITY_NOUN, each, ignoreCase = true)
    } else {
        "$innerText ${filter.description.replaceFirstChar { it.lowercase() }}"
    }.replaceFirstChar { it.uppercase() }
    return if (noRegenerate) "$rewritten. They can't be regenerated" else rewritten
}

private fun List<Effect>.asBody(): Effect =
    singleOrNull() ?: CompositeEffect(this)

// =============================================================================
// Lowering facades — the five pre-unification names, kept so card source (and the
// mtgish emitter's rendered DSL) is unchanged. Same precedent as Effects.IfYouDo →
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
 * Execute a fresh sub-pipeline once per matching player and append selected collection
 * outputs across every iteration. [collectCollections] maps each body-local output name
 * to the aggregate name exposed to following effects.
 */
@Suppress("FunctionName")
fun ForEachPlayerCollectingEffect(
    players: Player,
    effects: List<Effect>,
    collectCollections: Map<String, String>
): ForEachEffect = ForEachEffect(
    IterationSpace.Players(players),
    effects.asBody(),
    collectCollections
)

/**
 * Run [effect] once per entity in a named pipeline collection, with the current entity
 * bound as [EffectTarget.IterationEntity] (Fight or Flight). Lowers to [ForEachEffect]
 * over [IterationSpace.Collection].
 */
@Suppress("FunctionName")
fun ForEachInCollectionEffect(collection: String, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.Collection(collection), effect)

/**
 * Apply [effect] to every battlefield entity matching [filter] (snapshotted before any
 * iteration), with the current entity bound as [EffectTarget.IterationEntity]. Lowers to
 * [ForEachEffect] over [IterationSpace.Group].
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
fun ForEachColorOfEffect(source: EffectTarget.SingleEntity, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.ColorsOf(source), effect)

/**
 * Run [effect] once per kind of counter among the permanents matching [filter], exposing
 * the current kind via the chosen-counter-kind context (Exotic Pets). Lowers to
 * [ForEachEffect] over [IterationSpace.CounterKindsAmong].
 */
@Suppress("FunctionName")
fun ForEachCounterKindAmongEffect(filter: GroupFilter, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.CounterKindsAmong(filter), effect)

/**
 * Run [effect] once per kind of counter on the referenced permanent, exposing the current kind
 * via the chosen-counter-kind context (Dramatist's Puppet). Lowers to [ForEachEffect] over
 * [IterationSpace.CounterKindsOn].
 */
@Suppress("FunctionName")
fun ForEachCounterKindOnEffect(entity: EffectTarget.SingleEntity, effect: Effect): ForEachEffect =
    ForEachEffect(IterationSpace.CounterKindsOn(entity), effect)
