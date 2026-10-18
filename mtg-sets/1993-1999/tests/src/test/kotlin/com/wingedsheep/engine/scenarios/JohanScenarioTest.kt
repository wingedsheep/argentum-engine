package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Johan — {3}{R}{G}{W} Legendary Creature — Human Wizard 5/4.
 * "At the beginning of combat on your turn, you may have Johan gain "Johan can't attack" until end of
 * combat. If you do, attacking doesn't cause creatures you control to tap this combat if Johan is
 * untapped."
 *
 * Pinned: accepting keeps Johan home and lets the others attack untapped; declining changes nothing;
 * the "if Johan is untapped" clause is read when attackers are declared, so tapping Johan — or losing
 * him — after the trigger resolved makes the attackers tap as usual.
 */
class JohanScenarioTest : ScenarioTestBase() {

    private fun johanBoard(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("Johan", "Opponent")
            .withCardOnBattlefield(1, "Johan")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .extra()
            .apply { repeat(3) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Plains") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        withClue("Johan's trigger fires at the beginning of combat") { game.state.step shouldBe Step.BEGIN_COMBAT }
        return game
    }

    /** Resolve Johan's trigger, answering its "you may" with [accept]. */
    private fun TestGame.resolveJohan(accept: Boolean) {
        var answered = false
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 10) {
            if (state.pendingDecision is YesNoDecision) {
                answerYesNo(accept)
                answered = true
            } else {
                passPriority()
            }
        }
        withClue("the trigger asked whether to use it") { answered shouldBe true }
    }

    private fun TestGame.toDeclareAttackers() {
        var guard = 0
        while (state.step != Step.DECLARE_ATTACKERS && guard++ < 10) passPriority()
        state.step shouldBe Step.DECLARE_ATTACKERS
    }

    private fun TestGame.isTapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        context("Johan") {
            test("accepting lets creatures you control attack without tapping, and Johan can't attack") {
                val game = johanBoard()
                game.resolveJohan(accept = true)
                game.toDeclareAttackers()

                withClue("Johan gained \"Johan can't attack\"") {
                    game.declareAttackers(mapOf("Johan" to 2)).error shouldNotBe null
                }
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                withClue("attacking didn't tap them") {
                    game.isTapped("Grizzly Bears") shouldBe false
                    game.isTapped("Hill Giant") shouldBe false
                }
                withClue("and nothing gained vigilance") {
                    game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, "VIGILANCE") shouldBe false
                }
            }

            test("declining changes nothing: Johan may attack and attackers tap") {
                val game = johanBoard()
                game.resolveJohan(accept = false)
                game.toDeclareAttackers()

                game.declareAttackers(mapOf("Johan" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.isTapped("Johan") shouldBe true
                game.isTapped("Grizzly Bears") shouldBe true
            }

            test("if Johan is tapped when attackers are declared, attacking taps as usual") {
                val game = johanBoard()
                game.resolveJohan(accept = true)
                val johan = game.findPermanent("Johan")!!
                game.state = game.state.updateEntity(johan) { it.with(TappedComponent) }
                game.toDeclareAttackers()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.isTapped("Grizzly Bears") shouldBe true
            }

            test("if Johan has left the battlefield, attacking taps as usual") {
                val game = johanBoard {
                    withCardInHand(1, "Terror").withLandsOnBattlefield(1, "Swamp", 2)
                }
                game.resolveJohan(accept = true)
                game.castSpell(1, "Terror", game.findPermanent("Johan")!!).error shouldBe null
                game.resolveStack()
                withClue("Terror destroyed Johan") { game.findPermanent("Johan") shouldBe null }
                game.toDeclareAttackers()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.isTapped("Grizzly Bears") shouldBe true
            }

            test("the effect ends with the combat it was created in") {
                val game = johanBoard()
                game.resolveJohan(accept = true)
                game.toDeclareAttackers()
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("no attacks-without-tapping effect outlives end of combat") {
                    game.state.projectedState.attacksWithoutTapping(game.findPermanent("Hill Giant")!!) shouldBe false
                }
            }
        }
    }
}
