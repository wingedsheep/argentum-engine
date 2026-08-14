package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario coverage for Thoughtcast (MRD #54).
 *
 * {4}{U} Sorcery
 * "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 *  Draw two cards."
 *
 * Affinity shaves generic mana only, so the interesting claims are about the floor: four artifacts
 * take it to a bare {U}, and a fifth can't push it below that.
 */
class ThoughtcastScenarioTest : ScenarioTestBase() {

    private val costCalculator by lazy { CostCalculator(cardRegistry) }

    init {
        fun boardWithArtifacts(count: Int) = run {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Thoughtcast")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
            repeat(count) { builder = builder.withCardOnBattlefield(1, "Bonesplitter") }
            builder.inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        }

        fun TestGame.genericCost(): Int = costCalculator.calculateEffectiveCost(
            state,
            cardRegistry.requireCard("Thoughtcast"),
            player1Id
        ).genericAmount

        context("Thoughtcast") {

            test("each artifact you control shaves {1} off the generic cost") {
                withClue("no artifacts — full {4}{U}") {
                    boardWithArtifacts(0).genericCost() shouldBe 4
                }
                withClue("three artifacts — {1}{U}") {
                    boardWithArtifacts(3).genericCost() shouldBe 1
                }
            }

            test("the {U} is never reduced away — extra artifacts stop mattering at four") {
                withClue("four artifacts take the generic to zero") {
                    boardWithArtifacts(4).genericCost() shouldBe 0
                }
                withClue("a sixth artifact can't push the generic below zero") {
                    boardWithArtifacts(6).genericCost() shouldBe 0
                }
            }

            test("cast off a single Island with four artifacts out, and draw two") {
                val game = boardWithArtifacts(4)
                val handBefore = game.handSize(1)

                game.castSpell(1, "Thoughtcast").error shouldBe null
                game.resolveStack()

                withClue("Thoughtcast left hand, two cards came in: net +1") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                game.findCardsInGraveyard(1, "Thoughtcast").size shouldBe 1
            }

            test("one Island is not enough without artifacts") {
                val game = boardWithArtifacts(0)

                withClue("{4}{U} off a single Island is unpayable") {
                    game.castSpell(1, "Thoughtcast").error shouldNotBe null
                }
            }
        }
    }
}
