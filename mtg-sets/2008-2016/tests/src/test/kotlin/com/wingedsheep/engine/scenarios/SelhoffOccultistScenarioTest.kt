package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Selhoff Occultist (ISD #73) — {2}{U} 2/3 Human Rogue.
 *
 *   Whenever this creature or another creature dies, target player mills a card.
 */
class SelhoffOccultistScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Selhoff Occultist")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .also { b -> repeat(3) { b.withCardInLibrary(2, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("another creature dying makes the target player mill a card") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.isInGraveyard(2, "Island") shouldBe true
        }

        test("its own death triggers it too") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Selhoff Occultist")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Selhoff Occultist") shouldBe true

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 2
        }
    }
}
