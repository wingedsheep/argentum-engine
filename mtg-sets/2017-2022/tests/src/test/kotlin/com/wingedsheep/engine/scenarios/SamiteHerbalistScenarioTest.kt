package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SamiteHerbalistScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        val spell = putCardInHand(player1, "Lightning Bolt")
        castSpell(player1, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("attack tap gains life before scry even when the source dies in response") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Samite Herbalist")
        d.removeSummoningSickness(source)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(source), d.player2).outcome shouldBe Outcome.Done
        d.stackSize shouldBe 1
        d.bolt(source)
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 21
        d.getLifeTotal(d.player2) shouldBe 20
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(d.player1, CardsSelectedResponse(choice.id, emptyList()))
        d.pendingDecision shouldBe null
    }

    test("another creature becoming tapped does not trigger the Herbalist") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Samite Herbalist")
        val ally = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(ally)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(ally), d.player2).outcome shouldBe Outcome.Done
        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
        d.pendingDecision shouldBe null
    }
})
