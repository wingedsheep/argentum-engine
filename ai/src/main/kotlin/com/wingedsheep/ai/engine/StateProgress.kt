package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng

/**
 * "Is this position one I have already been in?" — how the AI tells progress from a treadmill.
 *
 * Two shapes of the same bug live here. The first is an **inert** action, one whose resolved
 * position is the position it started from: Aphetto Alchemist's `{T}: Untap target artifact or
 * creature` aimed at itself taps for its own cost and then untaps itself again. The second is a
 * **cycle** — two Alchemists untapping each other — where every single step does change the board
 * and the sequence still goes nowhere.
 *
 * A leaf-scoring search has no defence against either on its own. It compares "take this action"
 * against "pass" by scoring the two positions they lead to, and those two positions are not
 * measured at the same point in the game: passing carries the game forward into whatever was about
 * to happen, while a free ability that resolves back onto the same board stops right where it is.
 * When what is about to happen is bad, doing nothing at all can therefore *outscore* passing — and
 * once it does, it does so again from the identical position it just produced, forever. That is the
 * bug this exists to prevent: the AI activated Aphetto Alchemist until the game had to be
 * abandoned.
 *
 * So this is not a heuristic about value — the evaluator owns that question. It is the structural
 * claim underneath it, and the rules make the same one: CR 732.3 requires a player whose actions
 * have "resulted in the same game state being reached multiple times" to make a *different* game
 * choice, and its example turns on the very thing [IGNORED_COMPONENTS] does — the loop repeats a
 * position when "nothing in the game cares how many times an ability has been activated."
 *
 * [Strategist] is the consumer: it drops any candidate whose leaf repeats a position it has already
 * acted from.
 *
 * **This is deliberately not behind an [AiProfile] flag**, which is the one place this codebase
 * normally insists on one — `FrozenBaselineTest` exists to stop `AiProfile.LEGACY_V0` drifting, and
 * Phase 4's `fillPartial` is gated for no other reason. The rule is about *strength*: a change that
 * makes V0 play better silently rebases every published arena number. A game that has to be
 * abandoned has no strength to compare, so there is nothing here worth freezing. `FrozenBaselineTest`
 * stays green because its baseline deck is all-vanilla — no activated abilities, so nothing this
 * guard can fire on — which means it is not evidence either way and shouldn't be read as any.
 */
object StateProgress {

    /**
     * A 64-bit summary of everything about [state] that a player could point at.
     *
     * `GameState` equality is unusable for this. A resolved ability leaves behind an orphaned stack
     * entity, a bumped `nextEntityId` and an advanced `rng`, none of which is a game fact — so this
     * reads the position the way a player would: everything `GameState` itself records, minus the
     * bookkeeping [normalized] strips, plus everything true of the objects and players in the zones.
     *
     * Turn number and step *are* part of it, which is what keeps the repetition memory honest: the
     * same board in a later step is a different position, so a digest can only recur inside the
     * window where recurring means going in circles.
     *
     * Per-object hashes are summed rather than folded, so iteration order can't read as a change;
     * order *within* a zone still counts, because library and stack order are game facts, and it is
     * `zones` inside [normalized] that carries it.
     */
    fun digest(state: GameState): Long {
        var h = SEED.mix(normalized(state).hashCode())
        // Continuation frames are counted, not read. A frame holds mid-resolution bookkeeping —
        // the stack entity being resolved, indices into it — that a re-resolution mints afresh, so
        // hashing their contents would make an inert action look like progress. Depth alone is
        // enough here because the states this is asked to compare are quiet ones: a leaf that
        // paused mid-resolution comes back as `SimulationResult.NeedsDecision`, which [Strategist]
        // never digests.
        h = h.mix(state.continuationStack.size)

        var objects = 0L
        for ((key, contents) in state.zones) {
            // A library is hashed by its order alone — carried by `zones` in [normalized]. Its 60
            // cards have no components a game action touches without also moving them somewhere
            // this digest reads in full.
            if (key.zoneType == Zone.LIBRARY) continue
            for (entityId in contents) objects += objectHash(state, entityId)
        }
        for (entityId in state.stack) objects += objectHash(state, entityId)
        for (playerId in state.turnOrder) objects += objectHash(state, playerId)
        return h.mix(objects)
    }

