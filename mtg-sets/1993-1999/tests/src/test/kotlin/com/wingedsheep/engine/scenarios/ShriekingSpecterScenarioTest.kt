package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class ShriekingSpecterScenarioTest : ScenarioTestBase() {
    init {
        test("defender chooses a card to discard before blockers are declared") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Shrieking Specter", summoningSickness = false)
                .withCardOnBattlefield(2, "Air Elemental")
                .withCardInHand(1, "Swamp")
                .withCardInHand(2, "Forest")
                .withCardInHand(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shrieking Specter" to 2)).error shouldBe null
            game.resolveStack()

            game.state.step shouldBe Step.DECLARE_ATTACKERS
            val decision = game.getPendingDecision() as SelectCardsDecision
            decision.playerId shouldBe game.player2Id
            decision.minSelections shouldBe 1
            decision.maxSelections shouldBe 1
            val forest = game.findCardsInHand(2, "Forest").single()
            val island = game.findCardsInHand(2, "Island").single()
            decision.options shouldContainExactlyInAnyOrder listOf(forest, island)
            game.selectCards(listOf(island)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Island") shouldBe true
            game.handSize(2) shouldBe 1
            game.handSize(1) shouldBe 1
        }

        test("attack trigger still makes defender discard after the specter is destroyed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Shrieking Specter", summoningSickness = false)
                .withCardInHand(1, "Swamp")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Forest")
                .withCardInHand(2, "Island")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val specter = game.findPermanent("Shrieking Specter")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shrieking Specter" to 2)).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Lightning Bolt", specter).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Shrieking Specter") shouldBe true
            val decision = game.getPendingDecision() as SelectCardsDecision
            decision.playerId shouldBe game.player2Id
            game.selectCards(listOf(game.findCardsInHand(2, "Forest").single())).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Forest") shouldBe true
            game.handSize(2) shouldBe 1
            game.handSize(1) shouldBe 1
        }

        test("an empty defending hand requires no discard decision") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Shrieking Specter", summoningSickness = false)
                .withCardInHand(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shrieking Specter" to 2)).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.state.stack.size shouldBe 0
            game.handSize(2) shouldBe 0
            game.handSize(1) shouldBe 1
        }
    }
}
