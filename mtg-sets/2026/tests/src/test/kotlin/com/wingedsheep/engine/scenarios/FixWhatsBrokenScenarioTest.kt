package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fix What's Broken {2}{W}{B} Sorcery (Secrets of Strixhaven #188).
 *
 * "As an additional cost to cast this spell, pay X life.
 *  Return each artifact and creature card with mana value X from your graveyard to the battlefield."
 *
 * X is declared at cast time (paid as life via [com.wingedsheep.sdk.scripting.AdditionalCost.PayXLife])
 * and the same X drives the resolution filter `manaValueEqualsX()`
 * ("mana value X", exactly). Mass reanimation gathers every matching artifact/creature card from the
 * caster's graveyard and returns them to the battlefield.
 */
class FixWhatsBrokenScenarioTest : FunSpec({

    fun GameTestDriver.resolveStack() {
        var safety = 0
        while (stackSize > 0 && !isPaused && safety < 20) {
            bothPass(); safety++
        }
    }

    test("pay 2 life returns the MV-2 artifact and creature; MV 0 and MV 4 cards stay in the graveyard") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Graveyard: a MV-2 creature and a MV-2 artifact (return on X=2),
        // a MV-4 creature and a MV-0 artifact creature (stay).
        driver.putCardInGraveyard(player, "Grizzly Bears")  // MV 2 creature -> return
        driver.putCardInGraveyard(player, "Mind Stone")     // MV 2 artifact -> return
        driver.putCardInGraveyard(player, "Hill Giant")     // MV 4 creature -> stay
        driver.putCardInGraveyard(player, "Ornithopter")    // MV 0 artifact creature -> stay

        // Mana for {2}{W}{B}.
        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }
        driver.putLandOnBattlefield(player, "Plains")
        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }

        val spell = driver.putCardInHand(player, "Fix What's Broken")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = spell,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2),
            )
        )
        driver.resolveStack()

        // Paid 2 life.
        driver.getLifeTotal(player) shouldBe 18

        // MV-2 artifact and creature are reanimated onto the battlefield.
        (driver.findPermanent(player, "Grizzly Bears") != null) shouldBe true
        (driver.findPermanent(player, "Mind Stone") != null) shouldBe true

        // MV-4 and MV-0 cards stay in the graveyard (alongside the resolved sorcery itself).
        (driver.findPermanent(player, "Hill Giant") == null) shouldBe true
        (driver.findPermanent(player, "Ornithopter") == null) shouldBe true
        val gy = driver.getGraveyardCardNames(player).toSet()
        gy.contains("Hill Giant") shouldBe true
        gy.contains("Ornithopter") shouldBe true
        gy.contains("Grizzly Bears") shouldBe false
        gy.contains("Mind Stone") shouldBe false
    }

    test("pay 0 life returns only MV-0 cards") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInGraveyard(player, "Ornithopter")   // MV 0 -> return
        driver.putCardInGraveyard(player, "Grizzly Bears")  // MV 2 -> stay

        repeat(3) { driver.putLandOnBattlefield(player, "Swamp") }
        driver.putLandOnBattlefield(player, "Plains")

        val spell = driver.putCardInHand(player, "Fix What's Broken")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = spell,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 0),
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(player) shouldBe 20
        (driver.findPermanent(player, "Ornithopter") != null) shouldBe true
        val gy = driver.getGraveyardCardNames(player).toSet()
        gy.contains("Grizzly Bears") shouldBe true
        gy.contains("Ornithopter") shouldBe false
    }
})
