package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ashnod's Intervention (BRO): "Until end of turn, target creature gets +2/+0 and gains 'When this
 * creature dies or is put into exile from the battlefield, return it to its owner's hand.'"
 *
 * Covers both legs of the granted rider: dying (graveyard -> hand) and being exiled from the
 * battlefield (exile -> hand).
 */
class AshnodsInterventionScenarioTest : ScenarioTestBase() {

    init {
        test("grants +2/+0 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ashnod's Intervention")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Ashnod's Intervention", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("when the creature dies it returns to its owner's hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ashnod's Intervention")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Ashnod's Intervention", bears).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears should be back in its owner's hand") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }

        test("when the creature is exiled from the battlefield it returns to its owner's hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ashnod's Intervention")
                .withCardInHand(1, "Swords to Plowshares")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Ashnod's Intervention", bears).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Swords to Plowshares", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears should be back in its owner's hand, not in exile") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(1, "Grizzly Bears") shouldBe false
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
