package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.TourachsChant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Tourach's Chant (Fallen Empires).
 *
 * What is under test is the punisher's escape hatch — a `PayCost` that puts a counter on a
 * permanent the *payer* chooses. Its teeth are that it is a cost like any other: a player with no
 * creature cannot put the counter anywhere and takes the 3 damage, which is the whole reason the
 * card does anything against a creatureless board.
 */
class TourachsChantScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TourachsChant)
        return driver
    }

    test("a player with no creatures cannot pay and takes 3") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(alice, "Tourach's Chant")

        val forest = driver.putCardInHand(alice, "Forest")
        driver.playLand(alice, forest).isSuccess shouldBe true
        driver.bothPass()

        withClue("no creature to put the counter on, so the damage is unavoidable") {
            driver.getLifeTotal(alice) shouldBe 17
        }
    }

    test("a player who can pay puts the counter on a creature instead") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(alice, "Tourach's Chant")
        val warrior = driver.putCreatureOnBattlefield(alice, "Elvish Warrior")  // 2/3

        val forest = driver.putCardInHand(alice, "Forest")
        driver.playLand(alice, forest).isSuccess shouldBe true
        // The trigger goes on the stack first; the choice is offered as it resolves.
        driver.bothPass()
        driver.submitCardSelection(alice, listOf(warrior))

        withClue("paying the cost avoids the damage entirely") {
            driver.getLifeTotal(alice) shouldBe 20
        }
        val projected = projector.project(driver.state)
        withClue("the 2/3 Warrior is now a 1/2") {
            projected.getPower(warrior) shouldBe 1
            projected.getToughness(warrior) shouldBe 2
        }
    }
})
