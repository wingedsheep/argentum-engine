package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A resolved "play a Magic subgame" effect (CR 729), recorded by the `PlaySubgameExecutor` while
 * its spell resolves. The settle boundary starts the subgame once the effect has run, and the main
 * game keeps the request while it waits: when the subgame is over, [followUp] — the rest of the
 * spell's text — runs in the main game before the spell finishes resolving (CR 729.5).
 *
 * @property sourceName the spell that created the subgame, for the players' view of it
 * @property nonWinners the pipeline collection [followUp] reads the players who didn't win from
 */
@Serializable
data class SubgameRequest(
    val sourceName: String?,
    val nonWinners: String? = null,
    val followUp: TurnStartFollowUp? = null,
)

/** How many subgames deep this game is: 0 for the main game, 1 for a subgame of it (CR 729.6). */
val GameState.subgameDepth: Int
    get() {
        var depth = 0
        var game = mainGame
        while (game != null) {
            depth++
            game = game.mainGame
        }
        return depth
    }

/** The game every subgame was ultimately created from — this game itself when it's no subgame. */
val GameState.outermostGame: GameState
    get() {
        var game = this
        while (true) game = game.mainGame ?: return game
    }

/**
 * Starts and ends Magic subgames (CR 729).
 *
 * A subgame is a completely separate game created by an effect (CR 729.1a). While it is played the
 * main game is discontinued, kept whole as the subgame's [GameState.mainGame]; nothing created in
 * either game means anything in the other (CR 729.1b). Every engine path therefore works on the
 * subgame exactly as on any game, and only the settle boundary ([Settler]) crosses between them:
 * it calls [start] once the creating effect has run, and [end] the moment the subgame is over.
 */
