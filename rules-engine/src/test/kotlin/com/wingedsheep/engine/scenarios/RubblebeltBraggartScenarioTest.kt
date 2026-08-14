package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RubblebeltBraggart
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rubblebelt Braggart — "Whenever this creature attacks, if it's not suspected, you may suspect it."
 *
 * Covers the new `StatePredicate.IsSuspected` / `Conditions.SourceIsSuspected` vocabulary through
 * the card that needs it:
 *  - accepting the "may" applies the whole suspect composite (designation + menace + can't block);
 *  - declining leaves it untouched, and the offer comes back on a later attack;
 *  - once suspected, the intervening-if (CR 603.4) stops the trigger firing at all — which is what
 *    keeps CR 701.60d ("a suspected permanent can't become suspected again") from being a silent
 *    no-op the player still has to click through.
 */
class RubblebeltBraggartScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RubblebeltBraggart)
        return driver
    }

    /** Advance whole turns until [player] is the active player again, stopping in precombat main. */
    fun advanceToNextTurnOf(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId) {
        do {
            driver.passPriorityUntil(Step.END)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != player)
    }

    test("accepting the attack trigger suspects it — designation, menace and can't block") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val braggart = driver.putCreatureOnBattlefield(active, "Rubblebelt Braggart")
        driver.removeSummoningSickness(braggart)

        withClue("a freshly-played Braggart is not suspected") {
            StateProjector().project(driver.state).isSuspected(braggart) shouldBe false
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(braggart), opponent)

        // The trigger goes on the stack; resolving it asks the controller whether to suspect.
        driver.bothPass()
        withClue("attacking unsuspected must offer the may-suspect decision") {
            driver.pendingDecision shouldNotBe null
        }
        driver.submitYesNo(active, true)

        val projected = StateProjector().project(driver.state)
        withClue("accepting applies the suspected designation (CR 701.60a)") {
            projected.isSuspected(braggart) shouldBe true
        }
        withClue("suspected grants menace (CR 701.60c)") {
            projected.hasKeyword(braggart, Keyword.MENACE) shouldBe true
        }
        withClue("suspected can't block (CR 701.60c)") {
            projected.cantBlock(braggart) shouldBe true
        }
    }

    test("declining leaves it unsuspected, and the offer returns on the next attack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val braggart = driver.putCreatureOnBattlefield(active, "Rubblebelt Braggart")
        driver.removeSummoningSickness(braggart)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(braggart), opponent)
        driver.bothPass()
        driver.pendingDecision shouldNotBe null
        driver.submitYesNo(active, false)

        withClue("declining the may must leave it unsuspected") {
            val projected = StateProjector().project(driver.state)
            projected.isSuspected(braggart) shouldBe false
            projected.cantBlock(braggart) shouldBe false
        }

        // Still not suspected, so the intervening-if is satisfied again next turn.
        advanceToNextTurnOf(driver, active)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(braggart), opponent)
        driver.bothPass()
        withClue("a declined offer is not consumed — it comes back on the next attack") {
            driver.pendingDecision shouldNotBe null
        }
        driver.submitYesNo(active, true)
        StateProjector().project(driver.state).isSuspected(braggart) shouldBe true
    }

    test("already suspected — the intervening-if stops the trigger firing at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val braggart = driver.putCreatureOnBattlefield(active, "Rubblebelt Braggart")
        driver.removeSummoningSickness(braggart)

        // Attack once and accept, so it is suspected going into the second attack.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(braggart), opponent)
        driver.bothPass()
        driver.submitYesNo(active, true)
        StateProjector().project(driver.state).isSuspected(braggart) shouldBe true

        advanceToNextTurnOf(driver, active)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(braggart), opponent)
        driver.bothPass()

        withClue("an already-suspected Braggart must not put the trigger on the stack (CR 603.4)") {
            driver.pendingDecision shouldBe null
        }
        withClue("and it stays suspected exactly once") {
            StateProjector().project(driver.state).isSuspected(braggart) shouldBe true
        }
    }
})
