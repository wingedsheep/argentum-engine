package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ob Nixilis, Captive Kingpin — "Whenever one or more opponents each lose exactly 1 life, put a
 * +1/+1 counter on Ob Nixilis. Exile the top card of your library. Until your next end step, you
 * may play that card."
 *
 * Driven through real combat damage, so the ruling that simultaneous combat damage is summed per
 * opponent (two 1/1s connecting is a loss of 2) is exercised end to end.
 */
class ObNixilisCaptiveKingpinScenarioTest : ScenarioTestBase() {

    private fun TestGame.obCounters(): Int {
        val ob = findPermanent("Ob Nixilis, Captive Kingpin")!!
        return state.getEntity(ob)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun scenarioWith(vararg attackers: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Ob Nixilis, Captive Kingpin")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        attackers.forEach { builder.withCardOnBattlefield(1, it, tapped = false, summoningSickness = false) }
        return builder.build()
    }

    init {
        test("an opponent dealt exactly 1 combat damage grows Ob Nixilis and impulses the top card") {
            val g = scenarioWith("Savannah Lions")
            val topCard = g.state.getLibrary(g.player1Id).first()

            g.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            g.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
            g.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            g.getLifeTotal(2) shouldBe 19
            g.obCounters() shouldBe 1
            g.state.getExile(g.player1Id) shouldBe listOf(topCard)

            g.execute(PlayLand(g.player1Id, topCard)).error shouldBe null
            g.state.getBattlefield().contains(topCard) shouldBe true
        }

        test("two 1/1s connecting at once are a loss of 2 — no trigger") {
            val g = scenarioWith("Savannah Lions", "Llanowar Elves")

            g.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            g.declareAttackers(mapOf("Savannah Lions" to 2, "Llanowar Elves" to 2)).error shouldBe null
            g.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            g.getLifeTotal(2) shouldBe 18
            g.obCounters() shouldBe 0
            g.state.getExile(g.player1Id) shouldBe emptyList()
        }

        test("first-strike and regular combat damage are separate occurrences — triggers twice") {
            val g = scenarioWith("Tundra Wolves", "Savannah Lions")

            g.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            g.declareAttackers(mapOf("Tundra Wolves" to 2, "Savannah Lions" to 2)).error shouldBe null
            g.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            g.getLifeTotal(2) shouldBe 18
            g.obCounters() shouldBe 2
            g.state.getExile(g.player1Id).size shouldBe 2
        }
    }
}
