package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedKeywordAbility
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.engine.state.components.battlefield.GraveyardEntryTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of the entire game state.
 *
 * The GameState is the single source of truth for the game.
 * All game operations are pure functions: (GameState, Action) -> (GameState, Events)
 */
@Serializable
data class GameState(
    /** All entities in the game, keyed by their ID */
    val entities: Map<EntityId, ComponentContainer> = emptyMap(),

    /** Zone contents - maps zone keys to lists of entity IDs */
    val zones: Map<ZoneKey, List<EntityId>> = emptyMap(),

    /** Current turn number (starts at 0, becomes 1 when first player's turn starts) */
    val turnNumber: Int = 0,

    /** ID of the player whose turn it is */
    val activePlayerId: EntityId? = null,

    /** Current phase */
    val phase: Phase = Phase.BEGINNING,

    /** Current step */
    val step: Step = Step.UNTAP,

    /** ID of the player who currently has priority */
    val priorityPlayerId: EntityId? = null,

    /** The stack (spells and abilities waiting to resolve) */
    val stack: List<EntityId> = emptyList(),

    /** Players who have passed priority in sequence */
    val priorityPassedBy: Set<EntityId> = emptySet(),

    /** Timestamp counter for ordering effects */
    val timestamp: Long = 0,

    /** Player IDs in turn order */
    val turnOrder: List<EntityId> = emptyList(),

    /** ID of the player who won (null if game ongoing) */
    val winnerId: EntityId? = null,

    /** Whether the game has ended */
    val gameOver: Boolean = false,

    /** Current pending decision awaiting player input (null if engine is not paused) */
    val pendingDecision: com.wingedsheep.engine.core.PendingDecision? = null,

    /** Active floating effects (temporary effects from spells like Giant Growth) */
    val floatingEffects: List<ActiveFloatingEffect> = emptyList(),

    /** Delayed triggers waiting to fire at specific steps */
    val delayedTriggers: List<DelayedTriggeredAbility> = emptyList(),

    /** Triggered abilities granted to entities temporarily (e.g., Commando Raid) */
    val grantedTriggeredAbilities: List<GrantedTriggeredAbility> = emptyList(),

    /** Activated abilities granted to entities temporarily (e.g., Run Wild) */
    val grantedActivatedAbilities: List<GrantedActivatedAbility> = emptyList(),

    /** Cast-keyword abilities granted to card entities temporarily (e.g., Songcrafter Mage grants Harmonize) */
    val grantedKeywordAbilities: List<GrantedKeywordAbility> = emptyList(),

    /** Global triggered abilities not attached to any permanent (e.g., False Cure) */
    val globalGrantedTriggeredAbilities: List<GlobalGrantedTriggeredAbility> = emptyList(),

    /** Continuation stack for resuming after player decisions */
    val continuationStack: List<ContinuationFrame> = emptyList(),

    /** Number of spells cast this turn (by all players), used for Storm count */
    val spellsCastThisTurn: Int = 0,

    /** Per-player spell count this turn, used for Damping Sphere-style tax effects */
    val playerSpellsCastThisTurn: Map<EntityId, Int> = emptyMap(),

    /** Per-player spell records cast this turn, for conditional evasion and "first of type" triggers */
    val spellsCastThisTurnByPlayer: Map<EntityId, List<CastSpellRecord>> = emptyMap(),

    /** Pending spell copies — copy the next instant/sorcery spell cast by a player (e.g., Howl of the Horde) */
    val pendingSpellCopies: List<PendingSpellCopy> = emptyList(),

    /** Pending "next spell can't be countered" riders — stamp the next matching spell cast (e.g., Mistrise Village) */
    val pendingUncounterableSpells: List<PendingUncounterableSpell> = emptyList(),

    /** Whether a spell was warped this turn (for Void condition: "a spell was warped this turn") */
    val spellWarpedThisTurn: Boolean = false,

    /** Whether a nonland permanent left the battlefield this turn (for the Void ability word). */
    val nonlandPermanentLeftBattlefieldThisTurn: Boolean = false,

    /**
     * Players (by entity id) who have committed a crime this turn (CR 700-level Outlaws of Thunder
     * Junction rule). Populated wherever a [com.wingedsheep.engine.core.CommitCrimeEvent] is emitted
     * (spell cast, activated ability, triggered ability), and cleared at every turn boundary. Read by
     * the `PlayerCommittedCrimeThisTurn` condition (e.g. Seize the Secrets' cost reduction).
     */
    val playersWhoCommittedCrimeThisTurn: Set<EntityId> = emptySet(),

    /**
     * Colors of the spell most recently cast this turn (by any player), or null if no spell has
     * been cast yet this turn. Used by Mana Maze's "can't cast a spell that shares a color with
     * the spell most recently cast this turn" restriction. Cleared at the start of each turn.
     */
    val lastCastSpellColors: Set<Color>? = null,

    /**
     * Per-player entity id of the card most recently drawn this turn (CR 121 draws), or
     * absent if that player has not drawn a card this turn. Updated at every
     * [com.wingedsheep.engine.core.CardsDrawnEvent] emit site during a turn (the *last*
     * id in a multi-card batch wins, per Scryfall ruling) and cleared at every turn
     * boundary. Read by the `DiscardLastDrawnThisTurn` ability cost (Jandor's Ring).
     */
    val lastCardDrawnThisTurnByPlayer: Map<EntityId, EntityId> = emptyMap(),

    /**
     * Per-player snapshot of [com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent]'s
     * count captured at the start of that player's most recent draw step (set by
     * [com.wingedsheep.engine.core.DrawPhaseManager.performDrawStep]). Used to identify the
     * *first* card a player draws in their own draw step (CR 504.1) — the card exempted by the
     * "except the first card they draw in each of their draw steps" clause (Orcish Bowmasters).
     * A draw in this draw step is the exempt one iff the player's cards-drawn-this-turn count
     * just before it equals this snapshot. Cleared at every turn boundary.
     */
    val drawStepStartDrawCountByPlayer: Map<EntityId, Int> = emptyMap(),

    /**
     * Game-mode configuration the engine reads for format-dependent behaviour (commander damage
     * threshold, command-zone redirect, etc.). Defaults to [Format.Standard] so existing
     * persisted states / tests need no migration.
     */
    val format: Format = Format.Standard,

    /**
     * Cumulative combat damage dealt by each commander to each player, keyed by
     * `(commanderEntityId, defendingPlayerId)`. Populated by `CombatDamageManager` at the
     * `DamageDealtEvent` emission sites for combat damage to a player. Read by the
     * `CommanderDamageLossCheck` SBA against [Format.Commander.commanderDamageThreshold].
     */
    val commanderDamage: List<CommanderDamageEntry> = emptyList(),

    /**
     * "You may play this card" permissions for cards in exile or other non-hand zones.
     * Lifecycle is owned by GameState (not the card) so granting permanents leaving play
     * does not invalidate a permission whose condition is still satisfied (Possibility
     * Technician). See [com.wingedsheep.engine.state.permissions.MayPlayPermission] for
     * field semantics.
     */
    val mayPlayPermissions: List<com.wingedsheep.engine.state.permissions.MayPlayPermission> = emptyList(),

    /**
     * Deterministic RNG state for every random game event (shuffles, coin flips, "at random"
     * choices). Threaded purely: a draw returns a value plus the advanced generator, which the
     * caller writes back via [nextRandom]. Two games seeded identically and fed the same actions
     * therefore reproduce byte-identically — see [GameRng]. Defaults to a fixed seed so existing
     * tests/persisted states are unaffected; [com.wingedsheep.engine.core.GameInitializer] reseeds
     * it from [com.wingedsheep.engine.core.GameConfig.seed] (or fresh entropy) at game start.
     */
    val rng: GameRng = GameRng(0L),

    /**
     * Monotonic counter backing deterministic entity-id minting via [newEntity]. Kept in state (not
     * a process-global) so id generation is a pure function of the game — a prerequisite for
     * byte-identical replays/parity. Live IDs look like `e0`, `e1`, … See [EntityId].
     */
    val nextEntityId: Long = 0L,
) {
    /**
     * Cached projection of the game state with all continuous effects (Rule 613) applied.
     * Evaluated exactly once per immutable GameState instance.
     * Not serialized — body properties are excluded from kotlinx.serialization.
     */
    val projectedState: ProjectedState by lazy {
        StateProjector().project(this)
    }

    // =========================================================================
    // Entity Operations
    // =========================================================================

    /**
     * Get an entity's components, or null if entity doesn't exist.
     */
    fun getEntity(id: EntityId): ComponentContainer? = entities[id]

    /**
     * Get an entity's components, throwing if entity doesn't exist.
     */
    fun requireEntity(id: EntityId): ComponentContainer =
        entities[id] ?: throw IllegalArgumentException("Entity not found: $id")

    /**
     * Check if an entity exists.
     */
    fun hasEntity(id: EntityId): Boolean = entities.containsKey(id)

    /**
     * Add or update an entity (returns new state).
     */
    fun withEntity(id: EntityId, container: ComponentContainer): GameState =
        copy(entities = entities + (id to container))

    /**
     * Remove an entity (returns new state).
     */
    fun withoutEntity(id: EntityId): GameState =
        copy(entities = entities - id)

    /**
     * Update an entity's components (returns new state).
     */
    fun updateEntity(id: EntityId, update: (ComponentContainer) -> ComponentContainer): GameState {
        val existing = entities[id] ?: ComponentContainer.EMPTY
        return withEntity(id, update(existing))
    }

    // =========================================================================
    // Zone Operations
    // =========================================================================

    /**
     * Get all entities in a zone.
     */
    fun getZone(key: ZoneKey): List<EntityId> = zones[key] ?: emptyList()

    /**
     * Get all entities in a player's zone.
     */
    fun getZone(playerId: EntityId, zoneType: Zone): List<EntityId> =
        getZone(ZoneKey(playerId, zoneType))

    /**
     * Add an entity to a zone (returns new state).
     * Automatically strips TappedComponent when moving to a non-battlefield zone,
     * since cards in the graveyard, exile, hand, or library are never tapped.
     */
    fun addToZone(key: ZoneKey, entityId: EntityId): GameState {
        val current = zones[key] ?: emptyList()
        var newState = copy(zones = zones + (key to current + entityId))
        if (key.zoneType != Zone.BATTLEFIELD && key.zoneType != Zone.STACK) {
            val container = newState.getEntity(entityId)
            if (container != null && container.get<TappedComponent>() != null) {
                newState = newState.updateEntity(entityId) { c -> c.without<TappedComponent>() }
            }
        }
        if (key.zoneType == Zone.GRAVEYARD) {
            newState = newState.updateEntity(entityId) { c ->
                c.with(GraveyardEntryTurnComponent(turnNumber))
            }
        }
        if (key.zoneType == Zone.EXILE) {
            newState = newState.updateEntity(entityId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent(turnNumber))
            }
        }
        return newState
    }

    /**
     * Remove an entity from a zone (returns new state).
     */
    fun removeFromZone(key: ZoneKey, entityId: EntityId): GameState {
        val current = zones[key] ?: return this
        return copy(zones = zones + (key to current - entityId))
    }

    /**
     * Move an entity between zones (returns new state).
     */
    fun moveToZone(entityId: EntityId, from: ZoneKey, to: ZoneKey): GameState {
        return removeFromZone(from, entityId).addToZone(to, entityId)
    }

    // =========================================================================
    // Query Helpers
    // =========================================================================

    /**
     * Find all entities with a specific component type.
     */
    inline fun <reified T : Component> findEntitiesWith(): List<Pair<EntityId, T>> {
        return entities.mapNotNull { (id, container) ->
            container.get<T>()?.let { id to it }
        }
    }

    /**
     * Find all entities matching a predicate.
     */
    fun findEntities(predicate: (EntityId, ComponentContainer) -> Boolean): List<EntityId> {
        return entities.filter { (id, container) -> predicate(id, container) }.keys.toList()
    }

    /**
     * Players still in the game, in turn order — excludes players who have lost or left
     * (CR 800.4a keeps them in [turnOrder] for entity history; every iteration helper
     * must skip them).
     */
    val activePlayers: List<EntityId>
        get() = turnOrder.filter {
            getEntity(it)?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
        }

    /**
     * Opponents of a player, in turn order, excluding players who have lost or left the
     * game. There is deliberately no single-opponent helper: any code that needs one
     * specific opponent must say which one (a chosen target, an iteration, or the
     * per-creature defending player — CR 802.2a).
     */
    fun getOpponents(playerId: EntityId): List<EntityId> =
        activePlayers.filter { it != playerId }

    /**
     * Returns the player who currently has *input authority* for [playerId] — that is,
     * who clicks the buttons and answers the decisions. Normally this is [playerId]
     * itself; during a Mindslaver-style hijacked turn this resolves to the hijacker.
     *
     * Resource ownership (mana, cards, life) is unaffected — it always stays with
     * [playerId]. This helper is only consulted at the input-routing seam: legal
     * action enumeration, decision validation, and per-action seat checks.
     *
     * A session-level [com.wingedsheep.engine.state.components.player.HotseatControlComponent]
     * (play-against-yourself) takes precedence over a per-turn hijack: it permanently routes
     * input authority to its controller for the whole game.
     */
    fun actorFor(playerId: EntityId): EntityId {
        val entity = getEntity(playerId) ?: return playerId
        entity.get<com.wingedsheep.engine.state.components.player.HotseatControlComponent>()
            ?.let { return it.controllerId }
        val hijack = entity
            .get<com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent>()
        return if (hijack != null &&
            hijack.state == com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent.HijackState.ACTIVE
        ) hijack.controllerId else playerId
    }

    // =========================================================================
    // Stack Operations
    // =========================================================================

    /**
     * Push an entity onto the stack (returns new state).
     */
    fun pushToStack(entityId: EntityId): GameState =
        copy(stack = stack + entityId)

    /**
     * Pop the top entity from the stack (returns entity ID and new state).
     */
    fun popFromStack(): Pair<EntityId?, GameState> {
        if (stack.isEmpty()) return null to this
        val top = stack.last()
        return top to copy(stack = stack.dropLast(1))
    }

    /**
     * Get the top entity on the stack, or null if empty.
     */
    fun getTopOfStack(): EntityId? = stack.lastOrNull()

    /**
     * True if the stack entity is a *spell* (a card on the stack, CR 112.1), as opposed
     * to an activated/triggered ability on the stack (CR 113.3b/c, 113.7a). The canonical
     * marker is [SpellOnStackComponent]; ability entities never carry it. Use this for
     * "target spell" enumeration/validation so abilities aren't offered as spell targets.
     */
    fun isSpellOnStack(entityId: EntityId): Boolean =
        getEntity(entityId)?.has<SpellOnStackComponent>() == true

    /**
     * Remove a specific entity from the stack (for countering).
     */
    fun removeFromStack(entityId: EntityId): GameState =
        copy(stack = stack - entityId)

    // =========================================================================
    // Convenience Zone Accessors
    // =========================================================================

    /**
     * Get all entities on the battlefield.
     *
     * Phased-out permanents (Rule 702.26) are excluded: while phased out a permanent
     * is treated as though it doesn't exist. This is the central seam — state
     * projection, trigger detection, combat, targeting enumeration, and state-based
     * actions all funnel through here, so excluding phased-out permanents here removes
     * them from every game-logic query at once. They physically remain in the
     * battlefield zone (see [allBattlefieldEntities]) and the client still renders them.
     */
    fun getBattlefield(): List<EntityId> = cachedBattlefield

    /**
     * Memoized backing for [getBattlefield]. Safe because [GameState] is immutable —
     * the (non-phased-out) battlefield set is constant for the lifetime of a state
     * instance, exactly like [projectedState]. Built in a single pass with one list
     * allocation instead of the `filterKeys` map copy + `flatten` + `filter` chain that
     * ran on every call. A body `val` is not serialized (only constructor params are).
     */
    private val cachedBattlefield: List<EntityId> by lazy {
        val result = ArrayList<EntityId>()
        for ((key, ids) in zones) {
            if (key.zoneType != Zone.BATTLEFIELD) continue
            for (id in ids) {
                if (entities[id]?.has<PhasedOutComponent>() != true) result.add(id)
            }
        }
        result
    }

    /**
     * Get all entities physically in the battlefield zone, **including phased-out
     * permanents**. Used by phase-in processing and client rendering, which must see
     * permanents that [getBattlefield] hides.
     */
    fun allBattlefieldEntities(): List<EntityId> {
        return zones.filterKeys { it.zoneType == Zone.BATTLEFIELD }
            .values.flatten()
    }

    /**
     * Get entities in a player's owner-keyed battlefield zone.
     *
     * **This returns OWNED entities, not CONTROLLED entities.** Control-changing
     * effects (Annex, Threaten, Mind Control) do not move entities between
     * owner-keyed zones — they apply a Layer 2 floating effect that the
     * [projectedState] reads. For game logic that asks "what does this player
     * control?", use [controlledBattlefield] instead.
     *
     * The zone map is the source of truth for **ownership**; controller queries
     * always go through projection (Architecture Principle §2.3).
     */
    fun getBattlefield(playerId: EntityId): List<EntityId> =
        getZone(playerId, Zone.BATTLEFIELD)

    /**
     * Get entities on the battlefield currently **controlled by** [playerId],
     * after Rule 613 continuous effects have been applied (Layer 2 control
     * changes). This is the correct view for almost all game logic — costs,
     * targeting, static-ability scans, replacement-effect dispatch, and so on.
     *
     * Convenience wrapper around [ProjectedState.getBattlefieldControlledBy]
     * that lets executors avoid spelling out `state.projectedState.…` at every
     * call site.
     */
    fun controlledBattlefield(playerId: EntityId): List<EntityId> =
        projectedState.getBattlefieldControlledBy(playerId)

    /**
     * Get a player's hand.
     */
    fun getHand(playerId: EntityId): List<EntityId> =
        getZone(playerId, Zone.HAND)

    /**
     * Get a player's library.
     */
    fun getLibrary(playerId: EntityId): List<EntityId> =
        getZone(playerId, Zone.LIBRARY)

    /**
     * Get a player's graveyard.
     */
    fun getGraveyard(playerId: EntityId): List<EntityId> =
        getZone(playerId, Zone.GRAVEYARD)

    /**
     * Get a player's exile zone.
     */
    fun getExile(playerId: EntityId): List<EntityId> =
        getZone(playerId, Zone.EXILE)

    /**
     * Remove an entity completely from the game (from all zones).
     */
    fun removeEntity(entityId: EntityId): GameState {
        var newState = withoutEntity(entityId)
        // Also remove from all zones
        zones.forEach { (key, ids) ->
            if (entityId in ids) {
                newState = newState.removeFromZone(key, entityId)
            }
        }
        // Also remove from stack if present
        if (entityId in stack) {
            newState = newState.removeFromStack(entityId)
        }
        return newState
    }

    // =========================================================================
    // State Transitions
    // =========================================================================

    /**
     * Advance to the next timestamp (returns new state).
     */
    fun tick(): GameState = copy(timestamp = timestamp + 1)

    /**
     * Draw a random value, advancing the stored generator. The [draw] lambda receives the current
     * [rng] and returns `(value, nextRng)` — e.g. `state.nextRandom { nextBoolean() }` for a coin
     * flip or `state.nextRandom { shuffle(library) }` for a shuffle. Returns the drawn value paired
     * with the new [GameState] carrying the advanced generator; the caller must thread that state
     * onward, exactly like [tick].
     */
    fun <T> nextRandom(draw: GameRng.() -> Pair<T, GameRng>): Pair<T, GameState> {
        val (value, advanced) = rng.draw()
        return value to copy(rng = advanced)
    }

    /**
     * Mint a fresh, deterministic [EntityId] and return it with the state whose counter has been
     * advanced. Replaces [EntityId.generate] anywhere the id must be reproducible across runs.
     */
    fun newEntity(): Pair<EntityId, GameState> =
        EntityId("e$nextEntityId") to copy(nextEntityId = nextEntityId + 1)

    /**
     * Set the priority player (returns new state).
     */
    fun withPriority(playerId: EntityId?): GameState =
        copy(priorityPlayerId = playerId, priorityPassedBy = emptySet())

    /**
     * Record that a player passed priority (returns new state).
     */
    fun withPriorityPassed(playerId: EntityId): GameState =
        copy(priorityPassedBy = priorityPassedBy + playerId)

    /**
     * Check if all players have passed priority.
     */
    fun allPlayersPassed(): Boolean = priorityPassedBy.containsAll(turnOrder)

    /**
     * Check if the engine is paused awaiting a decision.
     */
    fun isPaused(): Boolean = pendingDecision != null

    /**
     * Set a pending decision (pauses the engine).
     */
    fun withPendingDecision(decision: com.wingedsheep.engine.core.PendingDecision): GameState =
        copy(pendingDecision = decision)

    /**
     * Clear the pending decision (resumes the engine).
     */
    fun clearPendingDecision(): GameState =
        copy(pendingDecision = null)

    /**
     * Push a continuation frame onto the stack.
     * Used when pausing for a decision to remember how to resume.
     */
    fun pushContinuation(frame: ContinuationFrame): GameState =
        copy(continuationStack = continuationStack + frame)

    /**
     * Pop the top continuation frame from the stack.
     * Returns the frame and the new state, or null if stack is empty.
     */
    fun popContinuation(): Pair<ContinuationFrame?, GameState> {
        if (continuationStack.isEmpty()) return null to this
        val top = continuationStack.last()
        return top to copy(continuationStack = continuationStack.dropLast(1))
    }

    /**
     * Peek at the top continuation frame without removing it.
     */
    fun peekContinuation(): ContinuationFrame? = continuationStack.lastOrNull()

    /**
     * Add a delayed trigger to the state.
     */
    fun addDelayedTrigger(trigger: DelayedTriggeredAbility): GameState =
        copy(delayedTriggers = delayedTriggers + trigger)

    /**
     * Read the cumulative commander damage dealt by [commanderId] to [defendingPlayerId].
     */
    fun commanderDamageOf(commanderId: EntityId, defendingPlayerId: EntityId): Int =
        commanderDamage.firstOrNull {
            it.commanderId == commanderId && it.defendingPlayerId == defendingPlayerId
        }?.amount ?: 0

    /**
     * Increment cumulative commander damage from [commanderId] to [defendingPlayerId] by [amount].
     * Returns a new state with the updated tally. Negative or zero amounts are no-ops.
     */
    fun recordCommanderDamage(
        commanderId: EntityId,
        defendingPlayerId: EntityId,
        amount: Int,
    ): GameState {
        if (amount <= 0) return this
        val idx = commanderDamage.indexOfFirst {
            it.commanderId == commanderId && it.defendingPlayerId == defendingPlayerId
        }
        return if (idx >= 0) {
            val existing = commanderDamage[idx]
            val updated = commanderDamage.toMutableList()
            updated[idx] = existing.copy(amount = existing.amount + amount)
            copy(commanderDamage = updated)
        } else {
            copy(commanderDamage = commanderDamage + CommanderDamageEntry(commanderId, defendingPlayerId, amount))
        }
    }

    /**
     * Remove delayed triggers by their IDs.
     */
    fun removeDelayedTriggers(ids: Set<String>): GameState =
        copy(delayedTriggers = delayedTriggers.filter { it.id !in ids })

    /**
     * Get the next player in turn order after the given player.
     */
    fun getNextPlayer(afterPlayer: EntityId): EntityId {
        val index = turnOrder.indexOf(afterPlayer)
        return turnOrder[(index + 1) % turnOrder.size]
    }

    companion object {
        /**
         * Create an initial game state for a new game.
         */
        fun initial(playerIds: List<EntityId>): GameState {
            require(playerIds.size >= 2) { "Need at least 2 players" }
            return GameState(
                turnOrder = playerIds,
                activePlayerId = playerIds.first(),
                priorityPlayerId = playerIds.first()
            )
        }
    }
}

