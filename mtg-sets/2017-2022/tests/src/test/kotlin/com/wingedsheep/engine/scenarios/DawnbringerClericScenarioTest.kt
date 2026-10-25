package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dawnbringer Cleric (AFR #9): when it enters, choose one — Cure Wounds (gain 2 life),
 * Dispel Magic (destroy target enchantment), or Gentle Repose (exile target card from a graveyard).
 */
class DawnbringerClericScenarioTest : ScenarioTestBase() {

    private fun castCleric(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Dawnbringer Cleric")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(2, "Glorious Anthem")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Dawnbringer Cleric").error shouldBe null
        game.resolveStack()
        return game
    }

    private fun TestGame.chooseMode(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        test("Cure Wounds gains 2 life") {
            val game = castCleric()
            game.chooseMode("Cure Wounds")
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
        }

        test("Dispel Magic destroys the opponent's enchantment") {
            val game = castCleric()
            val anthem = game.findPermanent("Glorious Anthem")!!
            game.chooseMode("Dispel Magic")
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(anthem)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Glorious Anthem") shouldBe true
        }

        test("Gentle Repose exiles a card from an opponent's graveyard") {
            val game = castCleric()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.chooseMode("Gentle Repose")
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
        }
    }
}