class Subgames(
    cardRegistry: CardRegistry,
    /** Starts the subgame's first turn — for a game set up without mulligans. */
    private val beginFirstTurn: ((GameState, List<GameEvent>) -> ExecutionResult)? = null,
    /** Runs the creating spell's remaining text in the main game. */
    private val effectExecutor: ((GameState, com.wingedsheep.sdk.scripting.effects.Effect, EffectContext) -> EffectResult)? = null,
    /** Drains the automatic work left waiting beneath the effect — the spell finishing its resolution. */
    private val drainAutomatic: ((GameState, List<GameEvent>) -> ExecutionResult)? = null,
) {
    private val newGame = NewGameProcedure(cardRegistry)

    /**
     * Start the subgame [main] asked for (CR 729.2). Every player still in the main game takes
     * part. Each player's main-game library becomes their subgame library and nothing else crosses
     * over, except a commander in the command zone (CR 729.2c) and a vanguard (CR 729.2b). Cards
     * outside the main game are outside the subgame too (CR 729.4), so the sideboard is shared. A
     * randomly chosen player goes first, and the subgame begins like any game (CR 103): shuffled
     * libraries, seven-card hands, mulligans, opening-hand actions.
     *
     * Returns the subgame, holding [main] (with its libraries emptied) as its [GameState.mainGame].
     */
    fun start(main: GameState): ExecutionResult {
        val request = checkNotNull(main.pendingSubgame) { "No subgame was asked for" }
        val players = main.turnOrder.filter { main.getEntity(it)?.has<PlayerLostComponent>() != true }
        val (startIndex, randomized) = main.nextRandom { nextInt(players.size) }
        val starting = players[startIndex]

        var parked = randomized
        var sub = newGame.emptyGame(parked, newGame.turnOrderFrom(parked, players.toSet(), starting))

        val idMap = mutableMapOf<EntityId, EntityId>()
        for (owner in players) {
            // The library's cards leave the main game for the subgame's library; the zone stays,
            // empty. A commander keeps its id in both games (so its owner's commander registry
            // keeps naming it), and its main-game self waits to come back.
            val libraryKey = ZoneKey(owner, Zone.LIBRARY)
            val (commanders, deck) = parked.getZone(libraryKey)
                .partition { parked.getEntity(it)?.has<CommanderComponent>() == true }
            val (next, ids) = newGame.placeUnderFreshIds(sub, owner,
                deck.map { Triple(it, newGame.printedCard(parked.getEntity(it)!!, owner), Zone.LIBRARY) })
            sub = next
            idMap += ids
            for (id in deck) parked = parked.removeEntity(id)
            for (id in commanders) {
                sub = sub.withEntity(id, newGame.printedCard(parked.getEntity(id)!!, owner)).addToZone(libraryKey, id)
                parked = parked.removeFromZone(libraryKey, id)
                idMap[id] = id
            }

            // A commander in the command zone (CR 729.2c) and a vanguard (CR 729.2b) go along
            // the same way.
            val command = ZoneKey(owner, Zone.COMMAND)
            for (id in parked.getZone(command)) {
                val container = parked.getEntity(id) ?: continue
                val moved = when {
                    container.has<CommanderComponent>() -> newGame.printedCard(container, owner)
                    container.has<VanguardAvatarComponent>() -> container
                    else -> continue
                }
                parked = parked.removeFromZone(command, id)
                sub = sub.withEntity(id, moved).addToZone(command, id)
                idMap[id] = id
            }
            // Cards outside the main game are outside the subgame as well (CR 729.4).
            val sideboard = ZoneKey(owner, Zone.SIDEBOARD)
            for (id in parked.getZone(sideboard)) {
                val container = parked.getEntity(id) ?: continue
                sub = sub.withEntity(id, container).addToZone(sideboard, id)
            }
        }
        for (playerId in players) {
            sub = sub.withEntity(playerId, newGame.newPlayer(parked.getEntity(playerId)!!, playerId == starting, idMap))
        }

        // A subgame is a game of Magic of its own (CR 729.1) played under the same house rules.
        if (sub.chaosRules) sub = com.wingedsheep.engine.mechanics.chaos.ChaosVariant.install(sub, players)

        val (dealt, dealEvents) = newGame.dealOpeningHands(sub, players)
        sub = dealt.copy(mainGame = parked.copy(
            rng = dealt.rng, nextEntityId = dealt.nextEntityId, nextRoutingId = dealt.nextRoutingId,
            nextObjectGeneration = dealt.nextObjectGeneration,
        ))
        val events = listOf<GameEvent>(SubgameStartedEvent(starting, sub.subgameDepth, request.sourceName)) + dealEvents
        if (newGame.mulligansSkipped(sub, players) && beginFirstTurn != null) return beginFirstTurn.invoke(sub, events)
        return ExecutionResult.success(sub, events)
    }

    /**
     * The subgame [sub] is over: carry its result back to the main game (CR 729.5). Each player
     * puts every card they own in the subgame — any zone, exile included — into their main-game
     * library and shuffles it; a commander in the subgame's command zone goes back to the main
     * game's (CR 729.5c) and a vanguard to its command zone (CR 729.5b). Tokens, copies, emblems and
     * the subgame's zones cease to exist, and so do the counters players got in it (CR 729.4b).
     *
     * The main game then continues where it was discontinued: the spell that created the subgame
     * follows its remaining text — told which players didn't win — and finishes resolving. The
     * returned result carries the main game's events only, so the caller can settle them there.
     */
    fun end(sub: GameState): ExecutionResult {
        val parked = checkNotNull(sub.mainGame) { "Not a subgame" }
        val request = checkNotNull(parked.pendingSubgame) { "The main game is waiting for no subgame" }
        val players = sub.turnOrder
        val winners = winnersOf(sub)
        val nonWinners = players.filter { it !in winners }

        // The counters that must keep running carry on from where the subgame left them.
        var main = parked.copy(
            pendingSubgame = null,
            rng = sub.rng,
            nextEntityId = sub.nextEntityId,
            nextRoutingId = sub.nextRoutingId,
            nextObjectGeneration = sub.nextObjectGeneration,
            timestamp = maxOf(parked.timestamp, sub.timestamp),
        )

        val subCards = newGame.cardsInGame(sub, players.toSet())
        for (owner in players) {
            val owned = subCards.filter { it.owner == owner }
            val toLibrary = mutableListOf<Triple<EntityId, ComponentContainer, Zone>>()
            for (card in owned) {
                // A card the subgame brought in from outside the game is no longer outside it.
                if (card.id in main.getZone(ZoneKey(owner, Zone.SIDEBOARD))) main = main.removeEntity(card.id)
                val original = parked.getEntity(card.id)
                if (card.container.has<CommanderComponent>() && original != null) {
                    // A commander's main-game self, tax and all, comes back under its own id.
                    val inCommandZone = card.id in sub.getZone(ZoneKey(owner, Zone.COMMAND))
                    main = main.withEntity(card.id, original)
                        .addToZone(ZoneKey(owner, if (inCommandZone) Zone.COMMAND else Zone.LIBRARY), card.id)
                } else {
                    toLibrary += Triple(card.id, newGame.printedCard(card.container, owner), Zone.LIBRARY)
                }
            }
            main = newGame.placeUnderFreshIds(main, owner, toLibrary).first

            // A vanguard goes back to the main game's command zone (CR 729.5b).
            for (id in sub.getZone(ZoneKey(owner, Zone.COMMAND))) {
                if (sub.getEntity(id)?.has<VanguardAvatarComponent>() == true && parked.hasEntity(id)) {
                    main = main.addToZone(ZoneKey(owner, Zone.COMMAND), id)
                }
            }
            // A card that went outside the subgame is outside the main game too.
            val sideboard = ZoneKey(owner, Zone.SIDEBOARD)
            for (id in sub.getZone(sideboard)) {
                if (id in main.getZone(sideboard)) continue
                val container = sub.getEntity(id) ?: continue
                main = main.withEntity(id, container).addToZone(sideboard, id)
            }
        }

        val events = mutableListOf<GameEvent>(SubgameEndedEvent(
            winnerIds = winners.toList(),
            nonWinnerIds = nonWinners,
            depth = sub.subgameDepth,
            sourceName = request.sourceName,
        ))
        for (owner in players) {
            val library = ZoneKey(owner, Zone.LIBRARY)
            val (order, shuffled) = main.nextRandom { shuffle(main.getZone(library)) }
            main = shuffled.reorderZone(library, order)
            events += LibraryShuffledEvent(owner, ShuffleCause.SPELL_OR_ABILITY)
        }

        // First, the spell that created the subgame finishes resolving (CR 729.5).
        val followUp = request.followUp
        val executor = effectExecutor
        if (followUp != null && executor != null) {
            val context = followUp.context.copy(pipeline = followUp.context.pipeline.copy(
                storedCollections = followUp.context.pipeline.storedCollections +
                    listOfNotNull(request.nonWinners?.let { it to nonWinners })
            ))
            val result = executor(main, followUp.effect, context)
            events += result.events
            if (result.outcome is Outcome.Paused) return ExecutionResult.propagatePause(result.state, events)
            main = result.state
        }
        val drain = drainAutomatic ?: return ExecutionResult.success(main, events)
        val finished = drain(main, emptyList())
        return finished.copy(events = events + finished.events)
    }

    /**
     * The players who won [sub]. A draw has no winner. A winning team wins together (CR 810.8a):
     * every member who didn't lose is a winner beside the representative [GameState.winnerId].
     */
    private fun winnersOf(sub: GameState): Set<EntityId> {
        val winner = sub.winnerId ?: return emptySet()
        return sub.teamOf(winner).filter {
            it == winner || sub.getEntity(it)?.has<PlayerLostComponent>() != true
        }.toSet()
    }
}
