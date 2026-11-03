package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A resolved "restart the game" effect (CR 727), waiting for its resolution to finish. Recorded by
 * the `RestartGameExecutor` and carried out by the settle boundary through [GameRestarter], the same
 * way "end the turn" waits for its resolution before running (see [Settler]).
 *
 * @property startingPlayerId the controller of the restarting effect, who takes the first turn of
 *   the new game (CR 727.1a)
 * @property exempt cards left in exile instead of joining their owners' decks (CR 727.5)
 * @property followUp the rest of the resolving ability's instructions, followed just before the new
 *   game's first untap step (CR 727.4); its pipeline names the [exempt] cards by their old ids
 */
@Serializable
data class GameRestartRequest(
    val startingPlayerId: EntityId,
    val exempt: List<EntityId> = emptyList(),
    val followUp: TurnStartFollowUp? = null,
)

/**
 * Parked beneath a restart's follow-up instructions when they stop to ask a question (an
 * "as this enters, choose …" of a card put onto the battlefield). Auto-resumed once they finish, it
 * starts the new game's first turn — the step the pre-game procedure would otherwise have taken.
 */
@Serializable
data object BeginFirstTurnContinuation : AutomaticContinuation

/**
 * Restarts the game (CR 727): the current game ends with no winner, loser or draw, and every player
 * still in it begins a new one following CR 103, with the restarting effect's controller as the
 * starting player (CR 727.1a).
 *
 * Every card the players own that is in the game — in any zone, phased out or not, cast or not —
 * becomes a card of its owner's new deck (CR 727.2), its printed self again. Tokens, copies of
 * cards, abilities on the stack and emblems are not cards and do not carry over, and nothing that
 * happened in the old game applies to the new one: life totals, counters, effects, designations and
 * turn history all start fresh. Cards outside the game (the sideboard) stay there, and an exempted
 * card stays in exile (CR 727.5). A commander goes back to its command zone (CR 103.2c) unless it
 * was exempted, and remains its deck's commander (CR 727.5a). A player who already left the game is
 * not part of the new one.
 *
 * Every card in the new game is given a new entity id, minted in a shuffled order exactly as at
 * game setup, so an id the old game made public can't name a card in a new library or hand.
 *
 * The new game then begins: libraries are shuffled, each player draws seven, and the mulligan and
 * opening-hand procedure runs as at any game start. When it finishes, [GameRestartRequest.followUp]
 * runs (CR 727.4) — see [com.wingedsheep.engine.handlers.MulliganHandler.beginFirstTurn].
 */
class GameRestarter(
    private val cardRegistry: CardRegistry,
    /**
     * Starts the new game's first turn, following the restart's instructions first — for a game
     * set up without mulligans, which has no pre-game procedure to end in it (see
     * [com.wingedsheep.engine.handlers.MulliganHandler.beginFirstTurn]).
     */
    private val beginFirstTurn: ((GameState, List<GameEvent>) -> ExecutionResult)? = null,
) {
    private val newGame = NewGameProcedure(cardRegistry)

    fun restart(state: GameState, request: GameRestartRequest): ExecutionResult {
        val involved = state.turnOrder.filter { state.getEntity(it)?.has<PlayerLostComponent>() != true }
        val starting = request.startingPlayerId.takeIf { it in involved } ?: involved.first()
        val exemptInExile = request.exempt.filter { state.logicalZone(it)?.zoneType == Zone.EXILE }.toSet()

        // Every seat stays in the turn order; a player who already left the game is not part of
        // the new one, but keeps their seat.
        var fresh = newGame.emptyGame(state, newGame.turnOrderFrom(state, state.turnOrder.toSet(), starting))
            // A restarted subgame is still a subgame of the same main game, which is unaffected (CR 727.6).
            .copy(mainGame = state.mainGame)

        // The cards each involved player owns, with the zone each starts the new game in.
        val cardsByOwner = newGame.cardsInGame(state, involved.toSet()).groupBy { it.owner }
        val idMap = mutableMapOf<EntityId, EntityId>()
        for ((owner, cards) in cardsByOwner) {
            val placed = cards.map { card ->
                val zone = when {
                    card.id in exemptInExile -> Zone.EXILE
                    card.container.has<CommanderComponent>() -> Zone.COMMAND
                    else -> Zone.LIBRARY
                }
                Triple(card.id, newGame.printedCard(card.container, owner), zone)
            }
            val (next, ids) = newGame.placeUnderFreshIds(fresh, owner, placed)
            fresh = next
            idMap += ids
        }

        for (playerId in state.turnOrder) {
            val old = state.getEntity(playerId) ?: continue
            fresh = fresh.withEntity(
                playerId,
                if (playerId in involved) newGame.newPlayer(old, playerId == starting, idMap) else old
            )
        }
        // Cards outside the game (CR 100.4) aren't involved in it and stay where they are, as they are.
        for ((key, ids) in state.zones) {
            if (key.zoneType != Zone.SIDEBOARD || key.ownerId !in involved) continue
            for (id in ids) {
                val container = state.getEntity(id) ?: continue
                fresh = fresh.withEntity(id, container).addToZone(key, id)
            }
        }
        // Vanguard avatars aren't cards of a deck; they keep their place in the command zone.
        for ((key, ids) in state.zones) {
            if (key.zoneType != Zone.COMMAND) continue
            for (avatarId in ids) {
                val container = state.getEntity(avatarId) ?: continue
                val owner = container.get<VanguardAvatarComponent>()?.ownerId ?: continue
                if (owner !in involved) continue
                fresh = fresh.withEntity(avatarId, container).addToZone(ZoneKey(owner, Zone.COMMAND), avatarId)
            }
        }

        // House rules carry over (CR 727 restarts the game, not the variant): the new game's
        // players get fresh Chaos emblems, with no memory of the old game's rolls.
        if (fresh.chaosRules) fresh = com.wingedsheep.engine.mechanics.chaos.ChaosVariant.install(fresh, involved)

        val (dealt, dealEvents) = newGame.dealOpeningHands(fresh, involved)
        fresh = dealt
        val events = listOf<GameEvent>(GameRestartedEvent(starting)) + dealEvents

        fresh = fresh.copy(restartFollowUp = request.followUp?.let { remap(it, idMap) })
        if (newGame.mulligansSkipped(fresh, involved) && beginFirstTurn != null) return beginFirstTurn.invoke(fresh, events)
        return ExecutionResult.success(fresh, events)
    }

    private fun remap(followUp: TurnStartFollowUp, idMap: Map<EntityId, EntityId>): TurnStartFollowUp {
        val context = followUp.context
        return followUp.copy(context = context.copy(
            sourceId = context.sourceId?.let { idMap[it] },
            pipeline = PipelineState(
                storedCollections = context.pipeline.storedCollections.mapValues { (_, ids) -> ids.mapNotNull { idMap[it] } }
            ),
        ))
    }
}
