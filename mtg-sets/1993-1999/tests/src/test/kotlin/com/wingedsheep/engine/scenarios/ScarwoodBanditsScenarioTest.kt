package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.ScarwoodBandits
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scarwood Bandits.
 *
 * Two things: declining the ransom hands the artifact over, and the theft is bounded — killing the
 * Bandits gives it straight back. The second is what makes the {2} a real decision, and it is what a
 * plain permanent GainControl would silently get wrong.
 */
class ScarwoodBanditsScenarioTest : FunSpec({

    val abilityId = ScarwoodBandits.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ScarwoodBandits)
        return driver
    }

    fun settle(driver: GameTestDriver, payer: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            if (driver.pendingDecision != null) driver.submitYesNo(payer, pay) else driver.bothPass()
        }
    }

    test("an opponent who declines loses the artifact, and gets it back when the Bandits die") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bandits = driver.putCreatureOnBattlefield(me, "Scarwood Bandits")
        driver.removeSummoningSickness(bandits)
        val prize = driver.putPermanentOnBattlefield(opponent, "Fountain of Youth")
        driver.giveMana(me, Color.GREEN, 3)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = bandits,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, prize)),
            )
        ).isSuccess shouldBe true
        settle(driver, opponent, pay = false)

        withClue("they declined, so the artifact came across") {
            driver.state.projectedState.getController(prize) shouldBe me
        }

        withClue("the theft is bounded by the Bandits staying on the battlefield") {
            driver.moveToGraveyard(bandits)
            driver.bothPass()
            driver.state.projectedState.getController(prize) shouldBe opponent
        }
    }

    test("the ransom is asked of the opponent, and paying it keeps the artifact") {
        // The declining case above can't tell who was asked: `submitYesNo` answers whatever
        // decision is pending regardless of whose it is, and declining and never-being-asked both
        // end with the artifact changing hands. This one names the player on the decision, and
        // pays out of *their* pool — the Bandits' controller has none left after activating.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bandits = driver.putCreatureOnBattlefield(me, "Scarwood Bandits")
        driver.removeSummoningSickness(bandits)
        val prize = driver.putPermanentOnBattlefield(opponent, "Fountain of Youth")
        driver.giveMana(me, Color.GREEN, 3)
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = bandits,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, prize)),
            )
        ).isSuccess shouldBe true

        var guard = 0
        while (guard++ < 16 && driver.pendingDecision == null && driver.state.stack.isNotEmpty()) {
            driver.bothPass()
        }

        withClue("the printed \"unless an opponent pays\" bills the opponent, not the activator") {
            driver.pendingDecision?.playerId shouldBe opponent
        }
        withClue("and the question is phrased from their side, not the thief's") {
            driver.pendingDecision?.prompt shouldBe
                "Pay {2} or Scarwood Bandits' controller gains control of that artifact " +
                "for as long as Scarwood Bandits remains on the battlefield?"
        }

        settle(driver, opponent, pay = true)

        withClue("they paid the ransom, so the artifact never moved") {
            driver.state.projectedState.getController(prize) shouldBe opponent
        }
    }
})
