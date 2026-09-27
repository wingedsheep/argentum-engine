package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.KondaLordOfEiganjo
import com.wingedsheep.mtg.sets.definitions.chk.cards.KondasHatamoto
import com.wingedsheep.mtg.sets.definitions.chk.cards.NumaiOutcast
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Konda's Hatamoto (CHK) — Bushido 1; as long as you control a legendary Samurai, it gets +1/+2
 * and has vigilance.
 */
class KondasHatamotoScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KondasHatamoto + KondaLordOfEiganjo + NumaiOutcast)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    test("alone it is a plain 1/2 without vigilance") {
        val d = driver()
        val h = d.putCreatureOnBattlefield(d.player1, "Konda's Hatamoto")
        d.state.projectedState.getPower(h) shouldBe 1
        d.state.projectedState.getToughness(h) shouldBe 2
        d.state.projectedState.hasKeyword(h, Keyword.VIGILANCE) shouldBe false
    }

    test("a non-legendary Samurai does not turn it on") {
        val d = driver()
        val h = d.putCreatureOnBattlefield(d.player1, "Konda's Hatamoto")
        d.putCreatureOnBattlefield(d.player1, "Numai Outcast")
        d.state.projectedState.getPower(h) shouldBe 1
        d.state.projectedState.getToughness(h) shouldBe 2
        d.state.projectedState.hasKeyword(h, Keyword.VIGILANCE) shouldBe false
    }

    test("an opponent's legendary Samurai does not turn it on") {
        val d = driver()
        val h = d.putCreatureOnBattlefield(d.player1, "Konda's Hatamoto")
        d.putCreatureOnBattlefield(d.getOpponent(d.player1), "Konda, Lord of Eiganjo")
        d.state.projectedState.getPower(h) shouldBe 1
        d.state.projectedState.hasKeyword(h, Keyword.VIGILANCE) shouldBe false
    }

    test("with a legendary Samurai it is a 2/4 with vigilance, and attacking does not tap it") {
        val d = driver()
        val me = d.player1
        val h = d.putCreatureOnBattlefield(me, "Konda's Hatamoto")
        d.removeSummoningSickness(h)
        val konda = d.putCreatureOnBattlefield(me, "Konda, Lord of Eiganjo")
        d.state.projectedState.getPower(h) shouldBe 2
        d.state.projectedState.getToughness(h) shouldBe 4
        d.state.projectedState.hasKeyword(h, Keyword.VIGILANCE) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(h), d.getOpponent(me))
        d.isTapped(h) shouldBe false

        d.moveToGraveyard(konda)
        d.state.projectedState.getPower(h) shouldBe 1
        d.state.projectedState.hasKeyword(h, Keyword.VIGILANCE) shouldBe false
    }

    test("bushido 1 pumps it when it becomes blocked") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)
        val h = d.putCreatureOnBattlefield(me, "Konda's Hatamoto")
        d.removeSummoningSickness(h)
        val bird = d.putCreatureOnBattlefield(opp, "Birds of Paradise")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(h), opp)
        d.bothPass()
        d.declareBlockers(opp, mapOf(bird to listOf(h)))
        d.drainStack()

        d.state.projectedState.getPower(h) shouldBe 2
        d.state.projectedState.getToughness(h) shouldBe 3
    }
})
