package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bond of Discipline (WAR #6) — {4}{W} Sorcery.
 *
 *   Tap all creatures your opponents control. Creatures you control gain lifelink until end of turn.
 */
class BondOfDisciplineScenarioTest : ScenarioTestBase() {

    init {
        test("taps the opponents' creatures and gives only your creatures lifelink") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Bond of Discipline")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Bond of Discipline").error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
        }
    }
}
