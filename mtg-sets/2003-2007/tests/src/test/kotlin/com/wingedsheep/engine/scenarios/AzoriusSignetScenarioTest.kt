package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.AzoriusSignet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Azorius Signet: {1}, {T}: Add {W}{U}. The {1} must actually be paid.
 */
class AzoriusSignetScenarioTest : FunSpec({

    val signetAbilityId = AzoriusSignet.activatedAbilities[0].id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AzoriusSignet)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with a floating {1}, Azorius Signet adds one white and one blue") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Azorius Signet")
        driver.giveColorlessMana(player, 1)

        val result = driver.submit(
            ActivateAbility(player, signet, signetAbilityId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldBe Outcome.Done
        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.white shouldBe 1
        pool.blue shouldBe 1
        pool.colorless shouldBe 0
    }

    test("the Signet cannot pay for its own {1}") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Azorius Signet")

        val result = driver.submit(ActivateAbility(player, signet, signetAbilityId))
        (result.outcome == Outcome.Done) shouldBe false
        driver.state.getEntity(signet)!!.has<TappedComponent>() shouldBe false
    }
})
