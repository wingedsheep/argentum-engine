package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Chillbringer (RNA #33) — ETB: tap target creature an opponent controls; it doesn't untap during
 * its controller's next untap step.
 */
class ChillbringerScenarioTest : ScenarioTestBase() {
    init {
        test("the ETB taps the opponent's creature") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Chillbringer")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Chillbringer").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldNotBeNull()
            val giant = game.findPermanent("Hill Giant")!!
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
        }
    }
}
