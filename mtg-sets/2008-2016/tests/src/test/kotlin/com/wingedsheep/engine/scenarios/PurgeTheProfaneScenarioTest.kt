package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Purge the Profane (GTC) — target opponent discards two cards and you gain 2 life. */
class PurgeTheProfaneScenarioTest : ScenarioTestBase() {
    init {
        test("opponent discards two and the caster gains 2 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Purge the Profane")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Purge the Profane", 2).error shouldBe null
            game.resolveStack()
            var guard = 0
            while (game.hasPendingDecision() && guard++ < 5) {
                val d = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(d.options.take(d.minSelections))
            }

            withClue("two cards discarded") {
                game.handSize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 2
            }
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
