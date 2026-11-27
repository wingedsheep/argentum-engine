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
import io.kotest.matchers.shouldNotBe

class BurningOilScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("it can't target a creature that isn't attacking or blocking") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val oil = driver.putCardInHand(you, "Burning Oil")
        driver.giveMana(you, Color.RED, 2)
        driver.castSpell(you, oil, listOf(courser)).outcome shouldNotBe Outcome.Done
    }

    test("flashback {3}{W} from the graveyard deals 3 to an attacking creature and exiles it") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.removeSummoningSickness(courser)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(courser), opponent).error shouldBe null

        val oil = driver.putCardInGraveyard(you, "Burning Oil")
        driver.giveMana(you, Color.WHITE, 4)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = oil,
                targets = listOf(ChosenTarget.Permanent(courser)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(you).contains(courser) shouldBe true
        driver.getExile(you).contains(oil) shouldBe true
    }
})
