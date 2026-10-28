package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.LRellFierceVisionary
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LRellFierceVisionaryScenarioTest : FunSpec({
    test("combat token has haste and is sacrificed at the next end step") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LRellFierceVisionary)
        driver.initMirrorMatch(Deck.of("Mountain" to 40))
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val lrell = driver.putCreatureOnBattlefield(p1, "L'Rell, Fierce Visionary")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.bothPass()
        val token = driver.state.getBattlefield().single {
            driver.state.getEntity(it)?.has<TokenComponent>() == true
        }
        val card = driver.state.getEntity(token)!!.get<CardComponent>()!!
        card.typeLine.subtypes.map { it.value }.toSet() shouldBe setOf("Klingon", "Warrior")
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 1
        driver.state.projectedState.getKeywords(token).contains(Keyword.HASTE.name) shouldBe true

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.state.getBattlefield().contains(token) shouldBe false
        driver.state.getBattlefield().contains(lrell) shouldBe true
    }
})
