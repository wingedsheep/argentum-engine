package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BushiTenderfoot
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bushi Tenderfoot // Kenzo the Hardhearted (CHK) — a flip card.
 *
 * "When a creature dealt damage by this creature this turn dies, flip this creature."
 * Kenzo: "Double strike; bushido 2."
 */
class BushiTenderfootScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BushiTenderfoot)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    test("killing a blocker it damaged flips it into Kenzo the Hardhearted") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)

        val bushi = d.putCreatureOnBattlefield(me, "Bushi Tenderfoot")
        d.removeSummoningSickness(bushi)
        val bird = d.putCreatureOnBattlefield(opp, "Birds of Paradise") // 0/1

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bushi), opp)
        d.bothPass()
        d.declareBlockers(opp, mapOf(bird to listOf(bushi)))
        d.bothPass() // end of declare blockers
        d.bothPass() // combat damage
        d.drainStack()

        withClue("the bird died to Bushi's combat damage") {
            d.state.getBattlefield().contains(bird) shouldBe false
        }
        d.name(bushi) shouldBe "Kenzo the Hardhearted"
        d.state.projectedState.isLegendary(bushi) shouldBe true
        d.state.projectedState.hasKeyword(bushi, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("a creature it never damaged dying does not flip it") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)

        val bushi = d.putCreatureOnBattlefield(me, "Bushi Tenderfoot")
        val bird = d.putCreatureOnBattlefield(opp, "Birds of Paradise")

        d.moveToGraveyard(bird)
        d.drainStack()

        d.name(bushi) shouldBe "Bushi Tenderfoot"
    }

    test("Kenzo gets bushido 2 when blocked and kills a 3/3 blocker with first-strike damage") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)

        val bushi = d.putCreatureOnBattlefield(me, "Bushi Tenderfoot")
        d.removeSummoningSickness(bushi)
        val bird = d.putCreatureOnBattlefield(opp, "Birds of Paradise")

        // Flip it first, as in the first test.
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bushi), opp)
        d.bothPass()
        d.declareBlockers(opp, mapOf(bird to listOf(bushi)))
        d.bothPass()
        d.bothPass()
        d.drainStack()
        d.name(bushi) shouldBe "Kenzo the Hardhearted"

        // Next own turn: attack into a Centaur Courser (3/3).
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // opponent's turn
        d.passPriorityUntil(Step.END) // opponent's end step
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // back to ours
        d.state.activePlayerId shouldBe me
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bushi), opp)
        d.bothPass()
        d.declareBlockers(opp, mapOf(courser to listOf(bushi)))
        d.drainStack() // bushido trigger resolves

        d.state.projectedState.getPower(bushi) shouldBe 5
        d.state.projectedState.getToughness(bushi) shouldBe 6

        d.passPriorityUntil(Step.END_COMBAT)
        d.state.getBattlefield().contains(courser) shouldBe false
        d.state.getBattlefield().contains(bushi) shouldBe true
    }
})
