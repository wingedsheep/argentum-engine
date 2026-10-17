package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasBecomeTappedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PermanentAbilitiesActivatedThisTurnComponent
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
 * The position is read **net of mana spent**, which is the third shape. An action that costs mana
 * always changes *something* — a land is now tapped — so a paid no-op never repeats a position
 * exactly, and neither does a paid cycle. The AI spent every land it had re-equipping Well-Worn
 * Spatula to the creature already wearing it, and moving Equipment back and forth is the same
 * waste one step longer. Mana is the resource an action is paid *with*, not something it achieves,
 * so [digest] does not count it; see [manaSourcesOf].
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

        val manaSources = manaSourcesOf(state)
        var objects = 0L
        for ((key, contents) in state.zones) {
            // A library's order and object generations are carried by [normalized]. Its 60
            // cards have no components a game action touches without also moving them somewhere
            // this digest reads in full.
            if (key.zoneType == Zone.LIBRARY) continue
            for (entityId in contents) objects += objectHash(state, entityId, entityId in manaSources)
        }
        for (entityId in state.stack) objects += objectHash(state, entityId, spentMana = false)
        for (playerId in state.turnOrder) objects += objectHash(state, playerId, spentMana = false)
        return h.mix(objects)
    }

    /**
     * [state] with everything that is not a game fact zeroed out, so the data class's own
     * `hashCode` can supply the rest.
     *
     * A hand-written list of the fields to *read* was the first shape of this, and it had the
     * failure direction backwards. `GameState` carries ~50 fields and gains more; several are
     * turn-level riders an ability can set without touching a permanent — `spellCostModifiers`,
     * `activeCounterPlacementModifiers`, `pendingUncounterableSpells`,
     * `damageCantBePreventedThisTurn`. A field missing from a read-list makes a real action look
     * inert, and [Strategist] then refuses it *forever*. Naming the exclusions instead means a field
     * added tomorrow counts by default, and the worst a wrong entry here can do is cost one wasted
     * activation.
     *
     * What is stripped, and why none of it is a game fact:
     * - `entities` — read separately by [objectHash], which drops [IGNORED_COMPONENTS].
     * - `rng`, `nextEntityId`, `nextObjectGeneration`, `timestamp` — allocation/resolution bookkeeping.
     * - `nextRoutingId` — allocates question, delayed-trigger, and band references, not game facts.
     * - Orphan object identities — a completed stack object is not a live game object. Exact
     *   generations of physically present objects remain: a real zone round trip is progress.
     * - `priorityPlayerId`, `priorityPassedBy` — whose turn it is to speak, not what is true. This
     *   is what makes an action's own resolution comparable with the position it started from.
     * - `continuationStack` — counted instead; see [digest].
     *   Its derived `pendingDecision` disappears with the stack.
     *
     * `projectedState` is a body property rather than a constructor parameter, so it is already out
     * of `hashCode` — and would be redundant anyway, being a pure function of what is left.
     */
    private fun normalized(state: GameState): GameState {
        val liveObjects = buildSet {
            state.zones.values.forEach { addAll(it) }
            addAll(state.stack)
        }
        return state.copy(
            entities = emptyMap(),
            objectIdentities = state.objectIdentities.filterKeys { it in liveObjects },
            nextObjectGeneration = 0L,
            rng = GameRng(0L),
            nextEntityId = 0L,
            nextRoutingId = 0L,
            timestamp = 0L,
            priorityPlayerId = null,
            priorityPassedBy = emptySet(),
            continuationStack = emptyList(),
        )
    }

    /**
     * Everything the ECS records about one object, minus the ignored bookkeeping.
     *
     * The entity id is mixed with the component hash rather than added alongside it, so two objects
     * in the same zone trading component sets is a change rather than the same sum.
     *
     * [spentMana] marks one of the permanents [manaSourcesOf] picked out, whose tapped state is
     * not read.
     */
    private fun objectHash(state: GameState, entityId: EntityId, spentMana: Boolean): Long {
        val container = state.getEntity(entityId) ?: return 0L
        var components = 0L
        for (component in container.all()) {
            val type = component::class.java
            if (type in IGNORED_COMPONENTS) continue
            if (spentMana && component is TappedComponent) continue
            components += type.name.hashCode().toLong().mix(saturated(component).hashCode())
        }
        return entityId.hashCode().toLong().mix(components)
    }

    /**
     * The permanents whose tapped state is spent mana rather than a game fact: non-creature lands
     * and artifacts on the battlefield, read off the projection so an animated land or a crewed
     * Vehicle still counts as the creature it is.
     *
     * Tapping one of these is what paying a mana cost looks like on the board, and reading it
     * would make a paid no-op a fresh position every time — the guard would only ever catch free
     * ones. It did: the AI stopped re-equipping Well-Worn Spatula for {0} next to Dwarven Mauler,
     * then went on re-equipping it for {1} everywhere else until its lands ran out. Read net of
     * mana, "pay {1}, re-attach it where it already is" is exactly as inert as the free version,
     * and a paid back-and-forth exactly as circular as a free one.
     *
     * A mana *creature* is left out on purpose: tapping it does change the position (it can no
     * longer block), so the worst the omission costs is the one wasted activation this object
     * always accepts in that direction. The price of the reading is the other direction, the one
     * [IGNORED_COMPONENTS] warns about: an action whose whole effect is tapping or untapping a
     * non-creature land or artifact now reads as inert. That is rare off the stack — a spell moves
     * from hand to graveyard, nearly every ability taps or pays with something else, and a mana
     * ability is never a [Strategist] candidate to begin with.
     */
    private fun manaSourcesOf(state: GameState): Set<EntityId> {
        val battlefield = state.getBattlefield()
        if (battlefield.none { state.getEntity(it)?.has<TappedComponent>() == true }) return emptySet()
        val projected = state.projectedState
        return battlefield.filterTo(HashSet()) { id ->
            !projected.isCreature(id) && (projected.hasType(id, "LAND") || projected.hasType(id, "ARTIFACT"))
        }
    }

    private fun Long.mix(value: Int): Long = this * 0x100000001B3L xor value.toLong()

    private fun Long.mix(value: Long): Long = this * 0x100000001B3L xor value

    /**
     * "It happened" memories, as opposed to game position.
     *
     * Each of these records *that* an action was taken — the once-per-turn and `MaxPerTurn`
     * activation counts, Valiant's "has this been targeted by its controller yet this turn", the
     * per-permanent "has it already become tapped this turn" window Captain America, Living Legend
     * reads, and the timestamp bumped when a continuous effect is re-applied. An activation changes
     * them even when it changed nothing else, so reading them would make every inert action look
     * like progress — exactly the reading this object exists to avoid. Nothing is lost by the
     * omission: whatever these memories gate (a Valiant trigger, a first-time-tapped trigger, a
     * second activation being legal at all) shows up in the position the moment it actually does
     * something.
     *
     * [HasBecomeTappedComponent] is the reason Aphetto Alchemist's `{T}: Untap target` aimed at
     * itself has to be listed: the `{T}` cost stamps the marker and the untap does not clear it, so
     * without this entry the pay-its-own-cost-back no-op would read as a fresh position every time.
     *
     * [PermanentAbilitiesActivatedThisTurnComponent] is the player-side twin of
     * [AbilityActivatedThisTurnComponent]: every activation of a permanent's ability (mana sources
     * tapped by auto-pay included) records that permanent once, for Tezzeret, Betrayer of Flesh's
     * "first activated ability each turn" discount. The discount it gates shows up in the position
     * when a later activation is actually paid for.
     *
     * [ManaPoolComponent] is the pool half of spent mana — see [manaSourcesOf]. Mana left floating
     * after a payment is what an action was paid with, and the pool empties between steps anyway.
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
        HasBecomeTappedComponent::class.java,
        TimestampComponent::class.java,
        ManaPoolComponent::class.java,
        PermanentAbilitiesActivatedThisTurnComponent::class.java,
    )

    /**
     * The half-measure between counting a component and ignoring it: a tally whose readers only
     * ever ask "any yet?", clamped to `0` or `1` so its second and later increments are not a new
     * position.
     *
     * [IGNORED_COMPONENTS] is too blunt for [EquipActivationsThisTurnComponent]. Its `0 -> 1` step
     * is a game fact — Forge Anew's "you may pay {0} rather than pay the equip cost the first time
     * you activate an equip ability each turn" is exactly that boundary, and dropping the component
     * would tell the AI that using up the free equip changed nothing. Its `1 -> 2 -> 3 -> ...`
     * steps are not: `CastPermissionUtils.applyFreeFirstEquipDiscount` is the component's only
     * reader and it tests `count == 0`, so nothing in the game can distinguish a second equip
     * activation from a third. Clamping keeps the boundary and discards the rest.
     *
     * Which is what closes the loop this was written for. Equip's target is "target creature you
     * control" (CR 702.6a) with no exclusion for the creature the Equipment is already on, and
     * re-attaching it there "does nothing" (CR 701.3b) — so the activation is inert by rule. Put it
     * on a board that reduces the cost to {0} (The Hobbit's Dwarven Mauler discounting equip
     * abilities aimed at itself) and it is free as well, leaving nothing behind but this tally.
     * Under the unclamped count every repetition hashed as a fresh position, so [Strategist] had
     * nothing to refuse and an evaluator that preferred holding priority activated equip until the
     * game had to be abandoned. Equip stays legal — this is the AI declining a line, not the rules
     * forbidding one.
     *
     * `ExhaustAbilitiesActivatedThisTurnComponent` is the same activation-counter shape and is
     * deliberately *not* here: `PlayerActivatedExhaustAbilitiesThisTurn` carries an `atLeast`
     * threshold, so a card may yet ask whether the count reached two, and an exhaust ability can
     * only be activated once per permanent anyway (CR 702.177a) — there is no loop for it to feed.
     */
    private fun saturated(component: Component): Component = when (component) {
        is EquipActivationsThisTurnComponent ->
            if (component.count > 1) component.copy(count = 1) else component
        else -> component
    }

    private const val SEED = -0x340d631b7bdddcdbL
}
