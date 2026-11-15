package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Goblin Heelcutter (Fate Reforged #102) — {3}{R} Creature — Goblin Berserker 3/2.
 *
 * "Whenever this creature attacks, target creature can't block this turn. Dash {2}{R}"
 */
class GoblinHeelcutterScenarioTest : ScenarioTestBase() {
    init {
        test("attack trigger stops the targeted creature from blocking; others still can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goblin Heelcutter")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Goblin Heelcutter" to 2)).error shouldBe null
            game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("the targeted creature can't block") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Goblin Heelcutter"))).error shouldNotBe null
            }
            withClue("an untargeted creature still can") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Goblin Heelcutter"))).error shouldBe null
            }
        }
    }
}
