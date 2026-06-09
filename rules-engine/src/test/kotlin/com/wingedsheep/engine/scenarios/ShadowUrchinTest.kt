package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.ShadowUrchin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Shadow Urchin.
 *
 * Shadow Urchin {2}{B/R} — Creature — Ouphe 3/4
 * Whenever a creature you control with one or more counters on it dies, exile that many cards from
 * the top of your library. Until your next end step, you may play those cards.
 *
 * Regression guard for the reported bug: when the counter-bearing creature dies during the
 * OPPONENT's turn, "until your next end step" must close at the controller's own next end step — not
 * a full turn later. The expiry is driven by the cleanup step, and `GameState.turnNumber` is
 * round-based (it only increments when the starting player begins a new turn), so counting
 * player-turns until the controller's turn over-counted whenever the grant happened on an opponent's
 * turn, leaking the may-play window across an extra turn ("second turn from then still playable").
 */
class ShadowUrchinTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShadowUrchin))
        return driver
    }

    /** Advance to the next turn's precombat main, stepping out via the END step first. */
    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    // P1 is the starting player; P2 controls Shadow Urchin. P2 sits *after* P1 in the round, so when
    // Shadow Urchin dies on P1's turn, P2's next end step is still this same round — the case the old
    // round-based math pushed a full turn too late.
    test("may-play window closes at the controller's own next end step when granted on the opponent's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2

        // P2's Shadow Urchin carries a -1/-1 counter (as its own blight would leave). It is a 2/3.
        val urchin = driver.putCreatureOnBattlefield(p2, "Shadow Urchin")
        driver.addComponent(urchin, CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)))

        // P1's turn: bolt the urchin so a counter-bearing creature P2 controls dies on P1's turn.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe p1
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpell(p1, bolt, listOf(urchin)).isSuccess shouldBe true

        // Resolve the bolt + the death trigger (exile top card, grant may-play) on P1's turn.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 200)
        val exiled = driver.getExile(p2)
        exiled.size shouldBe 1
        val card = exiled.first()
        driver.state.mayPlayPermissions.any { card in it.cardIds } shouldBe true

        // P2's own turn (same round): "until your next end step" is still open here.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2
        driver.state.mayPlayPermissions.any { card in it.cardIds } shouldBe true

        // P1's next turn (next round): the window closed at the cleanup of P2's turn above. Before the
        // fix it leaked into here — the reported "second turn from then still playable" bug.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1
        driver.state.mayPlayPermissions.any { card in it.cardIds } shouldBe false
    }
})
