package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseOnePerCategoryEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CopyCollectionIntoCollectionEffect
import com.wingedsheep.sdk.scripting.effects.EachPlayerChoosesCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.NoteCreatureTypeEffect
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.PairWithSourceEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import com.wingedsheep.sdk.scripting.effects.StorePlayerEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

// =============================================================================
// Typed slot handles
// =============================================================================
//
// One handle type per pipeline variable namespace (mirroring CardLinter.Space).
// A handle is only obtainable from a builder step that *wrote* the slot, so a
// read-without-write cannot be expressed at the Kotlin authoring layer. The
// handle is a thin wrapper over the generated key string — the serialized tree
// is exactly the same string-keyed CompositeEffect the engine already executes.

/** Handle to a named entry in `EffectContext.storedCollections` (a list of entity ids). */
@JvmInline
value class CollectionSlot(val key: String) {
    /** Read this collection back as a [CardSource] for a downstream [PipelineBuilder.gather]. */
    val asSource: CardSource get() = CardSource.FromVariable(key)

    /**
     * The number of entities in this collection, as a [DynamicAmount] — "that many", "for each
     * card exiled this way". Evaluated when read (the engine resolves a `"<key>_count"`
     * [DynamicAmount.VariableReference] to the stored collection's current size), so this is the
     * one spelling of that suffix convention.
     */
    val count: DynamicAmount get() = DynamicAmount.VariableReference("${key}_count")

    /** The first entity in this collection as an [EffectTarget] ([EffectTarget.PipelineTarget]). */
    val asTarget: EffectTarget.PipelineTarget get() = EffectTarget.PipelineTarget(key)

    /** The entity at [index] in this collection as an [EffectTarget]. */
    fun asTarget(index: Int): EffectTarget.PipelineTarget = EffectTarget.PipelineTarget(key, index)

    /** The controller of the entity at [index] in this collection ([EffectTarget.ControllerOfPipelineTarget]). */
    fun controllerOf(index: Int = 0): EffectTarget = EffectTarget.ControllerOfPipelineTarget(key, index)

    /**
     * The players recorded in it (by [PipelineBuilder.storePlayer]) as a plural [Player] —
     * "each player who …" — for `Effects.ForEachPlayer(slot.asPlayers, …)`.
     */
    val asPlayers: Player get() = Player.InCollection(key)

    companion object {
        /**
         * The tokens the most recent token-creating effect in this resolution made — the "they" of
         * "create two 1/1 tokens. They gain haste until end of turn."
         */
        val CreatedTokens: CollectionSlot = CollectionSlot(com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS)

        /**
         * The objects a batch trigger captured when it fired — "them" / "those creatures"
         * ([IterationSpace.TRIGGER_CAPTURED_COLLECTION]). Inside `Effects.Pipeline { }` the same
         * slot is `triggerCaptured`.
         */
        val TriggerCaptured: CollectionSlot = CollectionSlot(IterationSpace.TRIGGER_CAPTURED_COLLECTION)
    }
}

/** Handle to a named entry in `EffectContext.storedNumbers`. */
@JvmInline
value class NumberSlot(val key: String) {
    /** Read this number as a [DynamicAmount] (`DynamicAmount.VariableReference`). */
    val amount: DynamicAmount get() = DynamicAmount.VariableReference(key)
}

/** Handle to a named entry in `EffectContext.chosenValues` (a chosen name/type/color). */
@JvmInline
value class ChosenSlot(val key: String)

/** Handle to a named entry in `EffectContext.storedSubtypeGroups` (`List<Set<String>>`). */
@JvmInline
value class SubtypeGroupsSlot(val key: String)

/** Handle to a named entry in `EffectContext.storedStringLists` (e.g. every player's chosen creature type). */
@JvmInline
value class StringListSlot(val key: String)

/** Match objects with a subtype in the list stored in [slot]. */
fun GameObjectFilter.withSubtypeInStoredList(slot: StringListSlot): GameObjectFilter = withSubtypeInStoredList(slot.key)

/** Match objects with no subtype in the list stored in [slot] ("…that aren't of a type chosen this way"). */
fun GameObjectFilter.withoutSubtypeInStoredList(slot: StringListSlot): GameObjectFilter =
    withoutSubtypeInStoredList(slot.key)

/** Match cards whose name equals the name captured in [slot] (cross-namespace handle overload). */
fun GameObjectFilter.namedFromVariable(slot: ChosenSlot): GameObjectFilter = namedFromVariable(slot.key)

/** All creatures of the creature type chosen into [slot] ([GroupFilter.ChosenSubtypeCreatures]). */
fun GroupFilter.Companion.ChosenSubtypeCreatures(slot: ChosenSlot, excludeSelf: Boolean = false): GroupFilter =
    ChosenSubtypeCreatures(slot.key, excludeSelf)

/** Narrow this group to the objects having the creature type chosen into [slot]. */
fun GroupFilter.withChosenSubtype(slot: ChosenSlot): GroupFilter = copy(chosenSubtypeKey = slot.key)

/** Match objects with the subtype chosen into [slot] (e.g. by [PipelineBuilder.chooseOption]). */
fun GameObjectFilter.withSubtypeFromVariable(slot: ChosenSlot): GameObjectFilter = withSubtypeFromVariable(slot.key)

/** Match objects *without* the subtype chosen into [slot] ("creatures that aren't of the chosen type"). */
fun GameObjectFilter.withoutSubtypeFromVariable(slot: ChosenSlot): GameObjectFilter =
    withCardPredicate(com.wingedsheep.sdk.scripting.predicates.CardPredicate.Not(
        com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtypeFromVariable(slot.key)
    ))

