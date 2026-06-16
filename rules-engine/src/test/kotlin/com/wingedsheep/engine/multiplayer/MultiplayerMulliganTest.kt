package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * CR 800.6 — in a multiplayer game (one that began with more than two players) the first mulligan
 * a player takes doesn't count toward the number of cards they put on the bottom or the number of
 * mulligans they may take. Two-player games keep the plain London Mulligan.
 */
class MultiplayerMulliganTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Mulligan Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry() = CardRegistry().also { it.register(bear) }

    fun initGame(players: Int): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Mulligan Test Bear" })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..players).map { com.wingedsheep.engine.core.PlayerConfig("Player $it", deck, 20) },
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    fun GameState.mull(playerId: EntityId): MulliganStateComponent =
        getEntity(playerId)?.get<MulliganStateComponent>() ?: MulliganStateComponent()

    // ── The rule, in isolation on the component ────────────────────────────────

    test("free first mulligan discounts one card from the bottom count") {
        // freeMulligan: first mull bottoms 0, second bottoms 1, third bottoms 2…
        MulliganStateComponent(mulligansTaken = 1, freeMulligan = true).cardsToBottom shouldBe 0
        MulliganStateComponent(mulligansTaken = 2, freeMulligan = true).cardsToBottom shouldBe 1
        MulliganStateComponent(mulligansTaken = 3, freeMulligan = true).cardsToBottom shouldBe 2
    }

    test("two-player mulligan is the plain London Mulligan (no discount)") {
        MulliganStateComponent(mulligansTaken = 1, freeMulligan = false).cardsToBottom shouldBe 1
        MulliganStateComponent(mulligansTaken = 2, freeMulligan = false).cardsToBottom shouldBe 2
    }

    test("the free mulligan grants one extra mulligan before the bottom-7 cap") {
        // Non-free: capped once 7 cards would be bottomed (mulligansTaken == 7).
        MulliganStateComponent(mulligansTaken = 7, freeMulligan = false).canMulligan.shouldBeFalse()
        // Free: the discount means 7 mulligans still only bottoms 6, so one more is allowed.
        MulliganStateComponent(mulligansTaken = 7, freeMulligan = true).canMulligan.shouldBeTrue()
        MulliganStateComponent(mulligansTaken = 8, freeMulligan = true).canMulligan.shouldBeFalse()
    }

    // ── Wired up at game setup by player count ─────────────────────────────────

    test("a multiplayer game (>2 players) sets freeMulligan on every player") {
        val (state, players) = initGame(4)
        players.forEach { state.mull(it).freeMulligan.shouldBeTrue() }
    }

    test("a two-player game does not set freeMulligan") {
        val (state, players) = initGame(2)
        players.forEach { state.mull(it).freeMulligan.shouldBeFalse() }
    }

    // ── End-to-end through the action processor ────────────────────────────────

    test("first multiplayer mulligan bottoms 0 cards, second bottoms 1") {
        val (initial, players) = initGame(4)
        val processor = ActionProcessor(registry())

        val afterFirst = processor.process(initial, TakeMulligan(players[0])).result
        afterFirst.isSuccess.shouldBeTrue()
        afterFirst.newState.mull(players[0]).cardsToBottom shouldBe 0

        val afterSecond = processor.process(afterFirst.newState, TakeMulligan(players[0])).result
        afterSecond.isSuccess.shouldBeTrue()
        afterSecond.newState.mull(players[0]).cardsToBottom shouldBe 1
    }

    test("first two-player mulligan bottoms 1 card") {
        val (initial, players) = initGame(2)
        val processor = ActionProcessor(registry())

        val afterFirst = processor.process(initial, TakeMulligan(players[0])).result
        afterFirst.isSuccess.shouldBeTrue()
        afterFirst.newState.mull(players[0]).cardsToBottom shouldBe 1
    }
})
