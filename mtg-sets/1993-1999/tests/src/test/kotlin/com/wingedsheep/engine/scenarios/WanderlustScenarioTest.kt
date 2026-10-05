package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Wanderlust
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WanderlustScenarioTest : FunSpec({
    test("damages the enchanted creature's controller only on that player's upkeep") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Wanderlust)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val victim = driver.getOpponent(owner)
        val creature = driver.putCreatureOnBattlefield(victim, "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(owner, "Wanderlust")
        driver.addComponent(aura, AttachedToComponent(creature))
        driver.addComponent(creature, AttachmentsComponent(listOf(aura)))

        driver.passPriorityUntil(Step.END)
        driver.getLifeTotal(victim) shouldBe 20
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim
        driver.bothPass()
        driver.getLifeTotal(victim) shouldBe 19
        driver.getLifeTotal(owner) shouldBe 20

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe owner
        driver.state.stack.isEmpty() shouldBe true
        driver.getLifeTotal(victim) shouldBe 19
        driver.getLifeTotal(owner) shouldBe 20
    }
})
