package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Iname, Life Aspect (CHK #215).
 *
 *   When Iname dies, you may exile it. If you do, return any number of target Spirit cards from
 *   your graveyard to your hand.
 *
 * "Spirit cards" is a bare tribal noun — any card with the subtype, but nothing else. The return
 * only happens if Iname was actually exiled; declining leaves everything in the graveyard.
 */
class InameLifeAspectScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Iname, Life Aspect")
        .withCardInGraveyard(1, "Gibbering Kami")
        .withCardInGraveyard(1, "Kami of the Hunt")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInHand(2, "Terror")
        .withLandsOnBattlefield(2, "Swamp", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Kill Iname, accept the "you may", choose [targets] from P1's graveyard and resolve. */
    private fun ScenarioTestBase.TestGame.killInameAndExile(targets: (ScenarioTestBase.TestGame) -> List<EntityId>) {
        val iname = findPermanent("Iname, Life Aspect")!!
        castSpell(2, "Terror", iname).outcome shouldBe Outcome.Done
        resolveStack()

        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(true)
        val choose = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("Iname itself is a legal (if pointless) target; Grizzly Bears is not") {
            choose.legalTargets[0]!!.toSet() shouldBe (
                findCardsInGraveyard(1, "Gibbering Kami") +
                    findCardsInGraveyard(1, "Kami of the Hunt") +
                    findCardsInGraveyard(1, "Iname, Life Aspect")
                ).toSet()
        }
        selectTargets(targets(this))
        resolveStack()
    }

    init {
        context("Iname, Life Aspect") {

            test("exiling Iname returns the chosen Spirit cards to hand") {
                val game = board()
                game.killInameAndExile {
                    it.findCardsInGraveyard(1, "Gibbering Kami") + it.findCardsInGraveyard(1, "Kami of the Hunt")
                }

                game.isInExile(1, "Iname, Life Aspect") shouldBe true
                game.isInHand(1, "Gibbering Kami") shouldBe true
                game.isInHand(1, "Kami of the Hunt") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("choosing only some Spirits returns only those") {
                val game = board()
                game.killInameAndExile { it.findCardsInGraveyard(1, "Gibbering Kami") }

                game.isInExile(1, "Iname, Life Aspect") shouldBe true
                game.isInHand(1, "Gibbering Kami") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            }

            test("Iname can be exiled with no targets chosen") {
                val game = board()
                game.killInameAndExile { emptyList() }

                game.isInExile(1, "Iname, Life Aspect") shouldBe true
                game.isInGraveyard(1, "Gibbering Kami") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            }

            test("declining the exile returns nothing and leaves Iname in the graveyard") {
                val game = board()
                val iname = game.findPermanent("Iname, Life Aspect")!!
                game.castSpell(2, "Terror", iname).outcome shouldBe Outcome.Done
                game.resolveStack()

                game.answerYesNo(false)
                if (game.hasPendingDecision()) game.skipTargets()
                game.resolveStack()

                game.isInGraveyard(1, "Iname, Life Aspect") shouldBe true
                game.isInGraveyard(1, "Gibbering Kami") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            }
        }
    }
}
