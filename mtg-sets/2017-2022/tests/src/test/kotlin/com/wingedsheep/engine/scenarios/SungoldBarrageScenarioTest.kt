package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Sungold Barrage. */
class SungoldBarrageScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
    }

    test("destroys a creature with toughness 4") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spider = d.putCreatureOnBattlefield(opponent, "Giant Spider") // 2/4
        val barrage = d.putCardInHand(you, "Sungold Barrage")
        d.giveMana(you, Color.WHITE, 3)

        d.castSpell(you, barrage, listOf(spider)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(opponent, "Giant Spider") shouldBe null
        d.getGraveyard(opponent) shouldContain spider
    }

    test("a creature with toughness 3 is not a legal target") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = d.putCreatureOnBattlefield(opponent, "Hill Giant") // 3/3
        val barrage = d.putCardInHand(you, "Sungold Barrage")
        d.giveMana(you, Color.WHITE, 3)

        d.castSpell(you, barrage, listOf(giant)).outcome shouldNotBe Outcome.Done
        d.findPermanent(opponent, "Hill Giant") shouldBe giant
    }
})