/** Match objects sharing a subtype with every group in [slot] ([PipelineBuilder.gatherSubtypes]). */
fun GameObjectFilter.withSubtypeInEachStoredGroup(slot: SubtypeGroupsSlot): GameObjectFilter =
    withSubtypeInEachStoredGroup(slot.key)

/** Selected + remainder pair returned by the `*Split` selection verbs. */
data class SelectionSlots(val selected: CollectionSlot, val remainder: CollectionSlot)

/** Matching + rest pair returned by [PipelineBuilder.filterSplit]. */
data class FilterSlots(val matching: CollectionSlot, val rest: CollectionSlot)

/** Match + all-revealed pair returned by [PipelineBuilder.gatherUntilMatch]. */
data class UntilMatchSlots(val match: CollectionSlot, val revealed: CollectionSlot)

/** Chosen + other pile pair returned by [PipelineBuilder.choosePile]. */
data class PileSlots(val chosen: CollectionSlot, val other: CollectionSlot)

@DslMarker
annotation class PipelineDsl

// =============================================================================
// Builder
// =============================================================================

/**
 * Inline pipeline builder — the one way a card file composes a Gather → Select →
 * Move pipeline. Card code never spells a slot key: the raw string-keyed step
 * constructors are SDK-internal, and `FacadeBoundaryTest` rejects them in cards.
 *
 * Each step verb serializes to the existing pipeline step `Effect` (one verb per
 * step type — the vocabulary grows with step types, never with cards) and returns
 * a typed slot handle. Keys are auto-generated deterministically per pipeline
 * (`"<verb><stepIndex>"`, e.g. `gathered0`, `selected1`), so renaming a Kotlin
 * `val` never churns the serialized JSON. A producing step's `name =` override
 * exists only for a key something *outside* the pipeline's lexical scope must
 * read (an `Effects.IfYouDo` success criterion that is a sibling of the pipeline);
 * everything inside reads the handle.
 *
 * Entry point: [Effects.Pipeline]. Example (the spec's Drop of Honey shape):
 *
 * ```kotlin
 * effect = Effects.Pipeline {
 *     val tied = gather(GameObjectFilter.Creature.hasLeastPowerAmongAllCreatures())
 *     val pick = chooseExactly(1, from = tied,
 *         prompt = "Choose a creature with the least power to destroy",
 *         useTargetingUI = true)
 *     destroy(pick, noRegenerate = true)
 * }
 * ```
 *
 * Non-pipeline effects interleave via [run]; branch scopes ([ifNotEmpty]/[orElse],
 * [forEachCaptured]) share the outer scope's handles by lexical capture, matching
 * the engine's actual `EffectContext` scoping (branches don't start fresh scopes).
 */
@PipelineDsl
class PipelineBuilder private constructor(private val shared: Shared) {

    private val steps = mutableListOf<Effect>()

    /** Key counter + name registry, shared across nested branch scopes so keys stay unique. */
    private class Shared {
        var stepIndex = 0
        val usedKeys = mutableSetOf<String>()
    }

    private fun nextIndex(): Int = shared.stepIndex++

    private fun slotKey(verb: String, index: Int, name: String?): String {
        val key = name ?: "$verb$index"
        require(shared.usedKeys.add(key)) {
            "Duplicate pipeline slot name '$key' — explicit name= overrides must be unique within one pipeline { }"
        }
        return key
    }

    private fun nested(block: PipelineBuilder.() -> Unit): List<Effect> =
        PipelineBuilder(shared).apply(block).steps.toList()

    /** A nested branch body: a single step stays bare; multiple steps wrap in a [CompositeEffect]. */
    private fun nestedAsEffect(block: PipelineBuilder.() -> Unit): Effect {
        val body = nested(block)
        require(body.isNotEmpty()) { "Pipeline branch block must add at least one step" }
        return body.singleOrNull() ?: CompositeEffect(body)
    }

    // =========================================================================
    // Gather
    // =========================================================================

    /**
     * The collection a batch trigger already seeded for this resolution — the objects that caused
     * it to fire, i.e. printed "them" / "those creatures" / "that many". Produced by the engine
     * rather than by a step in this pipeline, so it takes no slot index and can be read straight
     * away:
     *
     * ```kotlin
     * val creatureCards = filter(triggerCaptured, GameObjectFilter.Creature)
     * ```
     *
     * Empty when the trigger captured nothing, which every downstream step treats as "no cards".
     */
    val triggerCaptured: CollectionSlot get() = CollectionSlot.TriggerCaptured

    /** Gather cards from [source] into a new collection ([GatherCardsEffect]). */
    fun gather(
        source: CardSource,
        revealed: Boolean = false,
        name: String? = null,
        search: Boolean = false,
        lookAudience: LookAudience = LookAudience.Controller
    ): CollectionSlot {
        val slot = CollectionSlot(slotKey("gathered", nextIndex(), name))
        steps += GatherCardsEffect(
            source = source,
            storeAs = slot.key,
            revealed = revealed,
            lookAudience = lookAudience,
            search = search
        )
        return slot
    }

    /** Gather battlefield permanents matching [filter] ([CardSource.BattlefieldMatching] shorthand). */
    fun gather(
        filter: GameObjectFilter,
        player: Player = Player.Each,
        excludeSelf: Boolean = false,
        includeAttachments: Boolean = false,
        excludeTriggering: Boolean = false,
        revealed: Boolean = false,
        name: String? = null
    ): CollectionSlot = gather(
        source = CardSource.BattlefieldMatching(
            filter = filter,
            player = player,
            excludeSelf = excludeSelf,
            includeAttachments = includeAttachments,
            excludeTriggering = excludeTriggering
        ),
        revealed = revealed,
        name = name
    )

