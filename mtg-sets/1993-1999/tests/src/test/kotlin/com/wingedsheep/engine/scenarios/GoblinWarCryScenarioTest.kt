package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goblin War Cry — the targeted opponent picks one creature; their others can't block this turn.
 */
class GoblinWarCryScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Goblin War Cry")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the opponent chooses; only the chosen creature can block") {
            val game = setup()
            game.castSpellTargetingPlayer(1, "Goblin War Cry", 2).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            withClue("the targeted opponent makes the choice") {
                decision.playerId shouldBe game.player2Id
            }
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("the other creature can't block") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Centaur Courser"))).error shouldNotBe null
            }
            withClue("the chosen creature still can") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser"))).error shouldBe null
            }
        }
    }
}
