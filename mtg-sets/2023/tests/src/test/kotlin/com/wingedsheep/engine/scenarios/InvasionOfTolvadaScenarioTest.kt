package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of Tolvada // The Broken Sky.
 *
 * Front: entering returns target nonbattle permanent card from your graveyard to the battlefield.
 * Back: creature tokens you control get +1/+0 and have lifelink; a 1/1 white and black flying
 * Spirit token at your end step.
 */
class InvasionOfTolvadaScenarioTest : ScenarioTestBase() {

    init {
        test("front: returns a nonbattle permanent card; a battle card isn't a legal target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Tolvada")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Invasion of Dominaria")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Tolvada").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val legal = decision.legalTargets.values.flatten()
            legal shouldContain bears
            legal shouldNotContain game.findCardsInGraveyard(1, "Invasion of Dominaria").single()
            legal shouldNotContain game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("back: creature tokens get +1/+0 and lifelink; a flying Spirit at your end step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Tolvada")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Tolvada")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.findPermanent("The Broken Sky") shouldNotBe null
            val projected = game.state.projectedState
            val tokenBears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            projected.getPower(tokenBears) shouldBe 3
            projected.getToughness(tokenBears) shouldBe 2
            projected.hasKeyword(tokenBears, Keyword.LIFELINK) shouldBe true
            withClue("a nontoken creature is unaffected") {
                projected.getPower(giant) shouldBe 3
                projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val spirit = game.findPermanent("Spirit Token") ?: game.findPermanent("Spirit")
            spirit shouldNotBe null
            val after = game.state.projectedState
            after.getPower(spirit!!) shouldBe 2
            after.getToughness(spirit) shouldBe 1
            after.hasKeyword(spirit, Keyword.FLYING) shouldBe true
            after.hasKeyword(spirit, Keyword.LIFELINK) shouldBe true
        }
    }
}
