package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BloodSpeaker
import com.wingedsheep.mtg.sets.definitions.chk.cards.KuroPitlord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Blood Speaker.
 *
 * Upkeep: "you may sacrifice this creature. If you do, search your library for a Demon card …"
 * Graveyard: "Whenever a Demon you control enters, return this card from your graveyard to your hand."
 */
class BloodSpeakerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BloodSpeaker)
        driver.registerCard(KuroPitlord)
        return driver
    }

    fun advanceToMyNextUpkeep(driver: GameTestDriver, me: EntityId) {
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
    }

    /** Resolve the upkeep trigger: answer the "may" and pick [pick] from the search, if offered. */
    fun resolveUpkeep(driver: GameTestDriver, me: EntityId, accept: Boolean, pick: EntityId?) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val d = driver.pendingDecision) {
                is YesNoDecision -> driver.submitYesNo(me, accept)
                is SelectCardsDecision -> driver.submitCardSelection(me, listOfNotNull(pick))
                null -> driver.bothPass()
                else -> error("unexpected decision $d")
            }
        }
    }

    test("sacrificing on upkeep tutors a Demon to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Blood Speaker")
        val pitlord = driver.putCardOnTopOfLibrary(me, "Kuro, Pitlord")

        advanceToMyNextUpkeep(driver, me)
        resolveUpkeep(driver, me, accept = true, pick = pitlord)

        withClue("Blood Speaker was sacrificed and the Demon is in hand") {
            driver.findPermanent(me, "Blood Speaker") shouldBe null
            driver.getGraveyardCardNames(me) shouldContain "Blood Speaker"
            driver.getHand(me) shouldContain pitlord
        }
    }

    test("declining keeps Blood Speaker and searches nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Blood Speaker")
        val pitlord = driver.putCardOnTopOfLibrary(me, "Kuro, Pitlord")

        advanceToMyNextUpkeep(driver, me)
        resolveUpkeep(driver, me, accept = false, pick = pitlord)

        driver.findPermanent(me, "Blood Speaker") shouldNotBe null
        driver.getHand(me) shouldNotContain pitlord
    }

    test("a Demon entering under my control returns Blood Speaker from the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val speaker = driver.putCardInGraveyard(me, "Blood Speaker")
        val pitlord = driver.putCardInHand(me, "Kuro, Pitlord")
        driver.giveMana(me, Color.BLACK, 9)
        driver.castSpell(me, pitlord)
        var guard = 0
        while (guard++ < 8 && driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(me, "Kuro, Pitlord") shouldNotBe null
        driver.getHand(me) shouldContain speaker
        driver.getGraveyardCardNames(me) shouldNotContain "Blood Speaker"
    }

    test("an opponent's Demon does not return Blood Speaker") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInGraveyard(me, "Blood Speaker")
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent

        val pitlord = driver.putCardInHand(opponent, "Kuro, Pitlord")
        driver.giveMana(opponent, Color.BLACK, 9)
        driver.castSpell(opponent, pitlord)
        var guard = 0
        while (guard++ < 8 && driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Kuro, Pitlord") shouldNotBe null
        driver.getGraveyardCardNames(me) shouldContain "Blood Speaker"
    }
})
