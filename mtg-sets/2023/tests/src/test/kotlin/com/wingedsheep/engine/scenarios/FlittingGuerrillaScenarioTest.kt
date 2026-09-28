package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Flitting Guerrilla: dies -> each player mills two; may exile it, then a graveyard card goes on top. */
class FlittingGuerrillaScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Flitting Guerrilla")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .apply { repeat(4) { withCardInLibrary(1, "Island") } }
        .apply { repeat(4) { withCardInLibrary(2, "Swamp") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("dies, mills both, exiles itself and puts a creature card on top") {
            val game = setup()
            val guerrilla = game.findPermanent("Flitting Guerrilla")!!
            game.castSpell(1, "Lightning Bolt", guerrilla).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("both players milled two") { game.librarySize(2) shouldBe 2 }
            withClue("Guerrilla exiled") { game.isInExile(1, "Flitting Guerrilla") shouldBe true }
            withClue("Bears on top: 4 - 2 milled + 1") { game.librarySize(1) shouldBe 3 }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("declining leaves Guerrilla in the graveyard") {
            val game = setup()
            val guerrilla = game.findPermanent("Flitting Guerrilla")!!
            game.castSpell(1, "Lightning Bolt", guerrilla).error shouldBe null
            game.resolveStack()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Flitting Guerrilla") shouldBe true
            game.librarySize(1) shouldBe 2
        }
    }
}
