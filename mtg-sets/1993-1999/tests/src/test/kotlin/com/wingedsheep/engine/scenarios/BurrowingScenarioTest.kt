package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Burrowing
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BurrowingScenarioTest : FunSpec({
    listOf(true, false).forEach { defendingMountain ->
        test("mountainwalk prevents blocking only with a defending Mountain: $defendingMountain") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(Burrowing)
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val attacker = driver.activePlayer!!
            val defender = driver.getOpponent(attacker)
            val bear = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
            val blocker = driver.putCreatureOnBattlefield(defender, "Hill Giant")
            val aura = driver.putPermanentOnBattlefield(attacker, "Burrowing")
            driver.addComponent(aura, AttachedToComponent(bear))
            driver.addComponent(bear, AttachmentsComponent(listOf(aura)))
            driver.putLandOnBattlefield(if (defendingMountain) defender else attacker, "Mountain")
            driver.removeSummoningSickness(bear)

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(attacker, listOf(bear), defender).error shouldBe null
            driver.bothPass()
            val result = driver.declareBlockers(defender, mapOf(blocker to listOf(bear)))
            (result.error != null) shouldBe defendingMountain
        }
    }
})
