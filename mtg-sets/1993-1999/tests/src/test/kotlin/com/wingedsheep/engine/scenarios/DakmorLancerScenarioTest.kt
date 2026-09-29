package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class DakmorLancerScenarioTest : ScenarioTestBase() {
    init {
        test("entering destroys a nonblack creature and excludes black creatures and lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dakmor Lancer")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Black Knight")
                .withCardOnBattlefield(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val knight = game.findPermanent("Black Knight")!!
            val forest = game.findPermanent("Forest")!!
            game.castSpell(1, "Dakmor Lancer").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.playerId shouldBe game.player1Id
            decision.legalTargets[0]!! shouldContain bears
            decision.legalTargets[0]!! shouldNotContain knight
            decision.legalTargets[0]!! shouldNotContain forest
            decision.legalTargets[0]!! shouldNotContain game.findPermanent("Dakmor Lancer")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Black Knight") shouldBe true
            game.isOnBattlefield("Dakmor Lancer") shouldBe true
        }

        test("must destroy a friendly nonblack creature when it is the only legal target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dakmor Lancer")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Dakmor Lancer").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Dakmor Lancer") shouldBe true
        }

        test("enters normally when no nonblack creature is available") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dakmor Lancer")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(2, "Black Knight")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Dakmor Lancer").error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Dakmor Lancer") shouldBe true
            game.isOnBattlefield("Black Knight") shouldBe true
        }
    }
}