    /**
     * [state] with everything that is not a game fact zeroed out, so the data class's own
     * `hashCode` can supply the rest.
     *
     * A hand-written list of the fields to *read* was the first shape of this, and it had the
     * failure direction backwards. `GameState` carries ~50 fields and gains more; several are
     * turn-level riders an ability can set without touching a permanent — `turnSpellCostReductions`,
     * `activeCounterPlacementModifiers`, `pendingUncounterableSpells`,
     * `damageCantBePreventedThisTurn`. A field missing from a read-list makes a real action look
     * inert, and [Strategist] then refuses it *forever*. Naming the exclusions instead means a field
     * added tomorrow counts by default, and the worst a wrong entry here can do is cost one wasted
     * activation.
     *
     * What is stripped, and why none of it is a game fact:
     * - `entities` — read separately by [objectHash], which drops [IGNORED_COMPONENTS].
     * - `rng`, `nextEntityId`, `timestamp` — advanced by resolving anything at all.
     * - `priorityPlayerId`, `priorityPassedBy` — whose turn it is to speak, not what is true. This
     *   is what makes an action's own resolution comparable with the position it started from.
     * - `continuationStack` — counted instead; see [digest].
     * - `pendingDecision` — the same mid-resolution bookkeeping, and never set on a quiet state.
     *
     * `projectedState` is a body property rather than a constructor parameter, so it is already out
     * of `hashCode` — and would be redundant anyway, being a pure function of what is left.
     */
    private fun normalized(state: GameState): GameState = state.copy(
        entities = emptyMap(),
        rng = GameRng(0L),
        nextEntityId = 0L,
        timestamp = 0L,
        priorityPlayerId = null,
        priorityPassedBy = emptySet(),
        continuationStack = emptyList(),
        pendingDecision = null,
    )

    /**
     * Everything the ECS records about one object, minus the ignored bookkeeping.
     *
     * The entity id is mixed with the component hash rather than added alongside it, so two objects
     * in the same zone trading component sets is a change rather than the same sum.
     */
    private fun objectHash(state: GameState, entityId: EntityId): Long {
        val container = state.getEntity(entityId) ?: return 0L
        var components = 0L
        for (component in container.all()) {
            val type = component::class.java
            if (type in IGNORED_COMPONENTS) continue
            components += type.name.hashCode().toLong().mix(component.hashCode())
        }
        return entityId.hashCode().toLong().mix(components)
    }

    private fun Long.mix(value: Int): Long = this * 0x100000001B3L xor value.toLong()

    private fun Long.mix(value: Long): Long = this * 0x100000001B3L xor value

    /**
     * "It happened" memories, as opposed to game position.
     *
     * Each of these records *that* an action was taken — the once-per-turn and `MaxPerTurn`
     * activation counts, Valiant's "has this been targeted by its controller yet this turn", and the
     * timestamp bumped when a continuous effect is re-applied. An activation changes them even when
     * it changed nothing else, so reading them would make every inert action look like progress —
     * exactly the reading this object exists to avoid. Nothing is lost by the omission: whatever
     * these memories gate (a Valiant trigger, a second activation being legal at all) shows up in
     * the position the moment it actually does something.
     *
     * The list is a floor, not a ceiling: a memory component not named here makes an inert action
     * read as progress, so the AI takes it once more than it should. Which is why it fails in that
     * direction — a *missing* entry costs a wasted activation, whereas wrongly ignoring something
     * real would cost the AI an ability it should have used.
     */
    private val IGNORED_COMPONENTS = setOf<Class<*>>(
        AbilityActivatedThisTurnComponent::class.java,
        AbilityActivatedEverComponent::class.java,
        TargetedByControllerThisTurnComponent::class.java,
        TimestampComponent::class.java,
    )

    private const val SEED = -0x340d631b7bdddcdbL
}
