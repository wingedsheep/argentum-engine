package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * War Historian (MOM #214) — "This creature has indestructible as long as it attacked a battle this
 * turn." Per the 2023-04-14 ruling it applies from declaration for the whole turn, whatever happens
 * to the battle afterwards.
 */
class WarHistorianScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "War Historian", summoningSickness = false)
        .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    private fun TestGame.indestructible(): Boolean =
        state.projectedState.hasKeyword(findPermanent("War Historian")!!, Keyword.INDESTRUCTIBLE)

    init {
        test("attacking a battle makes it indestructible for the rest of the turn, then it wears off") {
            val game = board()
            withClue("not before it attacks") { game.indestructible() shouldBe false }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("War Historian" to "Invasion of Innistrad")
            ).error shouldBe null
            withClue("from declaration") { game.indestructible() shouldBe true }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("still after combat, no longer attacking") { game.indestructible() shouldBe true }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("next turn the record has been cleared") { game.indestructible() shouldBe false }
        }

        test("attacking a player doesn't count") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("War Historian" to 2)).error shouldBe null
            game.indestructible() shouldBe false
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.indestructible() shouldBe false
        }
    }
}
