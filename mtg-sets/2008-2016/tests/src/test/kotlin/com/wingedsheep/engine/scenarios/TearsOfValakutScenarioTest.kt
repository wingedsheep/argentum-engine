package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tears of Valakut — {1}{R} Instant.
 * "This spell can't be countered. Tears of Valakut deals 5 damage to target creature with flying."
 */
class TearsOfValakutScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Tears of Valakut")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Serra Angel")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(2, "Counterspell")
            .withLandsOnBattlefield(2, "Island", 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("deals 5 damage to a creature with flying") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!

            val result = game.castSpell(1, "Tears of Valakut", angel)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Serra Angel") shouldBe false
            game.isInGraveyard(2, "Serra Angel") shouldBe true
        }

        test("a creature without flying can't be targeted") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Tears of Valakut", giant).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("Counterspell doesn't stop it") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!

            game.castSpell(1, "Tears of Valakut", angel).error shouldBe null
            game.passPriority().error shouldBe null
            // Whether the engine refuses the counter outright or lets it fizzle, Tears still resolves.
            game.castSpellTargetingStackSpell(2, "Counterspell", "Tears of Valakut")
            game.resolveStack()

            withClue("the uncounterable spell still kills the Angel") {
                game.isOnBattlefield("Serra Angel") shouldBe false
            }
            game.isInGraveyard(1, "Tears of Valakut") shouldBe true
        }
    }
}