    /**
     * Mill [count] cards from [player]'s library (CR 701.13) and return the milled cards —
     * "mill three cards, then … a creature card milled this way".
     */
    fun mill(count: DynamicAmount, player: Player = Player.You): CollectionSlot {
        val milled = gather(CardSource.TopOfLibrary(count, player, isMill = true))
        move(milled, CardDestination.ToZone(Zone.GRAVEYARD, player))
        return milled
    }

    /** Mill [count] cards from [player]'s library and return the milled cards. */
    fun mill(count: Int, player: Player = Player.You): CollectionSlot = mill(DynamicAmount.Fixed(count), player)

    /**
     * Walk [player]'s library top-down until [count] cards matching [filter] are found
     * ([GatherUntilMatchEffect]). Returns both the matches and everything revealed along
     * the way (including the matches). Does not emit a reveal event — pair with [reveal].
     */
    fun gatherUntilMatch(
        filter: GameObjectFilter,
        player: Player = Player.You,
        count: DynamicAmount = DynamicAmount.Fixed(1),
        matchName: String? = null,
        revealedName: String? = null
    ): UntilMatchSlots {
        val index = nextIndex()
        val match = CollectionSlot(slotKey("matched", index, matchName))
        val revealed = CollectionSlot(slotKey("revealed", index, revealedName))
        steps += GatherUntilMatchEffect(
            player = player,
            filter = filter,
            storeMatch = match.key,
            storeRevealed = revealed.key,
            count = count
        )
        return UntilMatchSlots(match, revealed)
    }

    /** Extract each gathered entity's subtypes into a [SubtypeGroupsSlot] ([GatherSubtypesEffect]). */
    fun gatherSubtypes(from: CollectionSlot, name: String? = null): SubtypeGroupsSlot {
        val slot = SubtypeGroupsSlot(slotKey("subtypes", nextIndex(), name))
        steps += GatherSubtypesEffect(from = from.key, storeAs = slot.key)
        return slot
    }

    // =========================================================================
    // Select
    // =========================================================================

    private fun select(
        mode: SelectionMode,
        from: CollectionSlot,
        chooser: Chooser,
        filter: GameObjectFilter,
        prompt: String?,
        selectedLabel: String?,
        remainderLabel: String?,
        useTargetingUI: Boolean,
        showAllCards: Boolean,
        restrictions: List<SelectionRestriction>,
        alwaysPrompt: Boolean,
        matchChosenCreatureType: Boolean,
        name: String?,
        remainderName: String?,
        withRemainder: Boolean
    ): SelectionSlots {
        val index = nextIndex()
        val selected = CollectionSlot(slotKey("selected", index, name))
        val remainder = if (withRemainder) CollectionSlot(slotKey("remainder", index, remainderName)) else null
        steps += SelectFromCollectionEffect(
            from = from.key,
            selection = mode,
            chooser = chooser,
            filter = filter,
            storeSelected = selected.key,
            storeRemainder = remainder?.key,
            matchChosenCreatureType = matchChosenCreatureType,
            prompt = prompt,
            selectedLabel = selectedLabel,
            remainderLabel = remainderLabel,
            useTargetingUI = useTargetingUI,
            showAllCards = showAllCards,
            restrictions = restrictions,
            alwaysPrompt = alwaysPrompt
        )
        return SelectionSlots(selected, remainder ?: selected)
    }

    /** Player chooses exactly [count] cards from [from] ([SelectFromCollectionEffect]). */
    fun chooseExactly(
        count: DynamicAmount,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.ChooseExactly(count), from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType,
        name, remainderName = null, withRemainder = false
    ).selected

