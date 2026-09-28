package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.MarchOfTheMachineSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Etched Familiar (MOM #101) — "When this creature dies, each opponent loses 2 life and you gain 2 life."
 */
class EtchedFamiliarScenarioTest : FunSpec({

    test("dying drains each opponent for 2 and gains its controller 2") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MarchOfTheMachineSet.cards)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val familiar = d.putCreatureOnBattlefield(p1, "Etched Familiar")

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, bolt, targets = listOf(familiar))
        d.bothPass() // Bolt resolves, Familiar dies, trigger goes on the stack
        d.bothPass() // resolve the drain

        d.getGraveyardCardNames(p1).contains("Etched Familiar") shouldBe true
        d.getLifeTotal(p2) shouldBe 18
        d.getLifeTotal(p1) shouldBe 22
    }
})
