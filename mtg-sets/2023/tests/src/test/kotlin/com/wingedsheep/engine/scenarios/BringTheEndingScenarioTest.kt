package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.BringTheEnding
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bring the Ending (ONE #44) — {1}{U} Instant.
 *
 * "Counter target spell unless its controller pays {2}.
 *  Corrupted — Counter that spell instead if its controller has three or more poison counters."
 *
 * Proof card for `Conditions.PoisonCountersAtLeast(3, Player.ControllerOf(...))`: the poison is
 * read off the *targeted spell's controller*, and at the threshold the tax is skipped entirely.
 */
class BringTheEndingScenarioTest : FunSpec({

    /** Player 1 casts Grizzly Bears with two Islands still untapped; player 2 answers. */
    fun bearsOnTheStack(casterPoison: Int, responderPoison: Int = 0): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BringTheEnding))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.player1
        val p2 = driver.player2
        if (casterPoison > 0) driver.addComponent(p1, CountersComponent(mapOf(CounterType.POISON to casterPoison)))
        if (responderPoison > 0) driver.addComponent(p2, CountersComponent(mapOf(CounterType.POISON to responderPoison)))
        driver.putLandOnBattlefield(p1, "Island")
        driver.putLandOnBattlefield(p1, "Island")

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, bears).error shouldBe null
        driver.passPriority(p1)

        val counter = driver.putCardInHand(p2, "Bring the Ending")
        driver.giveMana(p2, Color.BLUE, 2)
        driver.castSpellWithTargets(p2, counter, listOf(ChosenTarget.Spell(bears))).error shouldBe null
        return Triple(driver, bears, counter)
    }

    test("below three poison, the spell's controller is offered the {2} tax") {
        val (driver, _, _) = bearsOnTheStack(casterPoison = 2)
        driver.bothPass()

        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision!!.playerId shouldBe driver.player1
    }

    test("at three poison on the spell's controller, the spell is countered with no chance to pay") {
        val (driver, bears, _) = bearsOnTheStack(casterPoison = 3)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.getGraveyard(driver.player1) shouldContain bears
        driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe null
    }

    test("the caster's own poison doesn't matter — only the targeted spell's controller's") {
        val (driver, _, _) = bearsOnTheStack(casterPoison = 0, responderPoison = 5)
        driver.bothPass()

        driver.pendingDecision?.playerId shouldBe driver.player1
    }
})
