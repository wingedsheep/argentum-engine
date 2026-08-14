package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ordinary Bear (HOB) — {3}{G} Creature — Bear 4/5, no abilities.
 *
 * A vanilla creature's contract is that it is exactly its printed body and nothing more, so this
 * exercises the body in combat: unblocked it deals 4, and blocking a 3/3 it kills the blocker and
 * survives the return damage.
 */
class OrdinaryBearScenarioTest : ScenarioTestBase() {

    init {
        context("Ordinary Bear") {

            test("it is a plain 4/5 with no abilities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .build()

                val bear = game.findPermanent("Ordinary Bear")!!
                withClue("printed body") {
                    game.state.projectedState.getPower(bear) shouldBe 4
                    game.state.projectedState.getToughness(bear) shouldBe 5
                }
                val def = cardRegistry.requireCard("Ordinary Bear")
                withClue("a vanilla creature carries no rules text at all") {
                    def.triggeredAbilities.isEmpty() shouldBe true
                    def.activatedAbilities.isEmpty() shouldBe true
                    def.staticAbilities.isEmpty() shouldBe true
                    def.keywords.isEmpty() shouldBe true
                }
            }

            test("unblocked it deals 4 combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ordinary Bear" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                game.getLifeTotal(2) shouldBe 16
            }

            test("blocking a 3/3 it kills the attacker and survives") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Ordinary Bear" to listOf("Centaur Courser")))
                    .error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("4 power kills the 3/3") {
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                withClue("5 toughness survives 3 damage") {
                    game.isOnBattlefield("Ordinary Bear") shouldBe true
                }
                withClue("no damage reached the defending player") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
