package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * War-Trained Slasher (MOM #172) — "Whenever this creature attacks a battle, double its power until
 * end of turn."
 */
class WarTrainedSlasherScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "War-Trained Slasher", summoningSickness = false)
        .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    private fun TestGame.slasherPT(): Pair<Int?, Int?> {
        val id = findPermanent("War-Trained Slasher")!!
        return state.projectedState.getPower(id) to state.projectedState.getToughness(id)
    }

    init {
        test("attacking a battle doubles its power, toughness unchanged") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("War-Trained Slasher" to "Invasion of Innistrad")
            ).error shouldBe null
            game.resolveStack()
            withClue("4/3 doubled to 8/3") { game.slasherPT() shouldBe (8 to 3) }
        }

        test("attacking a player does not trigger") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("War-Trained Slasher" to 2)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.slasherPT() shouldBe (4 to 3)
        }
    }
}
