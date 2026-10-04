package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.AzoriusChancery
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Azorius Chancery (DIS) — Land.
 *
 *  "This land enters tapped.
 *   When this land enters, return a land you control to its owner's hand.
 *   {T}: Add {W}{U}."
 *
 * The bounce doesn't target (CR 115.10a): the land is chosen as the trigger resolves, from every
 * land you control — the Chancery included.
 */
class AzoriusChanceryScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AzoriusChancery)
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolves the enters trigger, answering the resolution-time choice with [pick]. */
    fun resolveBounce(driver: GameTestDriver, pick: (SelectCardsDecision) -> Unit) {
        repeat(20) {
            when (val pending = driver.pendingDecision) {
                is ChooseTargetsDecision -> error("The bounce must not target")
                is SelectCardsDecision -> pick(pending)
                null -> {
                    if (driver.state.stack.isEmpty()) return
                    driver.bothPass()
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    test("enters tapped and returns a chosen land you control, chosen on resolution") {
        val driver = newDriver()
        val me = driver.player1
        val plains = driver.putLandOnBattlefield(me, "Plains")
        val chancery = driver.putCardInHand(me, "Azorius Chancery")

        driver.playLand(me, chancery)
        withClue("The trigger is on the stack with no target chosen") {
            driver.state.stack.size shouldBe 1
        }

        resolveBounce(driver) { decision ->
            decision.options.toSet() shouldBe setOf(plains, chancery)
            driver.submitCardSelection(decision.playerId, listOf(plains))
        }

        withClue("The chosen Plains went to hand; the Chancery stayed, tapped") {
            driver.state.getZone(me, Zone.HAND).contains(plains) shouldBe true
            driver.state.getZone(me, Zone.BATTLEFIELD).contains(chancery) shouldBe true
            driver.isTapped(chancery) shouldBe true
        }
    }

    test("returns itself when it is the only land you control") {
        val driver = newDriver()
        val me = driver.player1
        val chancery = driver.putCardInHand(me, "Azorius Chancery")

        driver.playLand(me, chancery)
        resolveBounce(driver) { decision ->
            driver.submitCardSelection(decision.playerId, listOf(chancery))
        }

        driver.state.getZone(me, Zone.HAND).contains(chancery) shouldBe true
    }
})
