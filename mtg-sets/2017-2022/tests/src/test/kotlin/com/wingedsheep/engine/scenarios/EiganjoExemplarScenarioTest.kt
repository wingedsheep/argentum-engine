package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eiganjo Exemplar (NEO #10) — "Whenever a Samurai or Warrior you control attacks alone, it gets
 * +1/+1 until end of turn."
 *
 * "it" is the lone attacker, not the Exemplar: a Warrior attacking alone gets the pump while the
 * Exemplar at home stays 2/1.
 */
class EiganjoExemplarScenarioTest : ScenarioTestBase() {

    init {
        context("Eiganjo Exemplar") {

            test("a Warrior attacking alone gets +1/+1, not the Exemplar") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Eiganjo Exemplar")
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3 Centaur Warrior
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val courser = game.findPermanent("Centaur Courser")!!
                val exemplar = game.findPermanent("Eiganjo Exemplar")!!
                withClue("the lone attacker is pumped to 4/4") {
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
                withClue("the Exemplar itself is untouched") {
                    game.state.projectedState.getPower(exemplar) shouldBe 2
                    game.state.projectedState.getToughness(exemplar) shouldBe 1
                }
            }

            test("the Exemplar attacking alone pumps itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Eiganjo Exemplar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Eiganjo Exemplar" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val exemplar = game.findPermanent("Eiganjo Exemplar")!!
                game.state.projectedState.getPower(exemplar) shouldBe 3
                game.state.projectedState.getToughness(exemplar) shouldBe 2
            }

            test("attacking with two creatures doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Eiganjo Exemplar")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Eiganjo Exemplar" to 2, "Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.state.projectedState.getPower(game.findPermanent("Centaur Courser")!!) shouldBe 3
                game.state.projectedState.getPower(game.findPermanent("Eiganjo Exemplar")!!) shouldBe 2
            }
        }
    }
}
