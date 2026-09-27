package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Iname, Death Aspect — on entry, an optional unbounded search that puts Spirit cards from the
 * library into the graveyard.
 */
class InameDeathAspectScenarioTest : ScenarioTestBase() {
    init {
        test("the entry trigger puts any number of Spirit cards from library into graveyard") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Iname, Death Aspect")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Iname, Life Aspect")
                .withCardInLibrary(1, "Kami of the Hunt")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Iname, Death Aspect").error shouldBe null
            game.resolveStack()

            game.answerYesNo(true).error shouldBe null
            val decision = game.state.pendingDecision as SelectCardsDecision
            // Hill Giant is not a Spirit, so it is not offered.
            decision.options.size shouldBe 2
            game.selectCards(decision.options).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Iname, Life Aspect") shouldBe true
            game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
            game.isOnBattlefield("Iname, Death Aspect") shouldBe true
        }

        test("choosing only some Spirits leaves the rest in the library") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Iname, Death Aspect")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Iname, Life Aspect")
                .withCardInLibrary(1, "Kami of the Hunt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Iname, Death Aspect").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            val kami = game.findCardsInLibrary(1, "Kami of the Hunt").single()
            game.selectCards(listOf(kami)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            game.findCardsInLibrary(1, "Iname, Life Aspect").size shouldBe 1
        }

        test("declining the may leaves the library untouched") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Iname, Death Aspect")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Kami of the Hunt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Iname, Death Aspect").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findCardsInLibrary(1, "Kami of the Hunt").size shouldBe 1
            game.graveyardSize(1) shouldBe 0
        }
    }
}
