package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rampaging Geoderm (MOM #251) — "Whenever you attack, target attacking creature gets +1/+1 until
 * end of turn. If it's attacking a battle, put a +1/+1 counter on it instead."
 */
class RampagingGeodermScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Rampaging Geoderm")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    private fun TestGame.bears() = findPermanent("Grizzly Bears")!!

    private fun TestGame.plusOneCounters() =
        state.getEntity(bears())?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.targetBearsAndResolve() {
        state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        selectTargets(listOf(bears())).error shouldBe null
        resolveStack()
    }

    init {
        test("an attacker aimed at a battle gets a +1/+1 counter instead of the pump") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Rampaging Geoderm" to 2),
                permanentAttackers = mapOf("Grizzly Bears" to "Invasion of Innistrad")
            ).error shouldBe null
            game.targetBearsAndResolve()

            withClue("one +1/+1 counter, no extra until-end-of-turn pump") {
                game.plusOneCounters() shouldBe 1
                game.state.projectedState.getPower(game.bears()) shouldBe 3
            }
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the counter outlasts the turn") {
                game.state.projectedState.getPower(game.bears()) shouldBe 3
            }
        }

        test("an attacker aimed at a player gets +1/+1 until end of turn and no counter") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Rampaging Geoderm" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.targetBearsAndResolve()

            game.plusOneCounters() shouldBe 0
            game.state.projectedState.getPower(game.bears()) shouldBe 3
            game.state.projectedState.getToughness(game.bears()) shouldBe 3
        }
    }
}