/**
 * Key for identifying a specific zone (player + zone type).
 */
@Serializable
data class ZoneKey(
    val ownerId: EntityId,
    val zoneType: Zone
) {
    override fun toString(): String = "${ownerId.value}:${zoneType.name}"
}

/**
 * Cumulative combat damage dealt by a single commander to a single player. Stored as a list of
 * entries (rather than a `Map<Pair<EntityId, EntityId>, Int>`) so kotlinx.serialization can
 * round-trip the state — map keys must be primitives.
 */
@Serializable
data class CommanderDamageEntry(
    val commanderId: EntityId,
    val defendingPlayerId: EntityId,
    val amount: Int,
)

/**
 * Snapshot of a spell's card characteristics at cast time,
 * used for retroactive filter matching (e.g., "did you cast a historic spell this turn?").
 *
 * [paidWithTreasureMana] captures whether any of the mana spent to cast the spell was
 * added by tapping a Treasure (see Rain of Riches, "the first spell you cast each turn
 * that mana from a Treasure was spent to cast").
 *
 * [sourceEntityId] is the entity id of the spell on the stack (the card entity that was cast).
 * It lets a resolving spell exclude its own record from a count of spells cast this turn —
 * e.g. Thunder Salvo's "the number of *other* spells you've cast this turn". Null for synthetic
 * records constructed only for retroactive filter matching.
 *
 * [castFromZone] is the zone the spell was cast from (HAND for a normal cast, GRAVEYARD for
 * flashback/forage, EXILE for a plotted/foretold/impulse cast, COMMAND for a commander, …). It
 * lets "you haven't cast a spell *from your hand* this turn" (Inventive Wingsmith, Prairie Dog,
 * Canyon Crab, Emergent Haunting, Wrangler of the Damned) distinguish hand casts from casts
 * originating in any other zone. Null for synthetic records and for casts whose origin zone
 * couldn't be determined.
 */
@Serializable
data class CastSpellRecord(
    val typeLine: TypeLine,
    val manaValue: Int,
    val colors: Set<Color>,
    val isFaceDown: Boolean,
    val paidWithTreasureMana: Boolean = false,
    val sourceEntityId: EntityId? = null,
    val castFromZone: Zone? = null,
)
