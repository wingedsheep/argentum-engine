package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Malcator, Purity Overseer (ONE #208) — "When Malcator enters, create a 3/3 colorless Phyrexian
 * Golem artifact creature token. At the beginning of your end step, if three or more artifacts
 * entered the battlefield under your control this turn, create a 3/3 colorless Phyrexian Golem
 * artifact creature token."
 *
 * Exercises `DynamicAmount.CardTypeEnteredUnderControlThisTurn(You, ARTIFACT)`: the ETB Golem token
 * itself is one of the three artifacts, and an artifact that has already left still counts.
 */
class MalcatorPurityOverseerScenarioTest : ScenarioTestBase() {

    private fun TestGame.golems(): Int = state.getBattlefield().count { id ->
        state.getEntity(id)?.get<CardComponent>()?.name?.contains("Golem") == true
    }

    private fun board(ornithopters: Int, withShatter: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Malcator, Purity Overseer")
        .apply { repeat(ornithopters) { withCardInHand(1, "Ornithopter") } }
        .apply { if (withShatter) withCardInHand(1, "Shatter") }
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castAndResolve(name: String, targetName: String? = null) {
        castSpell(1, name, targetName?.let { findPermanent(it)!! }).error shouldBe null
        resolveStack()
    }

    init {
        context("Malcator, Purity Overseer") {

            test("ETB creates a Golem; three artifacts entered → another Golem at your end step") {
                val game = board(ornithopters = 2)
                game.castAndResolve("Malcator, Purity Overseer")
                withClue("ETB Golem") { game.golems() shouldBe 1 }

                game.castAndResolve("Ornithopter")
                game.castAndResolve("Ornithopter")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("Golem token + two Ornithopters = three artifacts entered") { game.golems() shouldBe 2 }
            }

            test("only two artifacts entered → no end-step Golem") {
                val game = board(ornithopters = 1)
                game.castAndResolve("Malcator, Purity Overseer")
                game.castAndResolve("Ornithopter")

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.golems() shouldBe 1
            }

            test("an artifact that entered and then left the battlefield still counts") {
                val game = board(ornithopters = 2, withShatter = true)
                game.castAndResolve("Malcator, Purity Overseer")
                game.castAndResolve("Ornithopter")
                game.castAndResolve("Ornithopter")
                game.castAndResolve("Shatter", targetName = "Ornithopter")
                withClue("one Ornithopter destroyed") { game.findPermanents("Ornithopter").size shouldBe 1 }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.golems() shouldBe 2
            }

            test("the count is per turn — last turn's artifacts don't carry into the opponent's end step") {
                val game = board(ornithopters = 2)
                game.castAndResolve("Malcator, Purity Overseer")
                game.castAndResolve("Ornithopter")
                game.castAndResolve("Ornithopter")
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.golems() shouldBe 2

                // Opponent's turn: Malcator only watches *your* end step, and the log was cleared.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.golems() shouldBe 2
            }
        }
    }
}
