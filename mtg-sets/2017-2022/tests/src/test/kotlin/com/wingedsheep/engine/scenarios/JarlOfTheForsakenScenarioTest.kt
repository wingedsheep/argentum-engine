package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jarl of the Forsaken (KHM #100) — flash; when it enters, destroy target creature or planeswalker
 * an opponent controls that was dealt damage this turn. (Foretell is the shared ForetellEnumerator
 * path, pinned by Demon Bolt and the engine's foretell tests.)
 */
class JarlOfTheForsakenScenarioTest : ScenarioTestBase() {

    init {
        test("ETB destroys an opponent creature that was dealt damage this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Jarl of the Forsaken")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Shock", giant).error shouldBe null
            game.resolveStack()
            withClue("Hill Giant survives Shock") { game.isOnBattlefield("Hill Giant") shouldBe true }

            game.castSpell(1, "Jarl of the Forsaken").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Jarl of the Forsaken") shouldBe true
        }

        test("an undamaged opponent creature is not a legal target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Jarl of the Forsaken")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Jarl of the Forsaken").error shouldBe null
            game.resolveStack()

            withClue("no target prompt: the undamaged Giant is not offered") {
                game.hasPendingDecision() shouldBe false
            }
            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Jarl of the Forsaken") shouldBe true
        }
    }
}
