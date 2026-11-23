package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Paralyzing Grasp (ZEN) — Enchanted creature doesn't untap during its controller's untap step.
 */
class ParalyzingGraspScenarioTest : ScenarioTestBase() {
    init {
        test("the enchanted creature stays tapped through its controller's untap step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardAttachedTo(1, "Paralyzing Grasp", "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            withClue("it is now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the unenchanted Hill Giant untapped as normal") {
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
            }
            withClue("the enchanted Bears stayed tapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
