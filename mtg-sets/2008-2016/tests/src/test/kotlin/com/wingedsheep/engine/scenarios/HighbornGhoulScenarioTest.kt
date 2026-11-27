package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HighbornGhoulScenarioTest : FunSpec({

    test("intimidate: only creatures sharing a color can block it") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val ghoul = driver.putCreatureOnBattlefield(you, "Highborn Ghoul")
        driver.removeSummoningSickness(ghoul)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val cat = driver.putCreatureOnBattlefield(opponent, "Black Cat")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(ghoul), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(opponent, mapOf(bears to listOf(ghoul))).outcome shouldNotBe Outcome.Done
        driver.declareBlockers(opponent, mapOf(cat to listOf(ghoul))).outcome shouldBe Outcome.Done
    }
})
