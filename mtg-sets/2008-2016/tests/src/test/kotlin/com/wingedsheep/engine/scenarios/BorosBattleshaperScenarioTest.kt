package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Boros Battleshaper (DGM #58) — {5}{R}{W} Creature — Minotaur Soldier 5/5.
 *
 *   At the beginning of each combat, up to one target creature attacks or blocks this combat if
 *   able and up to one target creature can't attack or block this combat.
 *
 * Covers the active player's forced creature having to attack, an opponent's forced creature having
 * to block, the trigger on the opponent's combat too, the second target sitting combat out, and both
 * effects ending with the combat phase.
 */
class BorosBattleshaperScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseBoth(forced: EntityId?, restrained: EntityId?) {
        (getPendingDecision() is ChooseTargetsDecision) shouldBe true
        val decisionId = state.pendingDecision!!.id
        submitDecision(
            TargetsResponse(decisionId, mapOf(0 to listOfNotNull(forced), 1 to listOfNotNull(restrained)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        context("Boros Battleshaper") {

            test("the active player's forced creature must attack; the restrained creature can't block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boros Battleshaper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.chooseBoth(forced = bears, restrained = giant)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the Bears must attack") {
                    game.declareAttackers(emptyMap()).error shouldNotBe null
                }
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the Hill Giant can't block this combat") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                game.declareNoBlockers().error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("both effects end with the combat phase") {
                    game.state.projectedState.mustAttack(bears) shouldBe false
                    game.state.floatingEffects.none { giant in it.effect.affectedEntities } shouldBe true
                }
            }

            test("an opponent's forced creature must block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boros Battleshaper")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.chooseBoth(forced = bears, restrained = null)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the Bears must block") {
                    game.declareNoBlockers().error shouldNotBe null
                }
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            }

            test("it triggers on the opponent's combat too: their forced creature attacks, the restrained one sits out") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boros Battleshaper")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.chooseBoth(forced = giant, restrained = bears)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the Hill Giant must attack") {
                    game.declareAttackers(emptyMap()).error shouldNotBe null
                }
                withClue("the Bears can't attack this combat") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldNotBe null
                }
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            }

            test("the same creature chosen for both targets can't attack, and is excused from attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boros Battleshaper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.chooseBoth(forced = bears, restrained = bears)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldNotBe null
                game.declareAttackers(emptyMap()).error shouldBe null
            }
        }
    }
}
