package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.NoteCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
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

/** Match cards whose name equals the name captured in [slot] (cross-namespace handle overload). */
fun GameObjectFilter.namedFromVariable(slot: ChosenSlot): GameObjectFilter = namedFromVariable(slot.key)

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
 * Inline pipeline builder — the facade-respecting way to compose a one-off
 * Gather → Select → Move pipeline inside a card file without hand-threading
 * string slot keys (see `backlog/inline-pipeline-dsl.md`).
 *
 * Each step verb serializes to the existing pipeline step `Effect` (one verb per
 * step type — the vocabulary grows with step types, never with cards) and returns
 * a typed slot handle. Keys are auto-generated deterministically per builder
 * instance (`"<verb><stepIndex>"`, e.g. `gathered0`, `selected1`), so renaming a
 * Kotlin `val` never churns the serialized JSON; every producing step also takes
 * an optional `name =` override for readable goldens and byte-identical migration
 * of existing inline cards.
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

    /** Gather cards from [source] into a new collection ([GatherCardsEffect]). */
    fun gather(source: CardSource, revealed: Boolean = false, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("gathered", nextIndex(), name))
        steps += GatherCardsEffect(source = source, storeAs = slot.key, revealed = revealed)
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
        name: String? = null
    ): CollectionSlot = select(
        SelectionMode.All, from, Chooser.Controller, filter, prompt = null,
        selectedLabel = null, remainderLabel = null, useTargetingUI = false, showAllCards = false,
        restrictions = emptyList(), alwaysPrompt = false, matchChosenCreatureType = false,
        name = name, remainderName = null, withRemainder = false
    ).selected

    // =========================================================================
    // Filter / partition (no player choice)
    // =========================================================================

    /** Keep only the cards in [from] matching [filter] ([FilterCollectionEffect]). */
    fun filter(from: CollectionSlot, filter: CollectionFilter, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("matching", nextIndex(), name))
        steps += FilterCollectionEffect(from = from.key, filter = filter, storeMatching = slot.key)
        return slot
    }

    /** Keep only the cards in [from] matching [filter] ([CollectionFilter.MatchesFilter] shorthand). */
    fun filter(from: CollectionSlot, filter: GameObjectFilter, name: String? = null): CollectionSlot =
        filter(from, CollectionFilter.MatchesFilter(filter), name)

    /** Partition [from] into matching and non-matching slots ([FilterCollectionEffect]). */
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
            filter = filter,
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
    ): FilterSlots = filterSplit(from, CollectionFilter.MatchesFilter(filter), name, restName)

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

    /** Evaluate [amount] once and store it for later [NumberSlot.amount] reads ([StoreNumberEffect]). */
    fun storeNumber(amount: DynamicAmount, name: String? = null): NumberSlot {
        val slot = NumberSlot(slotKey("number", nextIndex(), name))
        steps += StoreNumberEffect(name = slot.key, amount = amount)
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
    fun selectTarget(requirement: TargetRequirement, name: String? = null): CollectionSlot {
        val slot = CollectionSlot(slotKey("target", nextIndex(), name))
        steps += SelectTargetEffect(requirement = requirement, storeAs = slot.key)
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

    /** Move the cards in [from] to [destination] ([MoveCollectionEffect]). */
    fun move(
        from: CollectionSlot,
        destination: CardDestination,
        order: CardOrder = CardOrder.Preserve,
        revealed: Boolean = false,
        revealToSelf: Boolean = true,
        moveType: MoveType = MoveType.Default,
        linkToSource: Boolean = false,
        unlinkFromSource: Boolean = false,
        faceDown: Boolean = false,
        noRegenerate: Boolean = false,
        underOwnersControl: Boolean = false,
        addCounterType: CounterType? = null,
        markEnteredViaSourceAbility: Boolean = false
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
            noRegenerate = noRegenerate,
            underOwnersControl = underOwnersControl,
            addCounterType = addCounterType,
            markEnteredViaSourceAbility = markEnteredViaSourceAbility
        )
    }

    /** Like [move], but also records which cards actually moved ([MoveCollectionEffect.storeMovedAs]). */
    fun moveTracked(
        from: CollectionSlot,
        destination: CardDestination,
        order: CardOrder = CardOrder.Preserve,
        revealed: Boolean = false,
        revealToSelf: Boolean = true,
        moveType: MoveType = MoveType.Default,
        linkToSource: Boolean = false,
        unlinkFromSource: Boolean = false,
        faceDown: Boolean = false,
        noRegenerate: Boolean = false,
        underOwnersControl: Boolean = false,
        addCounterType: CounterType? = null,
        markEnteredViaSourceAbility: Boolean = false,
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
            noRegenerate = noRegenerate,
            underOwnersControl = underOwnersControl,
            addCounterType = addCounterType,
            markEnteredViaSourceAbility = markEnteredViaSourceAbility,
            storeMovedAs = slot.key
        )
        return slot
    }

    /** Destroy the permanents in [from] (move to owners' graveyards with [MoveType.Destroy]). */
    fun destroy(from: CollectionSlot, noRegenerate: Boolean = false) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy, noRegenerate = noRegenerate)

    /** Sacrifice the permanents in [from] (owners' graveyards, [MoveType.Sacrifice]). */
    fun sacrifice(from: CollectionSlot) =
        move(from, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Sacrifice)

    /** Exile the cards in [from]. */
    fun exile(
        from: CollectionSlot,
        owner: Player = Player.You,
        faceDown: Boolean = false,
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

    // =========================================================================
    // Escape hatches
    // =========================================================================

    /** Append any non-pipeline [Effect] verbatim (a draw, a damage, a shuffle, …). */
    fun run(effect: Effect) {
        nextIndex()
        steps += effect
    }

    /** Condition that [collection] currently holds a card matching [filter] ([CollectionContainsMatch]). */
    fun whenMatches(collection: CollectionSlot, filter: GameObjectFilter = GameObjectFilter.Any): Condition =
        CollectionContainsMatch(collection.key, filter)

    companion object {
        internal fun build(
            stopOnError: Boolean,
            descriptionOverride: String?,
            descriptionAmounts: List<DynamicAmount>,
            block: PipelineBuilder.() -> Unit
        ): Effect {
            val builder = PipelineBuilder(Shared()).apply(block)
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
