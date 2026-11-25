package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Odric, Master Tactician (M13 #23) — "First strike. Whenever Odric and at least three other
 * creatures attack, you choose which creatures block this combat and how those creatures block."
 *
 * With three companions the defender's block declaration goes to Odric's controller, who may keep
 * every blocker home or throw a blocker in front of Odric; with only two the trigger doesn't fire.
 */
class OdricMasterTacticianScenarioTest : ScenarioTestBase() {

    private fun board(companions: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Odric", "Defender")
            .withCardOnBattlefield(1, "Odric, Master Tactician")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        companions.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    private fun TestGame.advanceTo(step: Step) {
        var guard = 0
        while (state.step != step && guard++ < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
        }
        state.step shouldBe step
    }

    init {
        test("Odric and three others attack: you declare the defender's blocks") {
            val game = board(listOf("Grizzly Bears", "Savannah Lions", "Llanowar Elves"))
            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Odric, Master Tactician" to 2, "Grizzly Bears" to 2, "Savannah Lions" to 2, "Llanowar Elves" to 2)
            ).error shouldBe null
            game.resolveStack()
            game.advanceTo(Step.DECLARE_BLOCKERS)

            withClue("the defender no longer declares their own blocks") {
                game.getLegalActions(2).none { it.action is DeclareBlockers } shouldBe true
            }
            (game.getLegalActions(1).single { it.action is DeclareBlockers }.action as DeclareBlockers)
                .playerId shouldBe game.player2Id

            // Hill Giant (3/3) is sent in front of first-striking Odric (3/4) and dies before it deals damage.
            val blocks = mapOf(game.findPermanent("Hill Giant")!! to listOf(game.findPermanent("Odric, Master Tactician")!!))
            game.execute(DeclareBlockers(game.player2Id, blocks)).error shouldBe null
            val companions = listOf("Grizzly Bears", "Savannah Lions", "Llanowar Elves")
            val unblockedPower = companions.sumOf { game.state.projectedState.getPower(game.findPermanent(it)!!) ?: 0 }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.findPermanent("Hill Giant") shouldBe null
            (game.findPermanent("Odric, Master Tactician") != null) shouldBe true
            withClue("the three companions are unblocked") {
                game.getLifeTotal(2) shouldBe 20 - unblockedPower
            }
        }

        test("only two others attack: the defender declares blocks as usual") {
            val game = board(listOf("Grizzly Bears", "Savannah Lions"))
            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Odric, Master Tactician" to 2, "Grizzly Bears" to 2, "Savannah Lions" to 2)
            ).error shouldBe null
            game.resolveStack()
            game.advanceTo(Step.DECLARE_BLOCKERS)

            game.getLegalActions(1).none { it.action is DeclareBlockers } shouldBe true
            game.getLegalActions(2).any { it.action is DeclareBlockers } shouldBe true
        }
    }
}
