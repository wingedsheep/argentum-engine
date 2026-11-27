package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReapTheSeagrafScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast from hand, it creates a 2/2 black Zombie token") {
        val driver = newDriver()
        val you = driver.activePlayer!!

        val reap = driver.putCardInHand(you, "Reap the Seagraf")
        driver.giveMana(you, Color.BLACK, 3)
        driver.castSpell(you, reap).outcome shouldBe Outcome.Done
        driver.bothPass()

        val tokens = driver.getCreatures(you)
        tokens.size shouldBe 1
        val token = tokens.single()
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
        driver.state.projectedState.hasSubtype(token, "Zombie") shouldBe true
        driver.getGraveyard(you).contains(reap) shouldBe true
    }

    test("flashback {4}{U} casts it from the graveyard and exiles it") {
        val driver = newDriver()
        val you = driver.activePlayer!!

        val reap = driver.putCardInGraveyard(you, "Reap the Seagraf")
        driver.giveMana(you, Color.BLUE, 5)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = reap,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getCreatures(you).size shouldBe 1
        driver.getExile(you).contains(reap) shouldBe true
    }
})
