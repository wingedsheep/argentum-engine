package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.plc.cards.ProdigalPyromancer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Questing Beast (ELD #171).
 *
 * The line worth pinning is "**Combat** damage that would be dealt by creatures you control can't
 * be prevented": it shuts off Fog for your attackers, but a creature's *noncombat* damage (Prodigal
 * Pyromancer's ping) stays preventable — the pattern's damage type scopes the shutoff, not just its
 * source.
 */
class QuestingBeastScenarioTest : ScenarioTestBase() {

    private fun board(opponentSpell: String, withJace: Boolean = false, blocker: String? = null): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Questing Beast")
            .withCardOnBattlefield(1, "Prodigal Pyromancer")
            .withCardInHand(2, opponentSpell)
            .withLandsOnBattlefield(2, "Forest", 3)
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { b = b.withCardInLibrary(2, "Forest") }
        if (withJace) b = b.withCardOnBattlefield(2, "Jace Beleren")
        if (blocker != null) b = b.withCardOnBattlefield(2, blocker)
        return b.build()
    }

    private fun TestGame.opponentCasts(spell: String) {
        castSpell(2, spell).error shouldBe null
        resolveStack()
    }

    private fun TestGame.attackWithBeast() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Questing Beast" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
    }

    init {
        context("Questing Beast") {

            test("Fog can't prevent its combat damage, and that much damage hits the opponent's planeswalker") {
                val game = board("Fog", withJace = true)
                game.opponentCasts("Fog")
                game.attackWithBeast()
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.findPermanent("Jace Beleren")!!)).error shouldBe null
                }
                game.resolveStack()
                withClue("Fog prevented nothing: the opponent took 4") { game.getLifeTotal(2) shouldBe 16 }
                withClue("the trigger dealt 4 to Jace Beleren (loyalty 3)") {
                    game.isInGraveyard(2, "Jace Beleren") shouldBe true
                }
            }

            test("a creature's noncombat damage stays preventable") {
                val game = board("Thwart the Enemy")
                game.opponentCasts("Thwart the Enemy")
                val pyromancer = game.findPermanent("Prodigal Pyromancer")!!
                game.execute(
                    ActivateAbility(
                        game.player1Id, pyromancer, ProdigalPyromancer.activatedAbilities.single().id,
                        listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()
                withClue("Thwart the Enemy prevented the Pyromancer's ping") { game.getLifeTotal(2) shouldBe 20 }

                game.attackWithBeast()
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("but not the Beast's combat damage") { game.getLifeTotal(2) shouldBe 16 }
            }

            test("Angel of Suffering can't prevent its combat damage: the opponent mills and still loses life") {
                val game = board("Fog", blocker = "Angel of Suffering")
                game.passPriority()
                val libraryBefore = game.librarySize(2)
                game.attackWithBeast()
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the replacement still mills twice the damage") {
                    game.librarySize(2) shouldBe libraryBefore - 8
                }
                withClue("but the damage isn't prevented") { game.getLifeTotal(2) shouldBe 16 }
            }

            test("can't be blocked by creatures with power 2 or less") {
                val game = board("Fog", blocker = "Grizzly Bears")
                game.passPriority()
                game.attackWithBeast()
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Questing Beast"))).error shouldNotBe null
            }
        }
    }
}
