package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Battle-Mad Ronin (CHK #156) — "Bushido 2 / This creature attacks each combat if able."
 */
class BattleMadRoninScenarioTest : ScenarioTestBase() {

    init {
        context("Battle-Mad Ronin") {

            test("it must attack each combat if able") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Battle-Mad Ronin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val noAttack = game.execute(DeclareAttackers(playerId = game.player1Id, attackers = emptyMap()))
                withClue("declaring no attackers is illegal") { noAttack.error shouldNotBe null }
                game.declareAttackers(mapOf("Battle-Mad Ronin" to 2)).error shouldBe null
            }

            test("bushido 2: becoming blocked makes it a 3/3 that survives a 2/2 blocker") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Battle-Mad Ronin")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ronin = game.findPermanent("Battle-Mad Ronin")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Battle-Mad Ronin" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Battle-Mad Ronin"))).error shouldBe null
                game.resolveStack()

                withClue("the becomes-blocked half of bushido pumped it to 3/3") {
                    game.state.projectedState.getPower(ronin) shouldBe 3
                    game.state.projectedState.getToughness(ronin) shouldBe 3
                }

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.checkStateBasedActions()

                withClue("the Ronin survives and the Bears die") {
                    game.isOnBattlefield("Battle-Mad Ronin") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
