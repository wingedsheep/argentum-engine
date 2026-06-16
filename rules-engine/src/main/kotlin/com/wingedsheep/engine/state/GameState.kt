package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.event.GrantedKeywordAbility
import com.wingedsheep.engine.event.GrantedStaticAbility
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
import com.wingedsheep.sdk.scripting.AbilityIdentity
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

    /** Static abilities granted to entities temporarily (e.g., Full Steam Ahead) */
    val grantedStaticAbilities: List<GrantedStaticAbility> = emptyList(),

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
     * Active temporary counter-placement *modifiers* — the duration-scoped, controller-scoped
     * analogue of a battlefield [com.wingedsheep.sdk.scripting.ModifyCounterPlacement] replacement
     * (Hardened Scales). Installed by
     * [com.wingedsheep.sdk.scripting.effects.GrantCounterPlacementModifierEffect] (e.g. Prairie
     * Dog's "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature
     * you control, put that many plus one instead"). Consulted from the single counter-placement
     * chokepoint
     * ([com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils.applyCounterPlacementModifiers])
     * and expired per-entry by `CleanupPhaseManager.cleanupEndOfTurn` (and re-cleared at each turn
     * boundary as a safety net). See [ActiveCounterPlacementModifier].
     */
    val activeCounterPlacementModifiers: List<ActiveCounterPlacementModifier> = emptyList(),

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
     * Which opponents a creature may attack in this game (CR 802 / 803). Chosen in the lobby for
     * Free-for-All games (CR 806.2b requires exactly one of the three). Defaults to
     * [AttackMode.MULTIPLE] so two-player games and existing callers are unaffected — in a
     * two-player game all three modes behave identically.
     */
    val attackMode: com.wingedsheep.sdk.core.AttackMode = com.wingedsheep.sdk.core.AttackMode.MULTIPLE,

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

    /**
     * Per-player persistent "yield" preferences keyed by [com.wingedsheep.sdk.scripting.AbilityIdentity]
     * (MTGO right-click yields — see `backlog/stack-collapse-and-batch-decisions.md` §C). Lives on
     * [GameState] (not the server session) so it survives serialization, replays deterministically,
     * and is naturally per-player-maskable. The `untilEndOfTurn` slice is cleared at every cleanup
     * step (CR 514); `wholeGame` / `autoAnswer` persist for the whole game. Read by
     * [com.wingedsheep.gameserver.priority.AutoPassManager] (auto-pass) and the may-question paths
     * (auto-answer). Empty entries are pruned, so an absent key means "no yields".
     */
    val yieldsByPlayer: Map<EntityId, PlayerYields> = emptyMap(),
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
    // Persistent Yields (MTGO right-click yields — backlog §C)
    // =========================================================================

    /** [playerId]'s yield preferences, or [PlayerYields.EMPTY] if none. */
    fun yieldsFor(playerId: EntityId): PlayerYields = yieldsByPlayer[playerId] ?: PlayerYields.EMPTY

    /** True when [playerId] has asked to auto-pass priority on [identity]'s stack objects. */
    fun isYieldingTo(playerId: EntityId, identity: AbilityIdentity): Boolean =
        yieldsFor(playerId).isYieldingTo(identity)

    /**
     * [playerId]'s remembered may-question answer for [identity] (`true`/`false`), or null if the
     * player hasn't set an auto-answer for this ability.
     */
    fun autoAnswerFor(playerId: EntityId, identity: AbilityIdentity): Boolean? =
        yieldsFor(playerId).answerFor(identity)

    /** Replace [playerId]'s yields, pruning the entry entirely when it becomes empty. */
    private fun withYields(playerId: EntityId, yields: PlayerYields): GameState =
        copy(yieldsByPlayer = if (yields.isEmpty) yieldsByPlayer - playerId else yieldsByPlayer + (playerId to yields))

    /** Apply a [YieldKind] for [playerId] against [identity] (returns new state). */
    fun withYield(playerId: EntityId, identity: AbilityIdentity, kind: YieldKind): GameState {
        val current = yieldsFor(playerId)
        val updated = when (kind) {
            YieldKind.YIELD_UNTIL_END_OF_TURN -> current.copy(untilEndOfTurn = current.untilEndOfTurn + identity)
            YieldKind.YIELD_WHOLE_GAME -> current.copy(wholeGame = current.wholeGame + identity)
            YieldKind.ALWAYS_ANSWER_YES -> current.copy(autoAnswer = current.autoAnswer + (identity to true))
            YieldKind.ALWAYS_ANSWER_NO -> current.copy(autoAnswer = current.autoAnswer + (identity to false))
        }
        return withYields(playerId, updated)
    }

    /** Remove every yield [playerId] holds against [identity] (revoke), returning new state. */
    fun withoutYield(playerId: EntityId, identity: AbilityIdentity): GameState {
        val current = yieldsFor(playerId)
        return withYields(
            playerId,
            current.copy(
                untilEndOfTurn = current.untilEndOfTurn - identity,
                wholeGame = current.wholeGame - identity,
                autoAnswer = current.autoAnswer - identity,
            ),
        )
    }

    /** Drop all of [playerId]'s yields (the "clear yields" control). */
    fun withoutYields(playerId: EntityId): GameState = copy(yieldsByPlayer = yieldsByPlayer - playerId)

    /** Clear every player's [PlayerYields.untilEndOfTurn] slice (turn-boundary cleanup, CR 514). */
    fun clearUntilEndOfTurnYields(): GameState {
        if (yieldsByPlayer.isEmpty()) return this
        val cleared = yieldsByPlayer
            .mapValues { (_, y) -> y.copy(untilEndOfTurn = emptySet()) }
            .filterValues { !it.isEmpty }
        return copy(yieldsByPlayer = cleared)
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
    fun getOpponents(playerId: EntityId): List<EntityId> {
        // CR 810: an opponent is a player on an opposing team, so exclude the player's whole team
        // (themselves and any teammates), not just themselves. In a non-team game a player is its
        // own team, so this is identical to "everyone but me".
        val ownTeam = teamOf(playerId).toHashSet()
        return activePlayers.filter { it !in ownTeam }
    }

    // =========================================================================
    // Teams (Two-Headed Giant and other team variants — CR 810)
    //
    // Team membership lives on the player entity as a
    // [com.wingedsheep.engine.state.components.identity.TeamComponent]; these helpers are the
    // single read surface for it. A player without that component is its own team, so every helper
    // degrades to per-player behaviour in non-team formats — `teamOf(p) == [p]`,
    // `teammatesOf(p) == []`, and `teams` is one singleton list per player. That is what keeps
    // future team-aware turn/priority/combat/SBA code a no-op for ordinary games.
    // =========================================================================

    /**
     * The game's teams, in turn order. Players sharing a `TeamComponent.teamIndex` are grouped into
     * one list; a player without the component forms a singleton team. Teams appear in the order
     * their first member appears in [turnOrder], and members keep turn order within a team. Includes
     * players who have lost or left — filter with [teamActivePlayers] when liveness matters.
     */
    val teams: List<List<EntityId>>
        get() {
            val ordered = mutableListOf<MutableList<EntityId>>()
            val byIndex = mutableMapOf<Int, MutableList<EntityId>>()
            for (playerId in turnOrder) {
                val idx = getEntity(playerId)
                    ?.get<com.wingedsheep.engine.state.components.identity.TeamComponent>()?.teamIndex
                if (idx == null) {
                    ordered.add(mutableListOf(playerId))
                } else {
                    val team = byIndex.getOrPut(idx) { mutableListOf<EntityId>().also { ordered.add(it) } }
                    team.add(playerId)
                }
            }
            return ordered
        }

    /**
     * Every player on [playerId]'s team, including [playerId] itself, in turn order. Returns just
     * `[playerId]` when it has no [com.wingedsheep.engine.state.components.identity.TeamComponent]
     * (its own team). Includes lost/left teammates.
     */
    fun teamOf(playerId: EntityId): List<EntityId> {
        val idx = getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.identity.TeamComponent>()?.teamIndex
            ?: return listOf(playerId)
        return turnOrder.filter {
            getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.TeamComponent>()?.teamIndex == idx
        }
    }

    /**
     * [playerId]'s teammates — its team minus itself, in turn order. Empty in non-team formats.
     */
    fun teammatesOf(playerId: EntityId): List<EntityId> =
        teamOf(playerId).filter { it != playerId }

    /**
     * Members of [playerId]'s team still in the game (not lost/left), in turn order. The team-level
     * analogue of [activePlayers].
     */
    fun teamActivePlayers(playerId: EntityId): List<EntityId> =
        teamOf(playerId).filter {
            getEntity(it)?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
        }

    /**
     * The players who act together as one side on the active player's turn — every still-in
     * teammate in a shared-team-turns format (CR 805.4 / 805.10: untap, draw, attack, block as a
     * unit), otherwise just [playerId] itself. This is the single read surface for "who collaborates
     * this turn"; it degrades to `[playerId]` whenever [Format.sharesTeamTurns] is false (Free-for-All,
     * **Team vs. Team**, 1v1, Commander), so a player on a Team-vs-Team team still untaps/draws/
     * attacks/blocks strictly alone even though they have teammates.
     */
    fun sharedTurnTeam(playerId: EntityId): List<EntityId> =
        if (format.sharesTeamTurns) teamActivePlayers(playerId) else listOf(playerId)

    /**
     * Teams with at least one player still in the game (not lost/left), in turn order. The
     * team-level analogue of [activePlayers] — the game ends when only one of these remains
     * (CR 810.8a). In a non-team game each player is its own team, so this mirrors [activePlayers].
     */
    val activeTeams: List<List<EntityId>>
        get() = teams.filter { team ->
            team.any { getEntity(it)?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true }
        }

    /**
     * The shared poison-counter total for [playerId]'s team (CR 810.10 — poison counters are placed
     * on players individually but pooled by the team). Summed across all team members. For a player
     * with no team this is just that player's own poison count.
     */
    fun teamPoison(playerId: EntityId): Int =
        // Poison pools by team only when the team also shares its life total (CR 810.10 is part of
        // the 2HG shared-pool rules). In Team vs. Team and every non-pooled format each player has
        // their own poison total.
        (if (format.sharesTeamLife) teamOf(playerId) else listOf(playerId)).sumOf { member ->
            getEntity(member)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?.getCount(com.wingedsheep.sdk.core.CounterType.POISON) ?: 0
        }

    // =========================================================================
    // Shared team turns (Two-Headed Giant — CR 805 / 810.2)
    //
    // A team takes ONE turn together (CR 805.4): both members untap and draw (805.4b), each may
    // play a land (805.4c), and either may take sorcery-speed actions while it is the team's turn
    // (805.5a — a player may act when their team has priority). The engine keeps a single
    // [activePlayerId] (CR 805.9 — "active player" is one specific player), but turn *ownership* —
    // "is it this player's turn?" — is a team question answered by [isActiveTurnFor]. Priority still
    // cycles per player, so each teammate gets their own window and the phase advances only once
    // everyone has passed; that already yields the shared-turn outcome.
    // =========================================================================

    /**
     * True when it is [playerId]'s team's turn — i.e. [playerId] is on the active team. The
     * team-aware replacement for `activePlayerId == playerId` at every turn-ownership / sorcery-speed
     * gate. In a non-team game the active team is just the active player, so this reduces to equality.
     */
    fun isActiveTurnFor(playerId: EntityId): Boolean {
        val active = activePlayerId ?: return false
        // Only a shared-team-turns format (CR 805.5a) lets a teammate share turn ownership; in Team
        // vs. Team (CR 808.4) and every non-team format the active turn belongs to one player.
        return if (format.sharesTeamTurns) teamOf(active).contains(playerId) else active == playerId
    }

    /**
     * The representative (first still-in member) of the team that takes its turn after [afterPlayer]'s
     * team — the team-level analogue of [getNextPlayer]. Fully-eliminated teams are skipped. For a
     * player with no team this is identical to [getNextPlayer]. Used by turn advancement so the turn
     * passes to the next *team*, not to a teammate who shares the same turn (CR 805.4).
     */
    fun getNextTeam(afterPlayer: EntityId): EntityId {
        // Without shared team turns (Team vs. Team — CR 808.4, Free-for-All, 1v1) each player takes
        // their own turn, so the turn simply passes to the next still-in player.
        if (!format.sharesTeamTurns) return getNextPlayer(afterPlayer)
        val teamsList = teams
        if (teamsList.isEmpty()) return afterPlayer
        val curIdx = teamsList.indexOfFirst { afterPlayer in it }
        val n = teamsList.size
        for (step in 1..n) {
            val team = teamsList[((curIdx + step) % n + n) % n]
            val rep = team.firstOrNull {
                getEntity(it)?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
            }
            if (rep != null) return rep
        }
        return afterPlayer
    }

    // =========================================================================
    // Shared life total (Two-Headed Giant — CR 810.4 / 810.9)
    //
    // A team has ONE life total (CR 810.4); "if a cost or effect needs an individual player's life
    // total, it uses the team's life total instead" (CR 810.9a). We model this by storing the single
    // [LifeTotalComponent] for a team on a stable *canonical owner* — the first member of the team in
    // turn order ([teamLifeOwnerOf]) — and resolving every life read/write to that owner.
    //
    // Every player still carries a [LifeTotalComponent] so player-detection (`has<LifeTotalComponent>`)
    // and entity shape are unchanged; only the canonical owner's value is authoritative. A
    // non-canonical teammate's component value is never read — all reads go through [lifeTotal] and
    // all writes through [withLifeTotal] / [adjustLife], which target the owner. In a non-team game a
    // player is its own team, so the owner is the player itself and these helpers are a pure pass-through.
    // =========================================================================

    /**
     * The entity that holds [playerId]'s team's authoritative [LifeTotalComponent] — the first
     * member of [teamOf] (stable across the game; team membership and turn order never change). For
     * a player with no team this is the player itself.
     */
    fun teamLifeOwnerOf(playerId: EntityId): EntityId =
        // Only a shared-life format (2HG — CR 810.4/810.9) routes every member's life to one
        // canonical owner. Team vs. Team (CR 808.5) and every non-team format keep per-player life.
        if (format.sharesTeamLife) teamOf(playerId).firstOrNull() ?: playerId else playerId

    /**
     * [playerId]'s life total — the team's shared total in a team game (CR 810.9a), the player's own
     * total otherwise. Returns 0 if the owner somehow has no [LifeTotalComponent] (should not happen
     * for a real player).
     */
    fun lifeTotal(playerId: EntityId): Int =
        getEntity(teamLifeOwnerOf(playerId))
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0

    /**
     * Set [playerId]'s (team's) life total to [newLife], writing the canonical owner's component.
     * Low-level — callers still emit the appropriate `LifeChangedEvent` (attributed to the
     * individual player per CR 810.9) and run any prevention/replacement before computing [newLife].
     */
    fun withLifeTotal(playerId: EntityId, newLife: Int): GameState =
        updateEntity(teamLifeOwnerOf(playerId)) { container ->
            container.with(
                com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife)
            )
        }

    /**
     * Convenience: adjust [playerId]'s (team's) life by [delta] (negative to lose), returning the new
     * state. Equivalent to `withLifeTotal(playerId, lifeTotal(playerId) + delta)`.
     */
    fun adjustLife(playerId: EntityId, delta: Int): GameState =
        withLifeTotal(playerId, lifeTotal(playerId) + delta)

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
     *
     * CR 800.4a / 800.4j: priority that would be given to a player who has left the game
     * passes instead to the next player still in the game. Routing every priority
     * assignment through here means the rest of the engine never has to special-case a
     * departed active player — a turn whose active player has left simply hands each
     * priority window to the next remaining player.
     */
    fun withPriority(playerId: EntityId?): GameState =
        copy(priorityPlayerId = redirectPriorityIfLeft(playerId), priorityPassedBy = emptySet())

    private fun redirectPriorityIfLeft(playerId: EntityId?): EntityId? {
        if (playerId == null) return null
        val hasLeft = getEntity(playerId)
            ?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() == true
        return if (hasLeft) getNextPlayer(playerId) else playerId
    }

    /**
     * Record that a player passed priority (returns new state).
     */
    fun withPriorityPassed(playerId: EntityId): GameState =
        copy(priorityPassedBy = priorityPassedBy + playerId)

    /**
     * Check if all players still in the game have passed priority. Players who have
     * left the game (CR 800.4a) never receive priority, so they are excluded — checking
     * against the full [turnOrder] would deadlock a multiplayer game once a player leaves.
     */
    fun allPlayersPassed(): Boolean = priorityPassedBy.containsAll(activePlayers)

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
     * Get the next player still in the game in turn order after the given player.
     *
     * Players who have left the game (CR 800.4a) are skipped: priority that would pass
     * to a departed player goes to the next remaining player (CR 800.4a), and a turn a
     * departed player would begin doesn't begin — the next remaining player's turn is
     * next instead (CR 800.4k). If everyone else has left, returns [afterPlayer]
     * (the game-end SBA resolves the single survivor).
     */
    fun getNextPlayer(afterPlayer: EntityId): EntityId {
        val size = turnOrder.size
        if (size == 0) return afterPlayer
        val start = turnOrder.indexOf(afterPlayer)
        for (step in 1..size) {
            val candidate = turnOrder[((start + step) % size + size) % size]
            if (getEntity(candidate)
                    ?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
            ) {
                return candidate
            }
        }
        return afterPlayer
    }

    /**
     * Get the previous player still in the game in turn order before the given player — the
     * mirror of [getNextPlayer], walking seats the other way. Players who have left the game
     * (CR 800.4a) are skipped. Used by the attack-right option (CR 803.1b): the player to your
     * right is the previous remaining seat in turn order. Returns [beforePlayer] if everyone
     * else has left.
     */
    fun getPreviousPlayer(beforePlayer: EntityId): EntityId {
        val size = turnOrder.size
        if (size == 0) return beforePlayer
        val start = turnOrder.indexOf(beforePlayer)
        for (step in 1..size) {
            val candidate = turnOrder[((start - step) % size + size) % size]
            if (getEntity(candidate)
                    ?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
            ) {
                return candidate
            }
        }
        return beforePlayer
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

/**
 * A temporary, duration-scoped counter-placement *modifier* — the activated/spell-granted,
 * time-bounded analogue of the static [com.wingedsheep.sdk.scripting.ModifyCounterPlacement]
 * replacement (Hardened Scales). Created by
 * [com.wingedsheep.sdk.scripting.effects.GrantCounterPlacementModifierEffect] and stored on
 * [GameState.activeCounterPlacementModifiers].
 *
 * Controller-scoped exactly like the static version: [controllerId] is the player who controls
 * the effect, and the [recipient] filter ("a creature you control") and the placer gate both
 * resolve relative to *that* player — not to a battlefield permanent's controller. The entry is
 * consulted from the single counter-placement chokepoint
 * ([com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils.applyCounterPlacementModifiers])
 * alongside the battlefield `ModifyCounterPlacement` scan, and is removed when [duration] expires
 * (typically end of turn) by `CleanupPhaseManager.cleanupEndOfTurn`.
 *
 * @property modifier Additional counters placed (negative reduces; chokepoint floors at 0).
 * @property controllerId The player who controls this modifier ("you").
 * @property counterType Which counter kind the modifier applies to.
 * @property recipient Which recipients the modifier applies to, relative to [controllerId].
 * @property duration How long the modifier stays active.
 */
@Serializable
data class ActiveCounterPlacementModifier(
    val modifier: Int,
    val controllerId: EntityId,
    val counterType: com.wingedsheep.sdk.scripting.events.CounterTypeFilter,
    val recipient: com.wingedsheep.sdk.scripting.events.RecipientFilter,
    val duration: com.wingedsheep.sdk.scripting.Duration,
)
