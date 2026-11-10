package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vines of the Recluse — {G} Instant.
 * "Target creature gets +1/+2 and gains reach until end of turn. Untap it."
 */
class VinesOfTheRecluseScenarioTest : ScenarioTestBase() {
    init {
        test("a tapped creature gets +1/+2, gains reach, and untaps") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Vines of the Recluse")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpell(1, "Vines of the Recluse", bears)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 4
            projected.hasKeyword(bears, Keyword.REACH) shouldBe true
            withClue("untapped") { game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false }
        }

        test("works on an opponent's creature too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Vines of the Recluse")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Vines of the Recluse", giant).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(giant) shouldBe 4
            game.state.projectedState.getToughness(giant) shouldBe 5
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
        }
    }
}
