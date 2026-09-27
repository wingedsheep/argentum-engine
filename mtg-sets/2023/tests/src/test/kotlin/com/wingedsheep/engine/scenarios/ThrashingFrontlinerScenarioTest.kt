package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thrashing Frontliner (MOM #167) — "Whenever this creature attacks a battle, it gets +1/+1 until
 * end of turn." Attacking a player or a planeswalker must not trigger it (CR 508.1: the defender
 * is a player, planeswalker, or battle).
 */
class ThrashingFrontlinerScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Thrashing Frontliner", summoningSickness = false)
        .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
        .withCardOnBattlefield(2, "Jace Beleren")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    private fun TestGame.frontlinerPT(): Pair<Int?, Int?> {
        val id = findPermanent("Thrashing Frontliner")!!
        return state.projectedState.getPower(id) to state.projectedState.getToughness(id)
    }

    init {
        test("attacking a battle gives it +1/+1 until end of turn") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Thrashing Frontliner" to "Invasion of Innistrad")
            ).error shouldBe null
            game.resolveStack()
            withClue("the trigger resolved: 3/3") { game.frontlinerPT() shouldBe (3 to 3) }
        }

        test("attacking a player does not trigger") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Thrashing Frontliner" to 2)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.frontlinerPT() shouldBe (2 to 2)
        }

        test("attacking a planeswalker does not trigger") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Thrashing Frontliner" to "Jace Beleren")
            ).error shouldBe null
            game.state.stack.size shouldBe 0
            game.frontlinerPT() shouldBe (2 to 2)
        }
    }
}
