package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SengirConnoisseurScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        val spell = putCardInHand(player1, "Lightning Bolt")
        castSpell(player1, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("opposing death adds one counter and later deaths wait until the next turn") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Sengir Connoisseur")
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Grizzly Bears"))
        d.stackSize shouldBe 1
        d.bothPass()
        d.state.projectedState.getPower(source) shouldBe 4
        d.bolt(d.putCreatureOnBattlefield(d.player1, "Grizzly Bears"))
        d.stackSize shouldBe 0
        d.state.projectedState.getPower(source) shouldBe 4

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.passPriority(d.player2)
        d.bolt(d.putCreatureOnBattlefield(d.player1, "Grizzly Bears"))
        d.stackSize shouldBe 1
        d.bothPass()
        d.state.projectedState.getPower(source) shouldBe 5
        d.state.projectedState.getToughness(source) shouldBe 5
    }

    test("simultaneous deaths of both players creatures produce only one counter") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Sengir Connoisseur")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveMana(d.player1, Color.RED, 2)
        val spell = d.putCardInHand(d.player1, "Pyroclasm")
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 1
        d.bothPass()
        d.state.projectedState.getPower(source) shouldBe 4
        d.state.projectedState.getToughness(source) shouldBe 4
    }

    test("its own death does not trigger its ability") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Sengir Connoisseur")
        d.bolt(source)
        d.stackSize shouldBe 0
        d.findPermanent(d.player1, "Sengir Connoisseur") shouldBe null
    }
})
