package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Swift Spinner (EMN #173) — {3}{G} Creature — Spider, 2/3.
 *
 *   Flash
 *   Reach
 */
class SwiftSpinnerScenarioTest : ScenarioTestBase() {

    init {
        test("flash lets it be cast on the opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swift Spinner")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            val cast = game.castSpell(1, "Swift Spinner")
            withClue("casting at instant speed on the opponent's turn: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Swift Spinner") shouldBe true
            val spinner = game.findPermanent("Swift Spinner")!!
            game.state.projectedState.getPower(spinner) shouldBe 2
            game.state.projectedState.getToughness(spinner) shouldBe 3
        }

        test("reach lets it block a flying attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Swift Spinner")
                .withCardOnBattlefield(2, "Birds of Paradise") // flying
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Birds of Paradise" to 1)).error shouldBe null

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val result = game.declareBlockers(mapOf("Swift Spinner" to listOf("Birds of Paradise")))
            withClue("a reach creature can block a flier: ${result.error}") { result.error shouldBe null }
        }
    }
}
