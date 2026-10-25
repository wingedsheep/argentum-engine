package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Orcus, Prince of Undeath (AFR #229): when it enters, choose one — each other creature gets
 * -X/-X and you lose X life, or return up to X target creature cards with total mana value X or
 * less from your graveyard, with haste. X is the X paid for Orcus; the second mode's "up to X"
 * target count has to reach the modal trigger's target decision.
 */
class OrcusPrinceOfUndeathScenarioTest : ScenarioTestBase() {

    private fun castOrcus(x: Int): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Orcus, Prince of Undeath")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(1, "Savannah Lions")
            .withCardInGraveyard(1, "Hill Giant")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Craw Wurm")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castXSpell(1, "Orcus, Prince of Undeath", x).error shouldBe null
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
        test("the -X/-X mode shrinks each other creature and costs X life") {
            val game = castOrcus(x = 3)
            game.chooseMode("Each other creature")
            game.resolveStack()

            val orcus = game.findPermanent("Orcus, Prince of Undeath")!!
            game.state.projectedState.getPower(orcus) shouldBe 5
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            val wurm = game.findPermanent("Craw Wurm")!!
            game.state.projectedState.getPower(wurm) shouldBe 3
            game.state.projectedState.getToughness(wurm) shouldBe 1
            game.getLifeTotal(1) shouldBe 17
        }

        test("the return mode offers up to X targets and returns them with haste") {
            val game = castOrcus(x = 3)
            game.chooseMode("Return up to X")
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.targetRequirements.single().maxTargets shouldBe 3
            decision.targetRequirements.single().minTargets shouldBe 0

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
            game.selectTargets(listOf(bears, lions)).error shouldBe null
            game.resolveStack()

            val returnedBears = game.findPermanent("Grizzly Bears")
            val returnedLions = game.findPermanent("Savannah Lions")
            returnedBears shouldNotBe null
            returnedLions shouldNotBe null
            game.state.projectedState.hasKeyword(returnedBears!!, Keyword.HASTE) shouldBe true
            game.state.projectedState.hasKeyword(returnedLions!!, Keyword.HASTE) shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
        }

        test("the return mode rejects targets whose total mana value exceeds X") {
            val game = castOrcus(x = 3)
            game.chooseMode("Return up to X")
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.selectTargets(listOf(bears, giant)).error shouldNotBe null
        }

        test("with X = 1 the return mode caps at one target") {
            val game = castOrcus(x = 1)
            game.chooseMode("Return up to X")
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.targetRequirements.single().maxTargets shouldBe 1

            val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
            game.selectTargets(listOf(lions)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Savannah Lions") shouldNotBe null
        }
    }
}
