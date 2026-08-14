package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Kellan, the Fae-Blooded // Birthright Boon (WOE #230). */
class KellanTheFaeBloodedScenarioTest : ScenarioTestBase() {

    init {
        test("each Aura attached to Kellan gives other creatures +1/+0, but not Kellan") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Kellan, the Fae-Blooded", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Holy Strength")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kellan = game.findPermanent("Kellan, the Fae-Blooded")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Holy Strength", kellan).error shouldBe null
            game.resolveStack()

            withClue("the attached Aura increases only the other creature's power") {
                game.state.projectedState.getPower(bears) shouldBe 3
                // Holy Strength itself gives Kellan +1/+2; Kellan's anthem must not add another +1.
                game.state.projectedState.getPower(kellan) shouldBe 3
            }
        }

        test("Birthright Boon finds either an Aura or Equipment and resolves as an Adventure") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Kellan, the Fae-Blooded")
                .withCardInLibrary(1, "Holy Strength")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kellan = game.findCardsInHand(1, "Kellan, the Fae-Blooded").single()
            val aura = game.findCardsInLibrary(1, "Holy Strength").single()
            game.execute(CastSpell(game.player1Id, kellan, faceIndex = 0)).error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(aura)).error shouldBe null
            game.resolveStack()

            withClue("the selected Aura moved to hand") {
                game.findCardsInHand(1, "Holy Strength").size shouldBe 1
            }
            withClue("the resolved Adventure exiled Kellan for later creature casting") {
                game.isInExile(1, "Kellan, the Fae-Blooded") shouldBe true
            }
        }
    }
}
