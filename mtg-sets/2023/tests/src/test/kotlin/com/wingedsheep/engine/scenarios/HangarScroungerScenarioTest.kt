package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hangar Scrounger (MOM #142) — {2}{R} 2/1. Backup 1; "Whenever this creature becomes tapped, you may
 * discard a card. If you do, draw a card."
 */
class HangarScroungerScenarioTest : ScenarioTestBase() {

    private fun attackWithRummage(answer: Boolean) {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hangar Scrounger")
            .withCardInHand(1, "Mountain")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Hangar Scrounger" to 2)).error shouldBe null
        game.resolveStack()
        withClue("tapping to attack triggers the may prompt") { game.hasPendingDecision() shouldBe true }
        game.answerYesNo(answer)
        if (answer && game.hasPendingDecision()) game.selectCards(listOf(game.findCardsInHand(1, "Mountain").first()))
        game.resolveStack()
        game.findCardsInHand(1, "Plains").size shouldBe (if (answer) 1 else 0)
        game.findCardsInHand(1, "Mountain").size shouldBe (if (answer) 0 else 1)
    }

    init {
        context("Hangar Scrounger") {
            test("becoming tapped lets you discard a card to draw a card") { attackWithRummage(true) }
            test("declining does nothing") { attackWithRummage(false) }

            test("backup on another creature grants it the tapped trigger and a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hangar Scrounger")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Hangar Scrounger").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()
        withClue("the granted trigger fires when the Bears tap") { game.hasPendingDecision() shouldBe true }
            }
        }
    }
}
