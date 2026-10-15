package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Quick-Draw Dagger (SNC #243) — "Flash. When this Equipment enters, attach it to target creature
 * you control. That creature gains first strike until end of turn. Equipped creature gets +1/+1."
 *
 * Pins the ETB attach plus the temporary first strike, and that the first strike ends with the turn
 * while the +1/+1 stays.
 */
class QuickDrawDaggerScenarioTest : ScenarioTestBase() {

    init {
        test("entering attaches to your creature, granting +1/+1 and first strike until end of turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Quick-Draw Dagger")
                .withLandsOnBattlefield(1, "Plains", 3)
                .apply { repeat(3) { withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Quick-Draw Dagger").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
            game.resolveStack()

            val dagger = game.findPermanent("Quick-Draw Dagger")!!
            game.state.getEntity(dagger)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // opponent's turn
            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
            game.state.projectedState.getPower(bears) shouldBe 3
        }
    }
}
