package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.BileVialBoggart
import com.wingedsheep.mtg.sets.definitions.ecl.cards.SizzlingChangeling
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Sizzling Changeling.
 *
 * Sizzling Changeling {2}{R}
 * Creature — Shapeshifter
 * 3/2
 *
 * Changeling (This card is every creature type.)
 * When this creature dies, exile the top card of your library. Until the end of your
 * next turn, you may play that card.
 *
 * Regression guard for "until the end of your next turn": the exiled card must stay playable
 * through the controller's *next* turn (a land via PlayLand and a nonland spell via CastSpell),
 * and the permission must expire only after that turn. The permission's `expiresAfterTurn` is a
 * floor (`turnNumber + 1` — "not this turn"); which turn it actually closes on is decided by the
 * controller half of the cleanup check, so a duel and a pod behave the same way.
 */
class SizzlingChangelingTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SizzlingChangeling, BileVialBoggart))
        return driver
    }

    /** Attack with the Changeling, let a blocker kill it, and exile the top card of the library. */
    fun setupDeathExile(
        driver: GameTestDriver,
        exiledCardName: String = "Mountain"
    ): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The card that will be exiled (and should stay playable until end of P1's next turn).
        driver.putCardOnTopOfLibrary(p1, exiledCardName)

        val changeling = driver.putCreatureOnBattlefield(p1, "Sizzling Changeling")
        driver.removeSummoningSickness(changeling)
        // 2/2 blocker deals lethal to the 3/2 Changeling.
        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(changeling), p2).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(p2, mapOf(blocker to listOf(changeling))).isSuccess shouldBe true

        // Resolve combat damage (Changeling dies) and its dies trigger (exile + grant may-play).
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 200)

        return p1 to p2
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

    test("dies trigger exiles the top card and grants permission to play it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val (p1, _) = setupDeathExile(driver)

        driver.getExileCardNames(p1) shouldBe listOf("Mountain")
        val exiled = driver.getExile(p1).single()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true
    }

    test("exiled card is still playable on the controller's NEXT turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val (p1, p2) = setupDeathExile(driver)
        val exiled = driver.getExile(p1).single()

        // Advance into the opponent's turn, then back to P1's next turn's main phase.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1

        // The "until end of your next turn" window is still open here.
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true

        // And the card can actually be played from exile.
        driver.playLand(p1, exiled).isSuccess shouldBe true
        driver.getExile(p1).contains(exiled) shouldBe false
    }

    test("permission expires after the controller's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val (p1, p2) = setupDeathExile(driver)
        val exiled = driver.getExile(p1).single()

        // P1's next turn (window still open — verified by the test above).
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1

        // Advance one more full turn cycle — past the end of P1's next turn.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2

        // The window has now closed.
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe false
    }

    test("exiled nonland SPELL (Bile-Vial Boggart) is castable on the controller's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val (p1, p2) = setupDeathExile(driver, exiledCardName = "Bile-Vial Boggart")
        val exiled = driver.getExile(p1).single()
        driver.getExileCardNames(p1) shouldBe listOf("Bile-Vial Boggart")

        // Advance into the opponent's turn, then to P1's next turn's main phase.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p2
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe p1

        // Window still open.
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true

        // Cast the {B} creature from exile.
        driver.giveMana(p1, Color.BLACK, 1)
        driver.castSpell(p1, exiled).isSuccess shouldBe true
    }
})
