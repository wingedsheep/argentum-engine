package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Masako the Humorless (CHK #33) — "Flash / Tapped creatures you control can block as though they
 * were untapped."
 *
 * Ruling: it lets a tapped creature block only if it could otherwise block.
 */
class MasakoTheHumorlessScenarioTest : ScenarioTestBase() {

    private fun TestGame.validBlockers() =
        getLegalActions(2).single { it.actionType == "DeclareBlockers" }.validBlockers.orEmpty()

    private fun TestGame.toDeclareBlockers(attacker: String) {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf(attacker to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
    }

    init {
        context("Masako the Humorless") {

            test("a tapped creature you control can block, and deals combat damage while tapped") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Masako the Humorless")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.toDeclareBlockers("Raging Goblin")
                withClue("the tapped Bears are offered as a blocker") { game.validBlockers() shouldContain bears }
                withClue("the tapped Bears carry the can-block badge") {
                    game.getClientState(2).cards[bears]!!.activeEffects
                        .map { it.effectId } shouldContain "can_block_while_tapped"
                }
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Raging Goblin"))).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.checkStateBasedActions()

                withClue("the tapped blocker dealt its damage and the Goblin died") {
                    game.isInGraveyard(1, "Raging Goblin") shouldBe true
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("blocking didn't untap it") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("Masako herself can block while tapped") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Masako the Humorless", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toDeclareBlockers("Raging Goblin")
                game.validBlockers() shouldContain game.findPermanent("Masako the Humorless")!!
                game.declareBlockers(mapOf("Masako the Humorless" to listOf("Raging Goblin"))).error shouldBe null
            }

            test("without Masako a tapped creature can't block") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toDeclareBlockers("Raging Goblin")
                game.validBlockers() shouldNotContain game.findPermanent("Grizzly Bears")!!
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Raging Goblin"))).error shouldNotBe null
            }

            test("an opponent's Masako doesn't let your tapped creatures block") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(1, "Masako the Humorless")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.toDeclareBlockers("Raging Goblin")
                game.validBlockers() shouldNotContain bears
                game.getClientState(2).cards[bears]!!.activeEffects
                    .map { it.effectId } shouldNotContain "can_block_while_tapped"
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Raging Goblin"))).error shouldNotBe null
            }

            test("ruling: a tapped creature that can't block still can't") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Masako the Humorless")
                    .withCardOnBattlefield(2, "Jungle Lion", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toDeclareBlockers("Raging Goblin")
                game.validBlockers() shouldNotContain game.findPermanent("Jungle Lion")!!
                game.declareBlockers(mapOf("Jungle Lion" to listOf("Raging Goblin"))).error shouldNotBe null
            }

            test("ruling: a tapped creature without flying or reach still can't block a flyer") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(2, "Masako the Humorless")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.toDeclareBlockers("Wind Drake")
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Wind Drake"))).error shouldNotBe null
            }
        }
    }
}
