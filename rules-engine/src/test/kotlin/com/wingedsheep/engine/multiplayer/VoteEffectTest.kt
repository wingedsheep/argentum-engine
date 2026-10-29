package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.VoteCastEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Voting (CR 701.38): starting with the specified player and proceeding in turn order, each player
 * chooses one of the listed choices (701.38a). [com.wingedsheep.sdk.scripting.effects.VoteEffect]
 * always starts with the controller ("starting with you"), announces each vote, and publishes one
 * tally per choice for the rest of the resolution to read.
 */
class VoteEffectTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }

    fun initGame(playerCount: Int): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..playerCount).map { PlayerConfig("Player $it", Deck(cards = List(40) { "Mountain" }), 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    /** Vote x/y, then gain life per x vote and draw a card per y vote — both tallies observable. */
    val voteThenSpend = Effects.Pipeline {
        val (x, y) = vote("x", "y")
        run(Effects.GainLife(x.amount))
        run(Effects.DrawCards(y.amount))
    }

    data class Run(val state: GameState, val voters: List<EntityId>, val prompts: List<String>, val events: List<GameEvent>)

    /** Resolve [voteThenSpend] for [controller], answering each ballot with the next of [votes]. */
    fun resolve(state: GameState, controller: EntityId, votes: List<Int>): Run {
        var result = EngineServices(registry).effectExecutorRegistry
            .execute(state, voteThenSpend, EffectContext(sourceId = null, controllerId = controller))
        var current = result.state
        val voters = mutableListOf<EntityId>()
        val prompts = mutableListOf<String>()
        val events = result.events.toMutableList()
        (result.outcome is Outcome.Paused) shouldBe true
        for (vote in votes) {
            val ballot = current.pendingDecision
            ballot.shouldBeInstanceOf<ChooseOptionDecision>()
            ballot.options shouldContainExactly listOf("X", "Y")
            voters += ballot.playerId
            prompts += ballot.prompt
            val processed = ActionProcessor(registry)
                .process(current, SubmitDecision(ballot.playerId, OptionChosenResponse(ballot.id, vote))).result
            current = processed.newState
            events += processed.events
        }
        current.pendingDecision.shouldBeNull()
        return Run(current, voters, prompts, events)
    }

    fun handSize(state: GameState, player: EntityId) = state.getZone(ZoneKey(player, Zone.HAND)).size

    test("starting with you, each player votes in turn order — not from the active player") {
        val (state, players) = initGame(4)
        // Player 1 is the active player; Player 3 controls the vote.
        val run = resolve(state, players[2], votes = listOf(0, 1, 0, 0))
        run.voters shouldContainExactly listOf(players[2], players[3], players[0], players[1])
    }

    test("each choice's tally is the number of players who voted for it") {
        val (state, players) = initGame(4)
        val hand = handSize(state, players[0])
        val run = resolve(state, players[0], votes = listOf(0, 1, 0, 0))
        run.state.lifeTotal(players[0]) shouldBe 23
        handSize(run.state, players[0]) shouldBe hand + 1
    }

    test("a choice nobody voted for tallies zero") {
        val (state, players) = initGame(2)
        val hand = handSize(state, players[0])
        val run = resolve(state, players[0], votes = listOf(0, 0))
        run.state.lifeTotal(players[0]) shouldBe 22
        handSize(run.state, players[0]) shouldBe hand
    }

    test("every vote is announced, and later voters see the votes cast before theirs") {
        val (state, players) = initGame(3)
        val run = resolve(state, players[0], votes = listOf(1, 0, 1))
        run.events.filterIsInstance<VoteCastEvent>().map { it.playerId to it.choice } shouldContainExactly listOf(
            players[0] to "y", players[1] to "x", players[2] to "y"
        )
        run.prompts shouldContainExactly listOf(
            "Vote for x or y",
            "Vote for x or y (votes so far: x 0, y 1)",
            "Vote for x or y (votes so far: x 1, y 1)",
        )
    }

    test("a player who has left the game does not vote") {
        val (initial, players) = initGame(3)
        val state = initial.updateEntity(players[1]) { it.with(PlayerLostComponent(LossReason.CONCESSION)) }
        val run = resolve(state, players[0], votes = listOf(0, 0))
        run.voters shouldContainExactly listOf(players[0], players[2])
        run.state.lifeTotal(players[0]) shouldBe 22
    }

    test("a player who leaves the game while an earlier ballot is pending is skipped") {
        val (state, players) = initGame(3)
        val first = EngineServices(registry).effectExecutorRegistry
            .execute(state, voteThenSpend, EffectContext(sourceId = null, controllerId = players[0])).state
        val ballot = first.pendingDecision as ChooseOptionDecision
        // Player 3 concedes before Player 1's vote is in; Player 2 votes next, then the vote ends.
        val conceded = first.updateEntity(players[2]) { it.with(PlayerLostComponent(LossReason.CONCESSION)) }
        val afterFirst = ActionProcessor(registry)
            .process(conceded, SubmitDecision(players[0], OptionChosenResponse(ballot.id, 0))).result.newState
        val second = afterFirst.pendingDecision as ChooseOptionDecision
        second.playerId shouldBe players[1]
        val done = ActionProcessor(registry)
            .process(afterFirst, SubmitDecision(players[1], OptionChosenResponse(second.id, 0))).result.newState
        done.pendingDecision.shouldBeNull()
        done.lifeTotal(players[0]) shouldBe 22
    }
})
