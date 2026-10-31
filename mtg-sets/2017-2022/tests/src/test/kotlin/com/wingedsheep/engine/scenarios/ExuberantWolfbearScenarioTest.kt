package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Exuberant Wolfbear (IKO #151, {3}{G} 4/4).
 *
 *   Whenever this creature attacks, you may have the base power and toughness of target Human you
 *   control become equal to this creature's power and toughness until end of turn.
 *
 * Pins that the Human copies the Wolfbear's *actual* power/toughness (anthem included) as its
 * layer-7b base, that the Human's own modifiers still apply on top, and that declining the "may"
 * leaves the Human untouched.
 */
class ExuberantWolfbearScenarioTest : ScenarioTestBase() {

    private fun wolfbearGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Exuberant Wolfbear", summoningSickness = false)
        .withCardOnBattlefield(1, "Elite Vanguard", summoningSickness = false)
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** The "you may" is the optional target: choosing the Human accepts, choosing no target declines. */
    private fun TestGame.attackAndTargetHuman(accept: Boolean) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Exuberant Wolfbear" to 2)).error shouldBe null
        val human = findPermanent("Elite Vanguard")!!
        withClue("the attack trigger asks for its optional target") { hasPendingDecision() shouldBe true }
        if (accept) selectTargets(listOf(human)).error shouldBe null else skipTargets().error shouldBe null
        resolveStack()
    }

    private fun TestGame.humanStats(): Pair<Int, Int> {
        val human = findPermanent("Elite Vanguard")!!
        return state.projectedState.getPower(human)!! to state.projectedState.getToughness(human)!!
    }

    init {
        context("Exuberant Wolfbear") {

            test("the Human's base becomes the Wolfbear's actual 5/5, then the anthem applies on top") {
                val game = wolfbearGame()
                game.attackAndTargetHuman(accept = true)

                withClue("Wolfbear is 5/5 under Glorious Anthem; Elite Vanguard's base becomes 5/5, +1/+1 = 6/6") {
                    game.humanStats() shouldBe (6 to 6)
                }
            }

            test("declining leaves the Human at its own stats") {
                val game = wolfbearGame()
                game.attackAndTargetHuman(accept = false)

                withClue("Elite Vanguard stays 2/1 + anthem = 3/2") {
                    game.humanStats() shouldBe (3 to 2)
                }
            }
        }
    }
}
