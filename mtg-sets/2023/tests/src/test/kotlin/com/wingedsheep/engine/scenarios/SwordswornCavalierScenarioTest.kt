package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Swordsworn Cavalier — first strike as long as *another* Knight entered under your control this
 * turn. The Cavalier's own entry is a Knight entry too, and must not satisfy "another".
 */
class SwordswornCavalierScenarioTest : ScenarioTestBase() {

    private fun TestGame.hasFirstStrike(): Boolean =
        state.projectedState.hasKeyword(findPermanent("Swordsworn Cavalier")!!, Keyword.FIRST_STRIKE)

    init {
        test("no Knight entered this turn — no first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Swordsworn Cavalier")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.hasFirstStrike() shouldBe false
        }

        test("another Knight entering gives it first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Swordsworn Cavalier")
                .withCardInHand(1, "Trokin High Guard")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Trokin High Guard").error shouldBe null
            game.resolveStack()
            game.hasFirstStrike() shouldBe true
        }

        test("a non-Knight entering does not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Swordsworn Cavalier")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.hasFirstStrike() shouldBe false
        }

        test("its own entry is not another Knight, but a second Knight after it is") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Swordsworn Cavalier")
                .withCardInHand(1, "Trokin High Guard")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Swordsworn Cavalier").error shouldBe null
            game.resolveStack()
            game.hasFirstStrike() shouldBe false

            game.castSpell(1, "Trokin High Guard").error shouldBe null
            game.resolveStack()
            game.hasFirstStrike() shouldBe true
        }

        test("an opponent's Knight entering does not count") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Swordsworn Cavalier")
                .withCardInHand(2, "Trokin High Guard")
                .withLandsOnBattlefield(2, "Plains", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Trokin High Guard").error shouldBe null
            game.resolveStack()
            game.hasFirstStrike() shouldBe false
        }
    }
}
