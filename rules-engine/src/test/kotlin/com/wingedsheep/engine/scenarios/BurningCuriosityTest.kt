package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.BurningCuriosity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Burning Curiosity.
 *
 * Burning Curiosity {2}{R}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may blight 1.
 * Exile the top two cards of your library. If this spell's additional cost was paid, exile the
 * top three cards instead. Until the end of your next turn, you may play those cards.
 *
 * Regression guard for "until the end of your next turn" when the controller is NOT the starting
 * player. The permission records a turn *floor* ("not this turn"), so the expiry must also be gated
 * on the *controller's own* turn; otherwise the non-starting player loses the permission at the end
 * of whichever opponent's turn comes first, one full turn too early, and can no longer play the
 * exiled land on their next turn (the reported bug).
 */
class BurningCuriosityTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BurningCuriosity))
        return driver
    }

    /**
     * Advance to the next turn's precombat main phase. Stepping out via the END step first is
     * required: calling [GameTestDriver.passPriorityUntil] for PRECOMBAT_MAIN while already in a
     * precombat main is a no-op.
     */
    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    /**
     * Cast Burning Curiosity (declining the optional blight, so it exiles the top two cards) on the
     * given player's precombat main phase, then resolve it. Returns the exiled card entity ids.
     */
    fun castAndResolve(driver: GameTestDriver, caster: com.wingedsheep.sdk.model.EntityId): List<com.wingedsheep.sdk.model.EntityId> {
        driver.giveMana(caster, Color.RED, 3) // {2}{R}, paid from pool
        val spell = driver.putCardInHand(caster, "Burning Curiosity")
        driver.castSpell(caster, spell).isSuccess shouldBe true
        // Resolve the spell (exile top two + grant may-play) without leaving the caster's turn.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 200)
        return driver.getExile(caster)
    }

    test("exiles the top two cards and grants permission to play them") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val exiled = castAndResolve(driver, p1)

        exiled.size shouldBe 2
        driver.getExileCardNames(p1) shouldBe listOf("Mountain", "Mountain")
        exiled.all { id -> driver.state.mayPlayPermissions.any { id in it.cardIds } } shouldBe true
    }

    // The non-starting player is the case the original bug broke: the permission expired at the end
    // of the starting player's turn in the next round instead of at the end of the controller's own.
    test("exiled land is still playable on the NON-STARTING controller's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2

        // Move from P1's first turn into P2's first turn, then cast there.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe p1
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2

        val exiled = castAndResolve(driver, p2)
        val land = exiled.first()
        driver.state.mayPlayPermissions.any { land in it.cardIds } shouldBe true

        // Advance through P1's next turn into P2's next turn — the one the window must cover.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2

        // The "until end of your next turn" window must still be open here.
        driver.state.mayPlayPermissions.any { land in it.cardIds } shouldBe true
        driver.playLand(p2, land).isSuccess shouldBe true
        driver.getExile(p2).contains(land) shouldBe false
    }

    test("permission expires after the NON-STARTING controller's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2

        val exiled = castAndResolve(driver, p2)
        val land = exiled.first()

        // P2's next turn (window still open — verified above).
        advanceToNextTurnMain(driver) // P1, next round
        advanceToNextTurnMain(driver) // P2, next round
        driver.state.activePlayerId shouldBe p2
        driver.state.mayPlayPermissions.any { land in it.cardIds } shouldBe true

        // One more turn cycle — past the end of P2's next turn.
        advanceToNextTurnMain(driver) // P1
        advanceToNextTurnMain(driver) // P2
        driver.state.activePlayerId shouldBe p2

        // The window has now closed.
        driver.state.mayPlayPermissions.any { land in it.cardIds } shouldBe false
    }

    test("exiled land is still playable on the STARTING controller's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe p1
        val exiled = castAndResolve(driver, p1)
        val land = exiled.first()

        // Advance through P2's turn into P1's next turn.
        advanceToNextTurnMain(driver)
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1

        driver.state.mayPlayPermissions.any { land in it.cardIds } shouldBe true
        driver.playLand(p1, land).isSuccess shouldBe true
        driver.getExile(p1).contains(land) shouldBe false
    }
})
