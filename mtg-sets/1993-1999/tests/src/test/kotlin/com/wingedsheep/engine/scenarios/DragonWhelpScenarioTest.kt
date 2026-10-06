package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.DragonWhelp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Dragon Whelp (Limited Edition Alpha).
 *
 * {R}: This creature gets +1/+0 until end of turn. If this ability has been activated four or more
 * times this turn, sacrifice this creature at the beginning of the next end step.
 *
 * Unlike Farrelite Priest (a mana ability), this ability uses the stack, so the activation tally is
 * read at resolution from the stacked ability's own id. Three activations are safe; the fourth
 * schedules the sacrifice for the end step, also when all four are stacked before any resolves.
 */
class DragonWhelpScenarioTest : FunSpec({

    val abilityId = DragonWhelp.activatedAbilities.first().id

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DragonWhelp)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val whelp = driver.putCreatureOnBattlefield(alice, "Dragon Whelp")
        return Triple(driver, alice, whelp)
    }

    fun activate(driver: GameTestDriver, player: EntityId, whelp: EntityId) {
        driver.giveMana(player, Color.RED, 1)
        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = whelp, abilityId = abilityId))
    }

    test("three activations pump without scheduling the sacrifice") {
        val (driver, alice, whelp) = setup()
        repeat(3) {
            activate(driver, alice, whelp)
            driver.bothPass()
        }
        driver.state.projectedState.getPower(whelp) shouldBe 5

        driver.passPriorityUntil(Step.CLEANUP)
        withClue("the burnout clause needs a fourth activation") {
            driver.state.getBattlefield().contains(whelp) shouldBe true
        }
    }

    test("the fourth activation sacrifices the Whelp at the next end step") {
        val (driver, alice, whelp) = setup()
        repeat(4) {
            activate(driver, alice, whelp)
            driver.bothPass()
        }
        withClue("the sacrifice is delayed; the Whelp is still a 6/3 now") {
            driver.state.getBattlefield().contains(whelp) shouldBe true
            driver.state.projectedState.getPower(whelp) shouldBe 6
        }

        driver.passPriorityUntil(Step.CLEANUP)
        driver.state.getBattlefield().contains(whelp) shouldBe false
        driver.assertInGraveyard(alice, "Dragon Whelp")
    }

    test("four activations stacked before any resolves still schedule the sacrifice") {
        val (driver, alice, whelp) = setup()
        repeat(4) { activate(driver, alice, whelp) }
        repeat(4) { driver.bothPass() }
        driver.state.projectedState.getPower(whelp) shouldBe 6

        driver.passPriorityUntil(Step.CLEANUP)
        driver.state.getBattlefield().contains(whelp) shouldBe false
        driver.assertInGraveyard(alice, "Dragon Whelp")
    }
})
