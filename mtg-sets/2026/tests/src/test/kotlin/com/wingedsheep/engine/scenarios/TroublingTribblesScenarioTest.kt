package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.TroublingTribbles
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TroublingTribblesScenarioTest : FunSpec({
    test("upkeep creates a copy even after its source token ceases to exist") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TroublingTribbles)
        driver.initMirrorMatch(Deck.of("Plains" to 40))
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val original = driver.putCreatureOnBattlefield(p1, "Troubling Tribbles")

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP, p1)
        driver.bothPass()
        val token = driver.state.getBattlefield().single {
            it != original && driver.state.getEntity(it)?.has<TokenComponent>() == true
        }

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP, p1)
        // Order the two identical upkeep triggers, if the engine requests an order.
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        val removal = driver.putCardInHand(p1, "Shock")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpellWithTargets(p1, removal, listOf(ChosenTarget.Permanent(token)))
        driver.bothPass()
        driver.state.getEntity(token) shouldBe null
        repeat(2) { driver.bothPass() }

        driver.state.getBattlefield().count {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Troubling Tribbles"
        } shouldBe 3
        driver.findPermanent(p1, "Troubling Tribbles") shouldNotBe null
    }
})
