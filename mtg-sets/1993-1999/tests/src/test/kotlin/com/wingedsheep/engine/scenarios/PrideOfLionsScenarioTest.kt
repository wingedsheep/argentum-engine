package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PrideOfLionsScenarioTest : FunSpec({
    for (accept in listOf(false, true)) {
        test("blocked Pride of Lions assigns all four damage to chosen destination, bypass=$accept") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all)
            d.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val lions = d.putCreatureOnBattlefield(d.player1, "Pride of Lions")
            d.removeSummoningSickness(lions)
            val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(lions), d.player2)
            d.bothPass()
            d.declareBlockers(d.player2, mapOf(giant to listOf(lions)))
            d.bothPass()
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
            d.submitYesNo(d.player1, accept).error shouldBe null
            d.getLifeTotal(d.player2) shouldBe if (accept) 16 else 20
            (giant in d.state.getBattlefield()) shouldBe accept
            (lions in d.state.getBattlefield()) shouldBe true
            // Assigning as unblocked does not prevent the blocker's damage.
            d.state.getEntity(lions)!!.get<DamageComponent>()!!.amount shouldBe 3
        }
    }
    test("Pride of Lions still offers bypass after its blocker leaves") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val lions = d.putCreatureOnBattlefield(d.player1, "Pride of Lions")
        d.removeSummoningSickness(lions)
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(lions), d.player2)
        d.bothPass()
        d.declareBlockers(d.player2, mapOf(giant to listOf(lions)))
        d.moveToGraveyard(giant)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 16
    }
    test("unblocked Pride of Lions deals damage without asking") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val lions = d.putCreatureOnBattlefield(d.player1, "Pride of Lions")
        d.removeSummoningSickness(lions)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(lions), d.player2)
        d.bothPass()
        d.declareBlockers(d.player2, emptyMap())
        d.bothPass()
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 16
    }
})
