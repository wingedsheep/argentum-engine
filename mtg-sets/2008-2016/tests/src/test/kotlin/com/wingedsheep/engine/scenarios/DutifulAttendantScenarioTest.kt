package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.DutifulAttendant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dutiful Attendant (DTK #99) — {2}{B} Creature — Human Warrior 1/2.
 * "When this creature dies, return another target creature card from your graveyard to your hand."
 *
 * The target is chosen once the Attendant is already in the graveyard, so these prove "another"
 * keeps it from returning itself, and "your graveyard" keeps an opponent's creature card out.
 */
class DutifulAttendantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DutifulAttendant))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.boltAttendant(attendant: EntityId) {
        val controller = activePlayer!!
        val bolt = putCardInHand(controller, "Lightning Bolt")
        giveMana(controller, Color.RED, 1)
        castSpell(controller, bolt, listOf(attendant)).outcome shouldBe Outcome.Done
        bothPass() // Lightning Bolt resolves; the Attendant dies and its trigger wants a target
    }

    test("returns another creature card from your graveyard, never itself or an opponent's card") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val attendant = driver.putCreatureOnBattlefield(controller, "Dutiful Attendant")
        val courser = driver.putCardInGraveyard(controller, "Centaur Courser")
        val opposingLions = driver.putCardInGraveyard(opponent, "Savannah Lions")

        driver.boltAttendant(attendant)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        withClue("the Attendant is already in the graveyard but \"another\" excludes it") {
            legal shouldNotContain attendant
        }
        withClue("only creature cards in your own graveyard") {
            legal shouldNotContain opposingLions
        }
        legal shouldContain courser

        driver.submitTargetSelection(controller, listOf(courser))
        while (driver.stackSize > 0) driver.bothPass()

        driver.getHand(controller) shouldContain courser
        driver.getGraveyard(controller) shouldContain attendant
        driver.getGraveyard(opponent) shouldContain opposingLions
    }

    test("with no other creature card in your graveyard the trigger has no target and nothing returns") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        val attendant = driver.putCreatureOnBattlefield(controller, "Dutiful Attendant")

        driver.boltAttendant(attendant)
        if (driver.stackSize > 0) driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.getHand(controller) shouldNotContain attendant
        driver.getGraveyard(controller) shouldContain attendant
    }
})
