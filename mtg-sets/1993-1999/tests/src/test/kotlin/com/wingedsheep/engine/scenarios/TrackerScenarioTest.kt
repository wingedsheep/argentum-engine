package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.Tracker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tracker — "deals damage equal to its power to target creature. That creature
 * deals damage equal to its power to this creature."
 *
 * Both directions have to land, and the case that proves the second one is a target big enough to
 * kill the 2/2 Tracker back. The trade case also pins the no-SBAs-mid-resolution behaviour: a
 * target the first half kills is still there to hit back, so both die.
 */
class TrackerScenarioTest : FunSpec({

    val abilityId = Tracker.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Tracker)
        return driver
    }

    test("a 2/2 Tracker and a 2/2 target trade") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tracker = driver.putCreatureOnBattlefield(me, "Tracker")
        driver.removeSummoningSickness(tracker)
        val prey = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = tracker,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, prey)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the Tracker's 2 killed the Bears") {
            driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        }
        withClue("and the Bears still hit back before state-based actions ran") {
            driver.findPermanent(me, "Tracker") shouldBe null
        }
    }

    test("a small target dies and the Tracker survives") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tracker = driver.putCreatureOnBattlefield(me, "Tracker")
        driver.removeSummoningSickness(tracker)
        // Goblin Balloon Brigade is a 1/1 — one damage back is not enough.
        val prey = driver.putCreatureOnBattlefield(opponent, "Goblin Balloon Brigade")
        driver.giveMana(me, Color.GREEN, 2)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = tracker,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, prey)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(opponent, "Goblin Balloon Brigade") shouldBe null
        withClue("1 damage on a 2/2 is not lethal") {
            driver.findPermanent(me, "Tracker") shouldBe tracker
        }
    }
})
