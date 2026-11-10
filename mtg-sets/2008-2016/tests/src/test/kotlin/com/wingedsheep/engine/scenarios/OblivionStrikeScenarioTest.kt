package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Oblivion Strike — {3}{B} Sorcery.
 * "Devoid. Exile target creature."
 */
class OblivionStrikeScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Oblivion Strike")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardOnBattlefield(2, "Hill Giant")
            .withLandsOnBattlefield(2, "Forest", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("exiles the target creature") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.castSpell(1, "Oblivion Strike", giant)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            withClue("exiled, not destroyed") {
                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe false
            }
            game.isInGraveyard(1, "Oblivion Strike") shouldBe true
        }

        test("a noncreature permanent can't be targeted") {
            val game = board()
            val forest = game.findPermanent("Forest")!!

            game.castSpell(1, "Oblivion Strike", forest).error shouldNotBe null
            game.isOnBattlefield("Forest") shouldBe true
        }
    }
}
