package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CursorStep
import com.wingedsheep.engine.core.EffectProgram
import com.wingedsheep.engine.core.EntryDelta
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.ProgramCursor
import com.wingedsheep.engine.core.ProgramOrigin
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineOutputs
import com.wingedsheep.engine.handlers.mergeOutputs
import com.wingedsheep.engine.handlers.pipelineOutputs
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Runs an effect tree as an [EffectProgram]: walks [CompositeEffect] sequences itself and runs
 * every other node (a *leaf*) through [effectExecutor].
 *
 * **Pausing never changes the outcome.** A run that pauses and resumes does exactly what the same
 * run does when nothing pauses, because the frame's cursor is a zipper: every open composite's
 * step holds an exact delta ([CursorStep.Child.delta]) from which the context it was entered with
 * is rebuilt, and leaving a composite goes through one function ([Run.leave]) on both paths. A
 * scope's delta and the steps around it are computed once when it opens, so the frame pushed
 * beneath each leaf costs one step, not one per depth.
 *
 * **Running.** From the cursor:
 * 1. A composite opens a scope (its entry context, one resolution depth deeper when nested) and
 *    descends to its first child; an empty one has finished.
 * 2. A leaf gets the program pushed beneath it, cursor **at that leaf**, then runs. If it pauses,
 *    the pause propagates with the frame waiting beneath the leaf's own frames. If it finishes, the
 *    frame is dropped and its result is folded into the context ([foldResult]); if it fails, it is
 *    skipped — unless its composite has `stopOnError`, which abandons the rest of that one
 *    composite (only that one: a nested composite without the flag absorbs its own failures).
 *    Either way the object moves a failed leaf or an abandoned composite made stay followable.
 *    The frame popped after a leaf must be the one pushed beneath it; a value the leaf handed to
 *    "the frame beneath" without pausing is taken from it, as a resumer's hand-off would be.
 * 3. Advancing goes to the next sibling, or leaves the composite and moves on to its next sibling.
 *    Leaving hands the parent its entry context plus what the composite stored — new pipeline keys
 *    (a value shadowing an outer key doesn't escape), appended snapshots, and the object moves its
 *    events authorise. Leaving the root yields the program's [outputs] against its true base.
 *
 * **Resuming** ([resume]): the node at the cursor has finished; its outputs are in the frame's
 * context. The scopes are rebuilt from the cursor's steps and the run advances past it. The
 * finished program's outputs are what [run] would have returned; the automatic resumer publishes
 * them to the frame beneath.
 *
 * @param effectExecutor how a leaf runs: the registry's `execute`, which labels it
 *   ([originFor]) and leaves the resolution depth alone — the context already carries it.
 */
class EffectProgramInterpreter(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) {

    /**
     * Run [root] from the top. The program's origin is [EffectContext.programOrigin]. A
     * non-composite root isn't a program: it runs through the executor as it is.
     */
    fun run(state: GameState, root: Effect, context: EffectContext): EffectResult {
        val composite = root as? CompositeEffect ?: return effectExecutor(state, root, context)
        // Every live program is labelled; null is reserved for frames converted from old saves.
        // The engine paths that start a card's tree label it; anything else is marked as such.
        val origin = context.programOrigin ?: ProgramOrigin.UNATTRIBUTED
        val labelled = if (context.programOrigin == null) context.copy(programOrigin = origin) else context
        return Run(state, composite, origin, labelled, ArrayList()).start()
    }

    /**
     * Run [effects], which the step [context] is labelled for built itself, now, as a program of
     * their own: the root is `CompositeEffect(effects)`, the origin [EffectContext.programOrigin]
     * (the label handed down to that step's call).
     */
    fun runBuilt(state: GameState, effects: List<Effect>, context: EffectContext): EffectResult =
        run(state, CompositeEffect(effects), context)

    /** Continue [program]: the node at its cursor has finished, or it hasn't started. */
    fun resume(state: GameState, program: EffectProgram): EffectResult {
        val root = program.root as? CompositeEffect
            ?: return EffectResult.error(state, "Program root is not a composite")
        val steps = program.cursor.steps
        if (steps == ProgramCursor.START.steps) {
            return Run(state, root, program.origin, program.effectContext, ArrayList()).start()
        }
        val children = steps.map { step ->
            when (step) {
                is CursorStep.Child -> step
                CursorStep.Start -> return EffectResult.error(state, "Program cursor ${program.cursor} is malformed")
            }
        }
        val at = children.map { it.index }
        val scopes = ArrayList<Scope>(children.size)
        var node: Effect = root
        var entry: EffectContext? = null
        for ((depth, child) in children.withIndex()) {
            // Rebuild the entry from the parent's (the root's from the frame's context).
            entry = entry?.advancedBy(child.delta) ?: program.effectContext.rebasedOn(child.delta)
            val scope = open(node, at.subList(0, depth), entry, eventStart = 0, child.delta, steps.subList(0, depth))
                ?: return EffectResult.error(state, "Program cursor ${program.cursor} does not follow its root")
            scopes += scope
            // Every step, the last included, names a child its composite has.
            node = scope.composite.effects.getOrNull(child.index)
                ?: return EffectResult.error(state, "Program cursor ${program.cursor} is outside its root")
        }
        return Run(state, root, program.origin, program.effectContext, scopes).advanceFrom(at)
    }

    /**
     * An open composite at [path], entered with [entry]. [eventStart] is where its events begin in
     * this run's event list (0 for one opened before the pause this run resumed from). [delta] is
     * how [entry] differs from the parent's entry, and [prefix] the cursor steps of the composites
     * around it, both computed once when it opens so a leaf's frame costs one step.
     */
    private class Scope(
        val composite: CompositeEffect,
        val path: List<Int>,
        val entry: EffectContext,
        val eventStart: Int,
        val delta: EntryDelta,
        val prefix: List<CursorStep>,
    )

    /**
     * The one dispatch on node kind, shared by a fresh descent and a resume's rebuild: the scope
     * [node] opens, or null when it is a leaf. Later node kinds (gates, loops) add a branch here.
     */
    private fun open(
        node: Effect, path: List<Int>, entry: EffectContext, eventStart: Int,
        delta: EntryDelta, prefix: List<CursorStep>,
    ): Scope? = when (node) {
        is CompositeEffect -> Scope(node, path, entry, eventStart, delta, prefix)
        else -> null
    }

    private enum class LeafOutcome { CONTINUE, ABANDON_SCOPE, EXIT }

    private inner class Run(
        private var state: GameState,
        private val root: CompositeEffect,
        private val origin: ProgramOrigin?,
        private var context: EffectContext,
        private val scopes: ArrayList<Scope>,
    ) {
        private val events = ArrayList<GameEvent>()
        /** The run's result when a leaf ends it early: a pause, or a frame the leaf left behind. */
        private lateinit var exitResult: EffectResult

        fun start(): EffectResult = execute(emptyList(), descend = true)

        fun advanceFrom(path: List<Int>): EffectResult = execute(path, descend = false)

        private fun execute(start: List<Int>, descend: Boolean): EffectResult {
            var path = start
            var descending = descend
            while (true) {
                if (descending) {
                    val parent = scopes.lastOrNull()
                    val node: Effect = if (parent == null) root else parent.composite.effects[path.last()]
                    val entry = context
                    val scope = if (node !is CompositeEffect) null else if (parent == null) {
                        open(node, path, entry, events.size, rootDelta(entry), emptyList())
                    } else {
                        val delta = EntryDelta(PipelineOutputs.between(parent.entry, entry),
                            entry.resolutionDepth - parent.entry.resolutionDepth)
                        open(node, path, entry, events.size, delta, parent.prefix + CursorStep.Child(path.last(), parent.delta))
                    }
                    if (scope != null) {
                        if (path.isNotEmpty()) {
                            // A nested composite used to run through the registry's recursion —
                            // one resolution depth deeper, object references re-checked.
                            context = context.copy(resolutionDepth = context.resolutionDepth + 1)
                                .withCurrentObjectReferences(state)
                        }
                        scopes += scope
                        if (scope.composite.effects.isNotEmpty()) {
                            path = path + 0
                            continue
                        }
                        // An empty composite has finished as soon as it starts.
                        leave(aborted = false)?.let { return it }
                    } else {
                        when (runLeaf(node, path)) {
                            LeafOutcome.EXIT -> return exitResult
                            LeafOutcome.ABANDON_SCOPE -> {
                                path = scopes.last().path
                                leave(aborted = true)?.let { return it }
                            }
                            LeafOutcome.CONTINUE -> {}
                        }
                    }
                }
                // Advance from `path`, the node that has just finished.
                val parent = scopes.last()
                val next = path.last() + 1
                if (next < parent.composite.effects.size) {
                    path = parent.path + next
                    descending = true
                } else {
                    path = parent.path
                    leave(aborted = false)?.let { return it }
                    descending = false
                }
            }
        }

        private fun runLeaf(leaf: Effect, path: List<Int>): LeafOutcome {
            // Every leaf gets the program beneath it, the last one too: finishing the program —
            // leaving its scopes and publishing its outputs — is work that a pause must not skip.
            val inner = scopes.last()
            val cursor = ProgramCursor(inner.prefix + CursorStep.Child(path.last(), inner.delta))
            val frame = EffectProgram(root, origin, cursor, context)
            val leafContext = if (continuesDamageInstruction(leaf, path)) context.copy(continuesDamageInstruction = true) else context
            val result = effectExecutor(state.pushContinuation(frame), leaf, leafContext)
            if (result.outcome is Outcome.Paused) {
                // The frame waits beneath the leaf's own frames.
                exitResult = EffectResult.propagatePause(result.state, events + result.events)
                return LeafOutcome.EXIT
            }
            // Pops the program pushed above. Every executor that pushes a frame of its own either
            // pauses with it, pops it, or returns a state without it, so on Done or a failure the
            // top of the stack must be this program's frame — the last leaf included. Anything
            // else would be another frame eaten, so it fails rather than pops blind.
            val (top, popped) = result.state.popContinuation()
            if (top !is EffectProgram || top.root !== root || top.cursor != cursor) {
                exitResult = EffectResult.error(result.state, "Leaf ${stepName(leaf)} left a frame above its program")
                return LeafOutcome.EXIT
            }
            state = popped
            events.addAll(result.events)
            // A leaf that handed a value to "the frame beneath" without pausing wrote it into this
            // frame, exactly as a resumer would after a pause; take it, so both paths agree.
            if (top.effectContext !== context) {
                context = context.mergeOutputs(PipelineOutputs.between(context, top.effectContext), dedupSnapshots = true)
            }
            if (result.outcome !is Outcome.Done) {
                // A failed instruction is skipped (do as much as possible) unless its composite is
                // a cost-then-payoff sequence, which it abandons. The moves it made before failing
                // still happened, so they stay followable either way.
                context = context.authorizeObjectMoves(result.events)
                return if (scopes.last().composite.stopOnError) LeafOutcome.ABANDON_SCOPE else LeafOutcome.CONTINUE
            }
            context = context.foldResult(result)
            return LeafOutcome.CONTINUE
        }

        /**
         * "Deals 4 damage to target creature and 2 damage to that creature's controller" is one
         * damage instruction authored as two adjacent damage steps: a damage step directly after
         * a damage step with the same source in the same sequence continues it. A step is a damage
         * effect or a simultaneous damage loop ("2 damage to each creature and each player",
         * Volcanic Fallout's two loops).
         */
        private fun continuesDamageInstruction(leaf: Effect, path: List<Int>): Boolean {
            val damage = simultaneousDamageOf(leaf) ?: return false
            val index = path.lastOrNull() ?: return false
            if (index == 0) return false
            val previous = simultaneousDamageOf(scopes.last().composite.effects[index - 1]) ?: return false
            return previous.damageSource == damage.damageSource
        }

        /**
         * Leave the innermost open composite, the same way whether it opened in this run or before
         * a pause. Returns the program's result when that composite is the root; otherwise
         * restores the parent's context — its entry plus what the composite stored — and returns
         * null. An [aborted] composite (a failed cost) stores no values, but the object moves it
         * made stay followable.
         */
        private fun leave(aborted: Boolean): EffectResult? {
            val scope = scopes.removeAt(scopes.lastIndex)
            val scoped = events.subList(scope.eventStart, events.size).toList()
            // Moves this composite authorised before a pause travel in its outputs; a run that
            // never paused authorises the same moves from the composite's events as well.
            val stored = if (aborted) moves(scope.entry, scoped) else outputs(scope.entry, scoped)
            if (scopes.isEmpty()) return stored
            context = scope.entry.foldResult(stored)
            return null
        }

        /** What an abandoned run kept on top of [base]: only the object moves it authorised. */
        private fun moves(base: EffectContext, resultEvents: List<GameEvent>): EffectResult = EffectResult(
            state,
            resultEvents,
            updatedPermittedMoves = newPermits(base),
        )

        private fun newPermits(base: EffectContext) =
            context.objectReferences.permittedMoves.filterNot { it in base.objectReferences.permittedMoves }

        /** What the run stored on top of [base]: new pipeline keys and appended snapshots. */
        private fun outputs(base: EffectContext, resultEvents: List<GameEvent>): EffectResult = EffectResult(
            state,
            resultEvents,
            updatedCollections = context.pipeline.storedCollections - base.pipeline.storedCollections.keys,
            updatedSubtypeGroups = context.pipeline.storedSubtypeGroups - base.pipeline.storedSubtypeGroups.keys,
            updatedStoredNumbers = context.pipeline.storedNumbers - base.pipeline.storedNumbers.keys,
            updatedChosenValues = context.pipeline.chosenValues - base.pipeline.chosenValues.keys,
            updatedStringLists = context.pipeline.storedStringLists - base.pipeline.storedStringLists.keys,
            updatedSacrificedPermanents = context.sacrificedPermanents.drop(base.sacrificedPermanents.size),
            updatedDepartedPermanents = context.departedPermanents.drop(base.departedPermanents.size),
            updatedPermittedMoves = newPermits(base),
        )
    }

    companion object {
        private val stepNames = ConcurrentHashMap<Class<*>, String>()

        /**
         * The one name for a step: the SDK serial name of [type] when it has one, otherwise its
         * simple class name. Every [ProgramOrigin.BuiltBy] label is named through here.
         */
        fun stepName(type: KClass<*>): String = stepNames.computeIfAbsent(type.java) { cls ->
            runCatching { kotlinx.serialization.serializer(cls).descriptor.serialName }.getOrNull()
                ?: cls.simpleName
        }

        /** [stepName] of [effect]'s type. */
        fun stepName(effect: Effect): String = stepName(effect::class)

        /**
         * The origin a leaf [effect] runs with: `BuiltBy(<effect's step>, <the caller's origin>)`,
         * so a program the leaf starts or queues is labelled with the leaf that built it. The
         * registry sets it for each leaf call; a composite keeps the caller's label, which becomes
         * its program's origin.
         */
        fun originFor(effect: Effect, context: EffectContext): ProgramOrigin? =
            if (effect is CompositeEffect) context.programOrigin
            else ProgramOrigin.BuiltBy(stepName(effect), context.programOrigin)

        /**
         * A label for a root that [step] starts: `BuiltBy(<step>, parent)`. [parent] is the origin of
         * the program [step] runs inside, when the starting path has one to hand; null when [step]
         * starts outside any program (a mana ability, a special action) or its frame never
         * recorded one.
         */
        fun startedBy(step: KClass<*>, parent: ProgramOrigin? = null): ProgramOrigin = ProgramOrigin.BuiltBy(stepName(step), parent)

        /**
         * Fold a finished step's [result] into the context the steps after it run in: the object
         * moves its events authorise, then its pipeline outputs through the one merge
         * ([mergeOutputs]) that resumers' hand-offs use too.
         */
        fun EffectContext.foldResult(result: EffectResult): EffectContext =
            authorizeObjectMoves(result.events).mergeOutputs(result.pipelineOutputs, dedupSnapshots = false)

        /** The root step at child [index] of a program entered with [entry]: that entry, whole. */
        fun rootStep(index: Int, entry: EffectContext): CursorStep.Child = CursorStep.Child(index, rootDelta(entry))

        private fun rootDelta(entry: EffectContext) = EntryDelta(PipelineOutputs.of(entry), entry.resolutionDepth)

        /** The root composite's entry: this (current) context with the root delta's values instead. */
        private fun EffectContext.rebasedOn(delta: EntryDelta): EffectContext = copy(
            pipeline = pipeline.copy(
                storedCollections = delta.outputs.collections,
                storedSubtypeGroups = delta.outputs.subtypeGroups,
                storedNumbers = delta.outputs.numbers,
                chosenValues = delta.outputs.chosenValues,
                storedStringLists = delta.outputs.stringLists,
            ),
            sacrificedPermanents = delta.outputs.sacrificed,
            departedPermanents = delta.outputs.departed,
            objectReferences = objectReferences.copy(permittedMoves = delta.outputs.permits),
            resolutionDepth = delta.depth,
        )

        /** A nested composite's entry: its parent's entry (this) with [delta] applied. */
        private fun EffectContext.advancedBy(delta: EntryDelta): EffectContext =
            mergeOutputs(delta.outputs, dedupSnapshots = false).copy(resolutionDepth = resolutionDepth + delta.depth)
    }
}
