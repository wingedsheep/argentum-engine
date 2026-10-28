package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tempt with Vengeance — a tempting offer whose offer is sized by the spell's X. The X has to
 * survive into the accepting opponent's run of the offer (they create X Elementals of their own)
 * and into the caster's repeat for that acceptance.
 */
class TemptWithVengeanceScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.elementals(player: EntityId): Int =
        getPermanents(player).count { getCardName(it)?.contains("Elemental") == true }

    fun GameTestDriver.castTempt(caster: EntityId, x: Int) {
        val tempt = putCardInHand(caster, "Tempt with Vengeance")
        giveMana(caster, Color.RED, x + 1)
        castXSpell(caster, tempt, xValue = x)
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 10) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    test("the opponent accepts: they create X Elementals and the caster creates X twice") {
        val driver = newGame()
        val caster = driver.player1
        val opponent = driver.player2
        driver.castTempt(caster, x = 2)

        withClue("The caster's first X Elementals are in play before the opponent is asked") {
            driver.elementals(caster) shouldBe 2
        }
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, true)

        withClue("The opponent created X Elementals and the caster created X more") {
            driver.pendingDecision shouldBe null
            driver.elementals(opponent) shouldBe 2
            driver.elementals(caster) shouldBe 4
        }
    }

    test("the opponent declines: the caster creates X Elementals once") {
        val driver = newGame()
        val caster = driver.player1
        val opponent = driver.player2
        driver.castTempt(caster, x = 3)

        driver.submitYesNo(opponent, false)

        withClue("Only the caster's first batch") {
            driver.pendingDecision shouldBe null
            driver.elementals(caster) shouldBe 3
            driver.elementals(opponent) shouldBe 0
        }
    }
})
