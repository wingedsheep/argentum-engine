package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.BondedHorncrest
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BondedHorncrestScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + BondedHorncrest)
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("another creature on the battlefield is insufficient but two Horncrests can attack together") {
        val driver = driver()
        val first = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, listOf(first), driver.player2).error shouldNotBe null
        driver.declareAttackers(driver.player1, listOf(first, second), driver.player2).error shouldBe null
    }

    test("Horncrest cannot block alone but two Horncrests can block different attackers") {
        val driver = driver()
        val first = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val attacker1 = driver.putCreatureOnBattlefield(driver.player2, "Bonded Horncrest")
        val attacker2 = driver.putCreatureOnBattlefield(driver.player2, "Bonded Horncrest")
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player2, listOf(attacker1, attacker2), driver.player1).error shouldBe null
        driver.bothPass()

        driver.declareBlockers(driver.player1, mapOf(first to listOf(attacker1))).error shouldNotBe null
        driver.declareBlockers(
            driver.player1, mapOf(first to listOf(attacker1), second to listOf(attacker2))
        ).error shouldBe null
    }
})
