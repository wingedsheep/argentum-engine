package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Duty Beyond Death. */
class DutyBeyondDeathScenarioTest : ScenarioTestBase() {

    init {
        context("Duty Beyond Death") {

            test("sacrifice cost, grants indestructible and a +1/+1 counter to each creature you control") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Duty Beyond Death")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Glory Seeker") // sacrifice fodder
                    .withCardOnBattlefield(1, "Marshal of the Lost") // survivor
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithAdditionalSacrifice(1, "Duty Beyond Death", sacrificeCreatureName = "Glory Seeker")
                game.resolveStack()

                val marshal = game.findPermanent("Marshal of the Lost")!!
                val counters = game.state.getEntity(marshal)?.get<CountersComponent>()
                withClue("Surviving creature should have a +1/+1 counter") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
                }
                withClue("Surviving creature should have indestructible until end of turn") {
                    game.getClientState(1).cards[marshal]?.keywords?.contains(Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("Glory Seeker was sacrificed as a cost") {
                    game.findPermanent("Glory Seeker") shouldBe null
                }
            }
        }
    }
}