    /** Player chooses exactly [count] cards from [from]. */
    fun chooseExactly(
        count: Int,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = chooseExactly(
        DynamicAmount.Fixed(count), from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType, name
    )

    /** Like [chooseExactly], but also keeps the non-selected cards as a remainder slot. */
    fun chooseExactlySplit(
        count: Int,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)), from, chooser, filter, prompt,
        selectedLabel, remainderLabel, useTargetingUI, showAllCards, restrictions, alwaysPrompt,
        matchChosenCreatureType, name, remainderName, withRemainder = true
    )

    /** Player may choose up to [count] cards from [from]. */
    fun chooseUpTo(
        count: DynamicAmount,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.ChooseUpTo(count), from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType,
        name, remainderName = null, withRemainder = false
    ).selected

    /** Player may choose up to [count] cards from [from]. */
    fun chooseUpTo(
        count: Int,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = chooseUpTo(
        DynamicAmount.Fixed(count), from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType, name
    )

    /** Like [chooseUpTo], but also keeps the non-selected cards as a remainder slot. */
    fun chooseUpToSplit(
        count: Int,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)), from, chooser, filter, prompt,
        selectedLabel, remainderLabel, useTargetingUI, showAllCards, restrictions, alwaysPrompt,
        matchChosenCreatureType, name, remainderName, withRemainder = true
    )

    /** Player may select any number of cards (0..all) from [from] ([SelectionMode.ChooseAnyNumber]). */
    fun chooseAnyNumber(
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.ChooseAnyNumber, from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType,
        name, remainderName = null, withRemainder = false
    ).selected

    /** Like [chooseAnyNumber], but also keeps the non-selected cards as a remainder slot. */
    fun chooseAnyNumberSplit(
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.ChooseAnyNumber, from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions, alwaysPrompt, matchChosenCreatureType,
        name, remainderName, withRemainder = true
    )

    /** Like [chooseExactly] with a dynamic count, also keeping the non-selected cards as a remainder slot. */
    fun chooseExactlySplit(
        count: DynamicAmount,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.ChooseExactly(count), from, chooser, filter, prompt,
        selectedLabel, remainderLabel, useTargetingUI, showAllCards, restrictions, alwaysPrompt,
        matchChosenCreatureType, name, remainderName, withRemainder = true
    )

    /** Like [chooseUpTo] with a dynamic count, also keeping the non-selected cards as a remainder slot. */
    fun chooseUpToSplit(
        count: DynamicAmount,
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        restrictions: List<SelectionRestriction> = emptyList(),
        alwaysPrompt: Boolean = false,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.ChooseUpTo(count), from, chooser, filter, prompt,
        selectedLabel, remainderLabel, useTargetingUI, showAllCards, restrictions, alwaysPrompt,
        matchChosenCreatureType, name, remainderName, withRemainder = true
    )

    /**
     * Player may choose up to one *spell* from [from] ([SelectionMode.ChooseSpell]) — [filter] is
     * tested against the face that would be cast rather than the card's current characteristics.
     */
    fun chooseSpell(
        from: CollectionSlot,
        chooser: Chooser = Chooser.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        prompt: String? = null,
        selectedLabel: String? = null,
        remainderLabel: String? = null,
        useTargetingUI: Boolean = false,
        showAllCards: Boolean = false,
        alwaysPrompt: Boolean = false,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.ChooseSpell, from, chooser, filter, prompt, selectedLabel, remainderLabel,
        useTargetingUI, showAllCards, restrictions = emptyList(), alwaysPrompt = alwaysPrompt,
        matchChosenCreatureType = false, name = name, remainderName = null, withRemainder = false
    ).selected

    /** Engine picks [count] cards at random — no player choice ([SelectionMode.Random]). */
    fun chooseRandom(
        count: DynamicAmount,
        from: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.Random(count), from, Chooser.Controller, filter, prompt = null,
        selectedLabel = null, remainderLabel = null, useTargetingUI = false, showAllCards = false,
        restrictions = emptyList(), alwaysPrompt = false, matchChosenCreatureType = false,
        name = name, remainderName = null, withRemainder = false
    ).selected

    /** Like [chooseRandom], also keeping the cards not picked as a remainder slot. */
    fun chooseRandomSplit(
        count: Int,
        from: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any
    ): SelectionSlots = select(
        SelectionMode.Random(DynamicAmount.Fixed(count)), from, Chooser.Controller, filter, prompt = null,
        selectedLabel = null, remainderLabel = null, useTargetingUI = false, showAllCards = false,
        restrictions = emptyList(), alwaysPrompt = false, matchChosenCreatureType = false,
        name = null, remainderName = null, withRemainder = true
    )

    /** Engine picks [count] cards at random — no player choice. */
    fun chooseRandom(
        count: Int,
        from: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any,
        name: String? = null
    ): CollectionSlot = chooseRandom(DynamicAmount.Fixed(count), from, filter, name)

    /** Select every card in [from] (no choice — [SelectionMode.All]), optionally narrowed by [filter]. */
    fun selectAll(
        from: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any,
        matchChosenCreatureType: Boolean = false,
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.All, from, Chooser.Controller, filter, prompt = null,
        selectedLabel = null, remainderLabel = null, useTargetingUI = false, showAllCards = false,
        restrictions = emptyList(), alwaysPrompt = false, matchChosenCreatureType = matchChosenCreatureType,
        name = name, remainderName = null, withRemainder = false
    ).selected

    /** Like [selectAll], also keeping the cards that did not match as a remainder slot. */
    fun selectAllSplit(
        from: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any,
        matchChosenCreatureType: Boolean = false,
        name: String? = null,
        remainderName: String? = null
    ): SelectionSlots = select(
        SelectionMode.All, from, Chooser.Controller, filter, prompt = null,
        selectedLabel = null, remainderLabel = null, useTargetingUI = false, showAllCards = false,
        restrictions = emptyList(), alwaysPrompt = false, matchChosenCreatureType = matchChosenCreatureType,
        name = name, remainderName = remainderName, withRemainder = true
    )

    /**
     * Each controller of a permanent in [from] picks one of their own for every filter in
     * [categories] ([ChooseOnePerCategoryEffect]) — "chooses a permanent they control of each
     * permanent type". Choosers are asked in APNAP order and one permanent may cover several
     * categories. Returns the collection of everyone's picks; feed it to [exclude] for "the rest".
     */
    fun chooseOnePerCategory(
        from: CollectionSlot,
        categories: List<GameObjectFilter>,
        name: String? = null
    ): CollectionSlot {
        val slot = CollectionSlot(slotKey("kept", nextIndex(), name))
        steps += ChooseOnePerCategoryEffect(
            from = from.key,
            categories = categories,
            storeAs = slot.key
        )
        return slot
    }

    // =========================================================================
    // Filter / partition (no player choice)
    // =========================================================================

    /** Keep only the cards in [from] matching [filter] ([FilterCollectionEffect]). */
    fun filter(from: CollectionSlot, filter: GameObjectFilter, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("matching", nextIndex(), name))
        steps += FilterCollectionEffect(from = from.key, filter = filter, storeMatching = slot.key)
        return slot
    }

    /**
     * Keep only the cards in [from] passing the collection-relative [filter] ([FilterCollectionEffect]) —
     * "the creature with the greatest power among them". A [GameObjectFilter] in [matching] narrows
     * the candidates first.
     */
    fun filter(
        from: CollectionSlot,
        filter: CollectionFilter,
        matching: GameObjectFilter = GameObjectFilter.Any,
        name: String? = null
    ): CollectionSlot {
        val slot = CollectionSlot(slotKey("matching", nextIndex(), name))
        steps += FilterCollectionEffect(
            from = from.key,
            filter = matching,
            collectionFilter = filter,
            storeMatching = slot.key
        )
        return slot
    }

    /** Partition [from] by a collection-relative [filter] into passing and failing slots. */
    fun filterSplit(
        from: CollectionSlot,
        filter: CollectionFilter,
        name: String? = null,
        restName: String? = null
    ): FilterSlots {
        val index = nextIndex()
        val matching = CollectionSlot(slotKey("matching", index, name))
        val rest = CollectionSlot(slotKey("rest", index, restName))
        steps += FilterCollectionEffect(
            from = from.key,
            collectionFilter = filter,
            storeMatching = matching.key,
            storeNonMatching = rest.key
        )
        return FilterSlots(matching, rest)
    }

    /** Partition [from] by a [GameObjectFilter] into matching and non-matching slots. */
    fun filterSplit(
        from: CollectionSlot,
        filter: GameObjectFilter,
        name: String? = null,
        restName: String? = null
    ): FilterSlots {
        val index = nextIndex()
        val matching = CollectionSlot(slotKey("matching", index, name))
        val rest = CollectionSlot(slotKey("rest", index, restName))
        steps += FilterCollectionEffect(
            from = from.key,
            filter = filter,
            storeMatching = matching.key,
            storeNonMatching = rest.key
        )
        return FilterSlots(matching, rest)
    }

    /**
     * Set difference: the members of [from] that are **not** in [minus]
     * ([CollectionFilter.ExcludeOtherCollection]). The "…and \<does something to\> the rest"
     * half of a choose-then-punish pipeline — pair with [chooseOnePerCategory] or a select step.
     */
    fun exclude(from: CollectionSlot, minus: CollectionSlot, name: String? = null): CollectionSlot =
        filter(from, CollectionFilter.ExcludeOtherCollection(minus.key), name = name)

    // =========================================================================
    // Capture / store
    // =========================================================================

    /** Snapshot the controller of each card in [from] as a parallel list ([CaptureControllersEffect]). */
    fun captureControllers(from: CollectionSlot, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("controllers", nextIndex(), name))
        steps += CaptureControllersEffect(from = from.key, storeAs = slot.key)
        return slot
    }

    /** Capture the name of the first card in [from] ([StoreCardNameEffect]). */
    fun storeCardName(from: CollectionSlot, name: String? = null): ChosenSlot {
        val slot = ChosenSlot(slotKey("cardName", nextIndex(), name))
        steps += StoreCardNameEffect(from = from.key, storeAs = slot.key)
        return slot
    }

    /**
     * Record [player] (default: the player this step resolves for — the iterated player inside
     * [forEachPlayerCollecting]) in a collection ([StorePlayerEffect]); read it back with
     * [CollectionSlot.asPlayers]. With [onlyIf], the player is recorded only when the condition
     * holds at that moment — "each player who can't …" snapshotted before anyone acts.
     */
    fun storePlayer(player: Player = Player.You, onlyIf: Condition? = null, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("players", nextIndex(), name))
        val store = StorePlayerEffect(storeAs = slot.key, player = player)
        steps += if (onlyIf == null) store else Effects.If(onlyIf, store)
        return slot
    }

    /** Evaluate [amount] once and store it for later [NumberSlot.amount] reads ([StoreNumberEffect]). */
    fun storeNumber(amount: DynamicAmount, name: String? = null): NumberSlot {
        val slot = NumberSlot(slotKey("number", nextIndex(), name))
        steps += StoreNumberEffect(name = slot.key, amount = amount)
        return slot
    }

    /**
     * Player names a card ([ChooseOptionEffect] over [OptionType.CARD_NAME]); with
     * [excludeBasicLandNames], "choose a card name other than a basic land card name"; with
     * [pool] = [CardNamePool.NONLAND], "choose a nonland card name".
     */
    fun chooseCardName(
        prompt: String? = null,
        excludeBasicLandNames: Boolean = false,
        pool: CardNamePool = CardNamePool.ANY,
        name: String? = null
    ): ChosenSlot {
        val slot = ChosenSlot(slotKey("cardName", nextIndex(), name))
        steps += Effects.ChooseCardName(slot.key, prompt, excludeBasicLandNames, pool)
        return slot
    }

    /** Player chooses an option (creature type / color / card name / …) ([ChooseOptionEffect]). */
    fun chooseOption(
        optionType: OptionType,
        prompt: String? = null,
        excludedOptions: List<String> = emptyList(),
        name: String? = null
    ): ChosenSlot {
        val slot = ChosenSlot(slotKey("chosen", nextIndex(), name))
        steps += ChooseOptionEffect(
            optionType = optionType,
            storeAs = slot.key,
            prompt = prompt,
            excludedOptions = excludedOptions
        )
        return slot
    }

    /** Each player chooses a creature type; the choices are stored as a list ([EachPlayerChoosesCreatureTypeEffect]). */
    fun eachPlayerChoosesCreatureType(name: String? = null): StringListSlot {
        val slot = StringListSlot(slotKey("chosenTypes", nextIndex(), name))
        steps += EachPlayerChoosesCreatureTypeEffect(storeAs = slot.key)
        return slot
    }

    /** Note a creature type not yet noted on the source ([NoteCreatureTypeEffect]). */
    fun noteCreatureType(prompt: String? = null, name: String? = null): ChosenSlot {
        val slot = ChosenSlot(slotKey("noted", nextIndex(), name))
        steps += NoteCreatureTypeEffect(storeAs = slot.key, prompt = prompt)
        return slot
    }

    /**
     * Select a target mid-resolution and store the chosen entity ids ([SelectTargetEffect]).
     * Only for non-targeting choices or choices that depend on earlier pipeline results —
     * printed "target" wording must use cast-time targeting instead.
     */
    fun selectTarget(
        requirement: TargetRequirement,
        nonTargeting: Boolean = false,
        name: String? = null
    ): CollectionSlot {
        val slot = CollectionSlot(slotKey("target", nextIndex(), name))
        steps += SelectTargetEffect(requirement = requirement, storeAs = slot.key, nonTargeting = nonTargeting)
        return slot
    }

    /**
     * Create a copy of the card [source] refers to (CR 707.12) and store the copy
     * ([CopyCardIntoCollectionEffect]) — pair with a cast-from-collection effect.
     */
    fun copyCard(source: EffectTarget, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("copy", nextIndex(), name))
        steps += CopyCardIntoCollectionEffect(source = source, storeAs = slot.key)
        return slot
    }

    /** Create a copy of each card in [from] and store the copies ([CopyCollectionIntoCollectionEffect]). */
    fun copyCards(from: CollectionSlot, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("copies", nextIndex(), name))
        steps += CopyCollectionIntoCollectionEffect(from = from.key, storeAs = slot.key)
        return slot
    }

    // =========================================================================
    // Reveal / pile choice
    // =========================================================================

    /** Reveal the cards in [from] to all players ([RevealCollectionEffect]). */
    fun reveal(
        from: CollectionSlot,
        revealToSelf: Boolean = true,
        fromZone: Zone? = null,
        toZone: Zone? = null
    ) {
        nextIndex()
        steps += RevealCollectionEffect(from = from.key, revealToSelf = revealToSelf, fromZone = fromZone, toZone = toZone)
    }

    /** A player picks one of two piles; both are re-stored as chosen/other ([ChoosePileEffect]). */
    fun choosePile(
        pileA: CollectionSlot,
        pileB: CollectionSlot,
        pileALabel: String = "Pile 1",
        pileBLabel: String = "Pile 2",
        chooser: Chooser = Chooser.Controller,
        prompt: String? = null,
        chosenName: String? = null,
        otherName: String? = null
    ): PileSlots {
        val index = nextIndex()
        val chosen = CollectionSlot(slotKey("chosenPile", index, chosenName))
        val other = CollectionSlot(slotKey("otherPile", index, otherName))
        steps += ChoosePileEffect(
            pileA = pileA.key,
            pileB = pileB.key,
            pileALabel = pileALabel,
            pileBLabel = pileBLabel,
            chooser = chooser,
            storeChosenAs = chosen.key,
            storeOtherAs = other.key,
            prompt = prompt
        )
        return PileSlots(chosen, other)
    }

    // =========================================================================
    // Move
    // =========================================================================

    /**
     * Move the cards in [from] to [destination] ([MoveCollectionEffect]). Prefer the named
     * shortcuts below ([destroy], [sacrifice], [discard], [exile], [toHand], [toGraveyard],
     * [toLibraryTop], [toLibraryBottom]) when one fits.
     *
     * @param filter only the cards in [from] matching this move; the rest stay where they are.
     * @param lookableInExile the controller may keep looking at face-down exiled cards.
     * @param attachTo the permanent an Aura/Equipment entering the battlefield attaches to.
     */
    fun move(
        from: CollectionSlot,
        destination: CardDestination,
        order: CardOrder = CardOrder.Preserve,
        revealed: Boolean = false,
        revealToSelf: Boolean = true,
        moveType: MoveType = MoveType.Default,
        linkToSource: Boolean = false,
        unlinkFromSource: Boolean = false,
        faceDown: FaceDownMode? = null,
        lookableInExile: Boolean = false,
        noRegenerate: Boolean = false,
        underOwnersControl: Boolean = false,
        addCounterType: CounterType? = null,
        markEnteredViaSourceAbility: Boolean = false,
        filter: GameObjectFilter? = null,
        attachTo: EffectTarget? = null
    ) {
        nextIndex()
        steps += MoveCollectionEffect(
            from = from.key,
            destination = destination,
            order = order,
            revealed = revealed,
            revealToSelf = revealToSelf,
            moveType = moveType,
            linkToSource = linkToSource,
            unlinkFromSource = unlinkFromSource,
            faceDown = faceDown,
            lookableInExile = lookableInExile,
            noRegenerate = noRegenerate,
            underOwnersControl = underOwnersControl,
            addCounterType = addCounterType,
            markEnteredViaSourceAbility = markEnteredViaSourceAbility,
            filter = filter,
            attachTo = attachTo
        )
    }

    /**
     * Like [move], but also records which cards actually moved ([MoveCollectionEffect.storeMovedAs])
     * — "exile … then return the cards exiled this way", where a replacement or a card that left
     * first can make the moved set smaller than [from].
     */
    fun moveTracked(
        from: CollectionSlot,
        destination: CardDestination,
        order: CardOrder = CardOrder.Preserve,
        revealed: Boolean = false,
        revealToSelf: Boolean = true,
        moveType: MoveType = MoveType.Default,
        linkToSource: Boolean = false,
        unlinkFromSource: Boolean = false,
        faceDown: FaceDownMode? = null,
        lookableInExile: Boolean = false,
        noRegenerate: Boolean = false,
        underOwnersControl: Boolean = false,
        addCounterType: CounterType? = null,
        markEnteredViaSourceAbility: Boolean = false,
        filter: GameObjectFilter? = null,
        attachTo: EffectTarget? = null,
        name: String? = null
    ): CollectionSlot {
        val slot = CollectionSlot(slotKey("moved", nextIndex(), name))
        steps += MoveCollectionEffect(
            from = from.key,
            destination = destination,
            order = order,
            revealed = revealed,
            revealToSelf = revealToSelf,
            moveType = moveType,
            linkToSource = linkToSource,
            unlinkFromSource = unlinkFromSource,
            faceDown = faceDown,
            lookableInExile = lookableInExile,
            noRegenerate = noRegenerate,
            storeMovedAs = slot.key,
            underOwnersControl = underOwnersControl,
            addCounterType = addCounterType,
            markEnteredViaSourceAbility = markEnteredViaSourceAbility,
            filter = filter,
            attachTo = attachTo
        )
        return slot
    }

    /** Destroy the permanents in [from] (move to owners' graveyards with [MoveType.Destroy]). */
    fun destroy(from: CollectionSlot, noRegenerate: Boolean = false) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy, noRegenerate = noRegenerate)

    /** Sacrifice the permanents in [from] (owners' graveyards, [MoveType.Sacrifice]). */
    fun sacrifice(from: CollectionSlot) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Sacrifice)

    /**
     * Discard the cards in [from] ([MoveType.Discard]) — they go to their owner's graveyard and
     * fire discard triggers. [player] names whose hand they leave ("that player discards …").
     */
    fun discard(from: CollectionSlot, player: Player = Player.You) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD, player), moveType = MoveType.Discard)

    /**
     * Soulbond-pair the creature in [from] with the pipeline's source (CR 702.95a). An empty
     * [from] is a no-op, so this is also the landing spot for a declined "you may pair".
     */
    fun pairWithSource(from: CollectionSlot) {
        steps += PairWithSourceEffect(from = from.key)
    }

    /** Exile the cards in [from]. */
    fun exile(
        from: CollectionSlot,
        owner: Player = Player.You,
        faceDown: FaceDownMode? = null,
        linkToSource: Boolean = false
    ) = move(
        from, CardDestination.ToZone(Zone.EXILE, owner),
        faceDown = faceDown, linkToSource = linkToSource
    )

    /** Put the cards in [from] into [player]'s hand. */
    fun toHand(from: CollectionSlot, player: Player = Player.You, revealed: Boolean = false) =
        move(from, CardDestination.ToZone(Zone.HAND, player), revealed = revealed)

    /** Put the cards in [from] into [player]'s graveyard. */
    fun toGraveyard(from: CollectionSlot, player: Player = Player.You) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD, player))

    /** Put the cards in [from] on top of [player]'s library. */
    fun toLibraryTop(
        from: CollectionSlot,
        player: Player = Player.You,
        order: CardOrder = CardOrder.ControllerChooses
    ) = move(from, CardDestination.ToZone(Zone.LIBRARY, player, ZonePlacement.Top), order = order)

    /** Put the cards in [from] on the bottom of [player]'s library. */
    fun toLibraryBottom(
        from: CollectionSlot,
        player: Player = Player.You,
        order: CardOrder = CardOrder.ControllerChooses
    ) = move(from, CardDestination.ToZone(Zone.LIBRARY, player, ZonePlacement.Bottom), order = order)

    // =========================================================================
    // Branching / iteration
    // =========================================================================

    /**
     * Run [block] only if [collection] holds at least [minSize] cards (optionally narrowed by
     * [filter]) — serializes to [ConditionalOnCollectionEffect]. Chain `orElse { }` for the
     * empty branch. Handles from the outer scope are visible inside the branch (the engine's
     * `EffectContext` branches share the surrounding scope).
     */
    fun ifNotEmpty(
        collection: CollectionSlot,
        filter: GameObjectFilter = GameObjectFilter.Any,
        minSize: Int = 1,
        countDistinctCardTypes: Boolean = false,
        block: PipelineBuilder.() -> Unit
    ): ConditionalHandle {
        nextIndex()
        val stepPosition = steps.size
        steps += ConditionalOnCollectionEffect(
            collection = collection.key,
            ifNotEmpty = nestedAsEffect(block),
            minSize = minSize,
            countDistinctCardTypes = countDistinctCardTypes,
            filter = filter
        )
        return ConditionalHandle(stepPosition)
    }

    /** Continuation handle returned by [ifNotEmpty] so the else-branch can be chained. */
    inner class ConditionalHandle internal constructor(private val stepPosition: Int) {
        /** The branch to run when the collection check fails. */
        infix fun orElse(block: PipelineBuilder.() -> Unit) {
            val conditional = steps[stepPosition] as ConditionalOnCollectionEffect
            require(conditional.ifEmpty == null) { "orElse { } may only be chained once" }
            steps[stepPosition] = conditional.copy(ifEmpty = nestedAsEffect(block))
        }
    }

    /**
     * Iterate the unique controllers captured by [captureControllers] for cards that appear in
     * [collection], running [block] once per controller ([ForEachCapturedControllerEffect]).
     * The block receives the per-controller tally as a [NumberSlot].
     */
    fun forEachCaptured(
        collection: CollectionSlot,
        original: CollectionSlot,
        controllers: CollectionSlot,
        countName: String? = null,
        block: PipelineBuilder.(count: NumberSlot) -> Unit
    ) {
        val count = NumberSlot(slotKey("count", nextIndex(), countName))
        steps += ForEachCapturedControllerEffect(
            collection = collection.key,
            originalCollection = original.key,
            controllerSnapshot = controllers.key,
            countVariable = count.key,
            effects = PipelineBuilder(shared).apply { block(count) }.steps.toList()
        )
    }

    /**
     * Run [block] once per player matching [players] (the iterated player is `Player.You` inside,
     * with a fresh collection scope per iteration) and return, for each collection handle [block]
     * returns, the union of that collection across every iteration
     * ([com.wingedsheep.sdk.scripting.effects.ForEachPlayerCollectingEffect]) — "each player
     * chooses …; then destroy everything nobody chose".
     */
    fun forEachPlayerCollecting(
        players: Player,
        block: PipelineBuilder.() -> List<CollectionSlot>
    ): List<CollectionSlot> {
        nextIndex()
        val inner = PipelineBuilder(shared)
        val collected = inner.block()
        require(inner.steps.isNotEmpty()) { "forEachPlayerCollecting { } must add at least one step" }
        val aggregates = collected.map { CollectionSlot(slotKey("collected", nextIndex(), null)) }
        steps += com.wingedsheep.sdk.scripting.effects.ForEachPlayerCollectingEffect(
            players = players,
            effects = inner.steps.toList(),
            collectCollections = collected.zip(aggregates).associate { (each, all) -> each.key to all.key }
        )
        return aggregates
    }

    /**
     * Run [block] as the body of a do-while loop ([com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect])
     * — once, then again while [repeatCondition] holds — and return, for each collection handle
     * [block] returns, the union of that collection across every pass. Each pass starts from a
     * fresh collection scope, so the body re-gathers what it needs; only the returned handles
     * outlive it. "…then you exile a card from it. Repeat this process until all cards in that
     * hand have been exiled. That player returns the cards they exiled this way to their hand"
     * (Struggle for Sanity).
     */
    fun repeatCollecting(
        repeatCondition: RepeatCondition,
        block: PipelineBuilder.() -> List<CollectionSlot>
    ): List<CollectionSlot> {
        nextIndex()
        val inner = PipelineBuilder(shared)
        val collected = inner.block()
        require(inner.steps.isNotEmpty()) { "repeatCollecting { } must add at least one step" }
        val aggregates = collected.map { CollectionSlot(slotKey("collected", nextIndex(), null)) }
        steps += RepeatWhileEffect(
            body = inner.steps.singleOrNull() ?: CompositeEffect(inner.steps.toList()),
            repeatCondition = repeatCondition,
            collectCollections = collected.zip(aggregates).associate { (each, all) -> each.key to all.key }
        )
        return aggregates
    }

    // =========================================================================
    // Escape hatches
    // =========================================================================

    /** Append any non-pipeline [Effect] verbatim (a draw, a damage, a shuffle, …). */
    fun run(effect: Effect) {
        nextIndex()
        steps += effect
    }

    /**
     * Run an effect that is not a pipeline step but *writes* a collection under a key it is
     * handed — `Effects.DestroyAll(filter, storeDestroyedAs = it)`, `Patterns.Exile.impulse(…,
     * storeAs = it)` — and return a typed handle to what it wrote. The key is generated, so the
     * producer and every reader stay linked by the handle rather than by a spelled-out string.
     */
    fun runStoringCollection(effect: (key: String) -> Effect): CollectionSlot {
        val slot = CollectionSlot(slotKey("stored", nextIndex(), null))
        steps += effect(slot.key)
        return slot
    }

    /** Like [runStoringCollection], for an effect that records a number (`storeHeadsAs = it`). */
    fun runStoringNumber(effect: (key: String) -> Effect): NumberSlot {
        val slot = NumberSlot(slotKey("storedNumber", nextIndex(), null))
        steps += effect(slot.key)
        return slot
    }

    /** Like [runStoringCollection], for an effect that records a chosen value (a name, a type). */
    fun runStoringChoice(effect: (key: String) -> Effect): ChosenSlot {
        val slot = ChosenSlot(slotKey("storedChoice", nextIndex(), null))
        steps += effect(slot.key)
        return slot
    }

    /** Condition that [collection] currently holds a card matching [filter] ([CollectionContainsMatch]). */
    fun whenMatches(collection: CollectionSlot, filter: GameObjectFilter = GameObjectFilter.Any): Condition =
        CollectionContainsMatch(collection.key, filter)

    companion object {
        /** Key namespaces of the pipelines currently being built on this thread, innermost last. */
        private val building: ThreadLocal<ArrayDeque<Shared>> = ThreadLocal.withInitial { ArrayDeque() }

        internal fun build(
            stopOnError: Boolean,
            descriptionOverride: String?,
            descriptionAmounts: List<DynamicAmount>,
            block: PipelineBuilder.() -> Unit
        ): Effect {
            // A pipeline built while another is being built (an `Effects.Pipeline { }` inside a
            // `run(Effects.May(…))` of an outer one) executes in the same EffectContext, so it must
            // draw its keys from the same namespace — otherwise both would generate `gathered0`
            // and the inner one would silently overwrite a collection the outer one still reads.
            val enclosing = building.get().lastOrNull()
            val shared = enclosing ?: Shared()
            building.get().addLast(shared)
            val builder = try {
                PipelineBuilder(shared).apply(block)
            } finally {
                building.get().removeLast()
            }
            require(builder.steps.isNotEmpty()) { "pipeline { } must add at least one step" }
            return CompositeEffect(
                effects = builder.steps.toList(),
                stopOnError = stopOnError,
                descriptionOverride = descriptionOverride,
                descriptionAmounts = descriptionAmounts
            )
        }
    }
}
