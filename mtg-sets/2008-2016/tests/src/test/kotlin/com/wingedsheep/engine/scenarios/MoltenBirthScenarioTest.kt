package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Molten Birth (M14 #147): two 1/1 Elementals, then a coin flip whose win returns the resolving
 * spell from the stack straight to its owner's hand (never the graveyard); a loss lets it go to the
 * graveyard as usual. The flip is random, so each branch retries until it is observed.
 */
class MoltenBirthScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInHand(1, "Molten Birth")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")

        fun observe(wantWin: Boolean) {
            var seen = false
            repeat(60) {
                if (seen) return@repeat
                val game = base().build()
                val permanentsBefore = game.state.getBattlefield().size
                game.castSpell(1, "Molten Birth").error shouldBe null
                val coin = game.resolveStack().flatMap { it.events }
                    .filterIsInstance<CoinFlipEvent>().firstOrNull() ?: return@repeat
                if (coin.won != wantWin) return@repeat
                seen = true

                withClue("Two Elemental tokens are created either way") {
                    game.state.getBattlefield().size shouldBe permanentsBefore + 2
                }
                withClue("Won flip returns Molten Birth to hand; a lost flip leaves it in the graveyard") {
                    game.isInHand(1, "Molten Birth") shouldBe wantWin
                    game.isInGraveyard(1, "Molten Birth") shouldBe !wantWin
                }
            }
            withClue("Expected to observe a ${if (wantWin) "won" else "lost"} flip within 60 attempts") {
                seen shouldBe true
            }
        }

        test("win the flip: two Elementals and Molten Birth returns to its owner's hand") { observe(true) }

        test("lose the flip: two Elementals and Molten Birth goes to the graveyard") { observe(false) }
    }
}
