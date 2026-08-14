package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Crashing Wave (Avatar: The Last Airbender) — waterbend {X} plus the new
 * "distribute three stun counters among any number of tapped creatures your opponents control"
 * effect ([com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongFilteredEffect]).
 *
 * X (from the waterbend cost) bounds the targeting and is paid by tapping permanents. The spell taps
 * the targeted creatures, then the controller distributes 3 stun counters among the tapped creatures
 * opponents control — created fresh (nothing removed from the spell).
 */
class CrashingWaveScenarioTest : ScenarioTestBase() {

    init {
        test("taps X target creatures (waterbend paid by tapping) then distributes 3 stun among tapped opponent creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Crashing Wave")
                .withLandsOnBattlefield(1, "Island", 2)          // base {U}{U}
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(1, "Glory Seeker")          // tap both to pay waterbend {2}
                .withCardOnBattlefield(2, "Badgermole Cub")        // opponent creatures to tap + stun
                .withCardOnBattlefield(2, "Iguana Parrot")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tappers = game.findAllPermanents("Glory Seeker")
            tappers.size shouldBe 2
            val oppA = game.findPermanent("Badgermole Cub")!!
            val oppB = game.findPermanent("Iguana Parrot")!!

            val action = game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.isAffordable && it.hasTapForGeneric
            }
            withClue("Crashing Wave should be offered as an X-carrying waterbend cast") {
                action shouldNotBe null
                action!!.hasXCost shouldBe true
            }

            // X = 2: target the two opponent creatures (to tap), pay waterbend {2} by tapping our Glory Seekers.
            val cast = (action!!.action as CastSpell).copy(
                xValue = 2,
                targets = listOf(ChosenTarget.Permanent(oppA), ChosenTarget.Permanent(oppB)),
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = tappers.toSet()),
            )
            val result = game.execute(cast)
            withClue("casting Crashing Wave for waterbend {X=2} should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            // The two targeted opponent creatures are tapped, so both are eligible for the stun split.
            withClue("both targeted opponent creatures are tapped") {
                game.state.getEntity(oppA)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(oppB)!!.has<TappedComponent>() shouldBe true
            }

            val decision = game.getPendingDecision()
            withClue("resolution pauses to distribute the 3 stun counters") {
                decision shouldNotBe null
                (decision is DistributeDecision) shouldBe true
            }
            game.submitDecision(
                DistributionResponse((decision as DistributeDecision).id, mapOf(oppA to 2, oppB to 1))
            )

            fun stun(id: com.wingedsheep.sdk.model.EntityId) =
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
            withClue("3 stun counters were created and distributed (2 + 1) among the tapped opponent creatures") {
                stun(oppA) shouldBe 2
                stun(oppB) shouldBe 1
            }
        }
    }
}
