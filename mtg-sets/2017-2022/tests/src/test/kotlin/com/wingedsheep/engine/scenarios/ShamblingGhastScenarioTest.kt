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
 * Shambling Ghast (AFR #119): when it dies, choose one — target creature an opponent controls gets
 * -1/-1 until end of turn; or create a Treasure token.
 */
class ShamblingGhastScenarioTest : ScenarioTestBase() {

    private fun boltTheGhast(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Shambling Ghast")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Lightning Bolt", game.findPermanent("Shambling Ghast")!!).error shouldBe null
        game.resolveStack()
        game.isInGraveyard(1, "Shambling Ghast") shouldBe true
        return game
    }

    private fun TestGame.chooseMode(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        test("the -1/-1 mode shrinks the opponent's creature") {
            val game = boltTheGhast()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.chooseMode("-1/-1")
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 1
            game.state.projectedState.getToughness(bears) shouldBe 1
        }

        test("the Treasure mode creates a Treasure") {
            val game = boltTheGhast()
            game.chooseMode("Treasure")
            game.resolveStack()

            game.findPermanents("Treasure").size shouldBe 1
        }
    }
}
