package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fleshpulper Giant (M14 #140) — "When this creature enters, you may destroy target creature with
 * toughness 2 or less."
 */
class FleshpulperGiantScenarioTest : ScenarioTestBase() {

    init {
        context("Fleshpulper Giant") {
            test("ETB may destroy a creature with toughness 2 or less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fleshpulper Giant")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Fleshpulper Giant").error shouldBe null
                game.resolveStack()

                val td = game.getPendingDecision()
                td.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only toughness <= 2 creatures are legal: not the 3/3 Hill Giant") {
                    td.legalTargets[0]!!.shouldContainExactlyInAnyOrder(bears)
                }
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bears)))).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
