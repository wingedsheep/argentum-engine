package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.FarrelitePriest
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Farrelite Priest (Fallen Empires).
 *
 * Farrelite Priest: {1}{W}{W}
 * Creature — Human Cleric
 * 1/3
 * {1}: Add {W}. If this ability has been activated four or more times this turn, sacrifice this
 * creature at the beginning of the next end step.
 *
 * What is under test is the tally, not the mana: the clause *reads* an activation count without
 * imposing a limit, which is why the ability opts into bookkeeping (`trackActivations`). Three
 * activations must leave the Priest alive; the fourth must schedule its death for the end step —
 * and not before, so the mana from that fourth activation is still usable.
 */
class FarrelitePriestScenarioTest : FunSpec({

    val abilityId = FarrelitePriest.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FarrelitePriest)
        return driver
    }

    fun activate(driver: GameTestDriver, player: EntityId, priest: EntityId, times: Int) {
        repeat(times) {
            driver.giveColorlessMana(player, 1)
            driver.submitSuccess(
                ActivateAbility(playerId = player, sourceId = priest, abilityId = abilityId)
            )
        }
    }

    test("three activations leave the Priest alive through the end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val priest = driver.putCreatureOnBattlefield(alice, "Farrelite Priest")
        activate(driver, alice, priest, 3)

        driver.passPriorityUntil(Step.CLEANUP)
        withClue("the burnout clause needs a fourth activation") {
            driver.state.getBattlefield().contains(priest) shouldBe true
        }
    }

    test("the fourth activation sacrifices the Priest at the next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val priest = driver.putCreatureOnBattlefield(alice, "Farrelite Priest")
        activate(driver, alice, priest, 4)

        withClue("the sacrifice is delayed to the end step, so the fourth mana is still usable") {
            driver.state.getBattlefield().contains(priest) shouldBe true
        }

        driver.passPriorityUntil(Step.CLEANUP)
        driver.state.getBattlefield().contains(priest) shouldBe false
        driver.assertInGraveyard(alice, "Farrelite Priest")
    }
})
