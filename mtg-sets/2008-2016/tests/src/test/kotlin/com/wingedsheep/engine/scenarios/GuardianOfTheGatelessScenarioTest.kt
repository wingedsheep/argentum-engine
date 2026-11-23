package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.GuardianOfTheGateless
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Guardian of the Gateless (GTC #14) — "can block any number of creatures. Whenever this creature
 * blocks, it gets +1/+1 until end of turn for each creature it's blocking."
 *
 * The pump counts attackers through a source-relative `blockedBySource()` battlefield count, so the
 * test pins that the count reads the Guardian's own blocks (three attackers → +3/+3), that the blocks
 * trigger fires once rather than once per blocked attacker, and that a single block gives +1/+1.
 */
class GuardianOfTheGatelessScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + GuardianOfTheGateless)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.attackers(count: Int): List<EntityId> =
        (1..count).map {
            putCreatureOnBattlefield(player1, "Grizzly Bears").also { id -> removeSummoningSickness(id) }
        }

    test("blocking three creatures triggers once and gives +3/+3") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val bears = d.attackers(3)
        val guardian = d.putCreatureOnBattlefield(p2, "Guardian of the Gateless")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, bears, p2).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("it can block any number of creatures") {
            d.declareBlockers(p2, mapOf(guardian to bears)).outcome shouldBe Outcome.Done
        }
        withClue("'whenever this creature blocks' is one trigger, not one per attacker") {
            d.stackSize shouldBe 1
        }

        d.bothPass()
        d.state.projectedState.getPower(guardian) shouldBe 6
        d.state.projectedState.getToughness(guardian) shouldBe 6
    }

    test("blocking a single creature gives +1/+1") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val bears = d.attackers(2)
        val guardian = d.putCreatureOnBattlefield(p2, "Guardian of the Gateless")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, bears, p2).outcome shouldBe Outcome.Done
        d.bothPass()

        d.declareBlockers(p2, mapOf(guardian to listOf(bears[0]))).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("only the attacker it is blocking counts, not every attacker") {
            d.state.projectedState.getPower(guardian) shouldBe 4
            d.state.projectedState.getToughness(guardian) shouldBe 4
        }
    }
})
