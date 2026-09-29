package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Surrak and Goreclaw — {4}{G}{G} Legendary Creature — Human Bear 6/5.
 *   Trample
 *   Other creatures you control have trample.
 *   Whenever another nontoken creature you control enters, put a +1/+1 counter on it. It gains
 *   haste until end of turn.
 */
class SurrakAndGoreclawScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("a nontoken creature entering gets a +1/+1 counter, haste, and trample") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Surrak and Goreclaw")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            val surrak = game.findPermanent("Surrak and Goreclaw")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the entering creature gets the counter") { game.plusOneCounters(bears) shouldBe 1 }
            withClue("Surrak itself gets nothing") { game.plusOneCounters(surrak) shouldBe 0 }
            val projected = game.state.projectedState
            projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(surrak, Keyword.TRAMPLE) shouldBe true
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
        }

        test("tokens entering do not trigger, but still gain trample") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Surrak and Goreclaw")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            val soldiers = game.findAllPermanents("Soldier Token")
            soldiers.size shouldBe 2
            soldiers.forEach { soldier ->
                game.plusOneCounters(soldier) shouldBe 0
                game.state.projectedState.hasKeyword(soldier, Keyword.HASTE) shouldBe false
                game.state.projectedState.hasKeyword(soldier, Keyword.TRAMPLE) shouldBe true
            }
        }

        test("an opponent's creature entering does not trigger and does not gain trample") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Surrak and Goreclaw")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.plusOneCounters(bears) shouldBe 0
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
        }
    }
}
