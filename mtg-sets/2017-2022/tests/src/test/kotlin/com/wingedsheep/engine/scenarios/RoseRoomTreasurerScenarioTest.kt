package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rose Room Treasurer (NCC #51).
 *
 * {3}{R} Creature — Ogre Warrior 4/3
 * "Alliance — Whenever another creature you control enters, create a Treasure token if this is the
 *  first or second time this ability has resolved this turn. Otherwise, you may pay {X}. When you
 *  do, this creature deals X damage to any target."
 *
 * Covers the per-turn resolution count branching (Treasure on the first two, pay-{X} from the third)
 * and the reflexive "any target" chosen after X is paid.
 */
class RoseRoomTreasurerScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Beast",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype("Beast")),
                power = 1,
                toughness = 1
            )
        )

        fun boardWithThreeBeasts() = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Rose Room Treasurer")
            .withCardInHand(1, "Test Beast")
            .withCardInHand(1, "Test Beast")
            .withCardInHand(1, "Test Beast")
            .withLandsOnBattlefield(1, "Mountain", 8)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Rose Room Treasurer") {

            test("first and second resolutions make Treasures; the third pays X to deal X damage") {
                val game = boardWithThreeBeasts()

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                game.findAllPermanents("Treasure").size shouldBe 1

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                game.findAllPermanents("Treasure").size shouldBe 2
                game.hasPendingDecision() shouldBe false

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() is ChooseNumberDecision) shouldBe true
                game.chooseNumber(3).error shouldBe null

                // Reflexive trigger: the target is chosen after X is paid.
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.findAllPermanents("Treasure").size shouldBe 2
            }

            test("declining the third-resolution payment does nothing") {
                val game = boardWithThreeBeasts()

                repeat(2) {
                    game.castSpell(1, "Test Beast").error shouldBe null
                    game.resolveStack()
                }
                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() is ChooseNumberDecision) shouldBe true
                game.chooseNumber(0).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 20
                game.findAllPermanents("Treasure").size shouldBe 2
            }
        }
    }
}
