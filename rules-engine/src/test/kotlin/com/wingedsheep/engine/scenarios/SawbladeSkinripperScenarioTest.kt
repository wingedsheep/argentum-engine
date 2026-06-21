package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.SawbladeSkinripper
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sawblade Skinripper (DSK #231) — {1}{B}{R} 3/2 Creature — Human Assassin, Menace.
 *
 * "{2}, Sacrifice another creature or enchantment: Put a +1/+1 counter on this creature.
 *  At the beginning of your end step, if you sacrificed one or more permanents this turn,
 *  this creature deals that much damage to any target."
 *
 * Exercises the new per-player `PERMANENTS_SACRIFICED` turn tracker: the end-step trigger reads
 * the controller-scoped count of permanents sacrificed this turn for both the intervening-if gate
 * and the "that much" damage amount.
 */
class SawbladeSkinripperScenarioTest : FunSpec({

    val abilityId = SawbladeSkinripper.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("activating the sacrifice ability adds a +1/+1 counter and end step deals that many damage") {
        val driver = newDriver()
        val sawblade = driver.putCreatureOnBattlefield(driver.player1, "Sawblade Skinripper")
        driver.removeSummoningSickness(sawblade)
        val fodder = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putPermanentOnBattlefield(driver.player1, "Mountain")
        driver.putPermanentOnBattlefield(driver.player1, "Mountain")

        driver.setLifeTotal(driver.player2, 20)

        // {2}, Sacrifice the Bears: put a +1/+1 counter on Sawblade.
        val result = driver.submit(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = sawblade,
                abilityId = abilityId,
                costPayment = null,
            )
        )
        result.error shouldBe null
        if (driver.pendingDecision != null) {
            driver.submitCardSelection(driver.player1, listOf(fodder))
        }
        driver.bothPass()

        driver.state.getEntity(sawblade)
            ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        driver.state.getBattlefield(driver.player1).contains(fodder) shouldBe false

        // End step: you sacrificed 1 permanent this turn → deal 1 damage to any target (opponent).
        driver.passPriorityUntil(Step.END)
        // The end-step trigger targets "any target"; choose the opponent.
        if (driver.pendingDecision != null) {
            driver.submitTargetSelection(driver.player1, listOf(driver.player2))
        }
        driver.bothPass()

        driver.getLifeTotal(driver.player2) shouldBe 19
    }

    test("no sacrifice this turn means the end-step trigger does not fire") {
        val driver = newDriver()
        val sawblade = driver.putCreatureOnBattlefield(driver.player1, "Sawblade Skinripper")
        driver.removeSummoningSickness(sawblade)
        driver.setLifeTotal(driver.player2, 20)

        driver.passPriorityUntil(Step.END)
        // No pending target decision should exist — the intervening-if gate is false.
        driver.bothPass()

        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
