package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vraska's Finisher (WAR #112) — {2}{B} Creature — Gorgon Assassin 3/2.
 *
 *   When this creature enters, destroy target creature or planeswalker an opponent controls that
 *   was dealt damage this turn.
 */
class VraskasFinisherScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Vraska's Finisher")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardOnBattlefield(2, "Glory Seeker")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroys an opponent's creature that was dealt damage this turn") {
            val game = board()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Shock", wurm).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Craw Wurm") shouldBe true

            game.castSpell(1, "Vraska's Finisher").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(wurm)
            game.selectTargets(listOf(wurm)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Glory Seeker") shouldBe true
        }

        test("with no damaged opponent's creature the trigger has nothing to destroy") {
            val game = board()
            game.castSpell(1, "Vraska's Finisher").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Glory Seeker") shouldBe true
            game.isOnBattlefield("Vraska's Finisher") shouldBe true
        }
    }
}
