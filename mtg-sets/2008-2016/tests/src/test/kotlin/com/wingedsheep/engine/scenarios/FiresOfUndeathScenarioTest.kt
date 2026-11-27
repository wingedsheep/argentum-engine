package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FiresOfUndeathScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast from hand, it deals 2 damage to target player") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val fires = driver.putCardInHand(you, "Fires of Undeath")
        driver.giveMana(you, Color.RED, 3)
        driver.castSpell(you, fires, listOf(opponent)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getGraveyard(you).contains(fires) shouldBe true
    }

    test("flashback {5}{B} casts it from the graveyard at a creature and exiles it") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val fires = driver.putCardInGraveyard(you, "Fires of Undeath")
        driver.giveMana(you, Color.BLACK, 6)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = fires,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(opponent).contains(bears) shouldBe true
        driver.getExile(you).contains(fires) shouldBe true
        driver.getGraveyard(you).contains(fires) shouldBe false
    }
})
