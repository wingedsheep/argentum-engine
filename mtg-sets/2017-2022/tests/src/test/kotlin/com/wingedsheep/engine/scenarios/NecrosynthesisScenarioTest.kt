package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Necrosynthesis (MID #115) — {1}{B} Enchantment — Aura.
 *
 * "Enchanted creature has 'Whenever another creature dies, put a +1/+1 counter on this creature.'
 *  When enchanted creature dies, look at the top X cards of your library, where X is its power.
 *  Put one of those cards into your hand and the rest on the bottom of your library in a random order."
 *
 * Proves the granted trigger puts its counter on the *host*, and that X is the host's last-known
 * power — counters included (2021-09-24 ruling).
 */
class NecrosynthesisScenarioTest : ScenarioTestBase() {

    init {
        context("Necrosynthesis") {

            test("host grows when another creature dies, then digs X = its last-known power on death") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Necrosynthesis", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Doom Blade")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                withClue("the opposing Hill Giant died, so the enchanted Bears got a +1/+1 counter") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.state.projectedState.getPower(bears) shouldBe 3
                }

                game.librarySize(1) shouldBe 4
                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("X is the Bears' last-known power of 3, counter included") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.size shouldBe 3
                }

                game.selectCards(listOf((decision as SelectCardsDecision).options.first()))
                game.resolveStack()

                withClue("one card to hand, the other two bottomed") {
                    game.handSize(1) shouldBe 1
                    game.librarySize(1) shouldBe 3
                }
                game.isInGraveyard(1, "Necrosynthesis") shouldBe true
            }
        }
    }
}
