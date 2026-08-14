package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for Broodstar (MRD #31).
 *
 * {8}{U}{U} Creature — Beast star/star
 * "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 *  Flying
 *  Broodstar's power and toughness are each equal to the number of artifacts you control."
 *
 * Both halves read the same number, which is the trap worth pinning: the characteristic-defining
 * ability is re-read continuously (not snapshotted on entry), Broodstar is not itself an artifact
 * so it never counts toward its own size, and only artifacts *you* control count for either half.
 */
class BroodstarScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()
    private val costCalculator by lazy { CostCalculator(cardRegistry) }

    init {
        fun board(yourArtifacts: Int, opponentArtifacts: Int = 0) = run {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Broodstar")
            repeat(yourArtifacts) { builder = builder.withCardOnBattlefield(1, "Bonesplitter") }
            repeat(opponentArtifacts) { builder = builder.withCardOnBattlefield(2, "Bonesplitter") }
            builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
        }

        context("Broodstar") {

            test("power and toughness both track the artifacts you control") {
                val game = board(yourArtifacts = 3)
                val broodstar = game.findPermanent("Broodstar")!!

                val projected = projector.project(game.state)
                withClue("three artifacts, and Broodstar itself is not one of them") {
                    projected.getPower(broodstar) shouldBe 3
                    projected.getToughness(broodstar) shouldBe 3
                }
                projected.hasKeyword(broodstar, Keyword.FLYING) shouldBe true
            }

            test("it shrinks when an artifact leaves — the ability is re-read, not snapshotted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Broodstar")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withCardOnBattlefield(1, "Serum Tank")
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val broodstar = game.findPermanent("Broodstar")!!
                val doomed = game.findPermanent("Serum Tank")!!

                projector.project(game.state).getPower(broodstar) shouldBe 3

                game.castSpell(1, "Shatter", targetId = doomed).error shouldBe null
                game.resolveStack()

                withClue("two artifacts left, so Broodstar is a 2/2") {
                    game.findPermanent("Serum Tank") shouldBe null
                    val after = projector.project(game.state)
                    after.getPower(broodstar) shouldBe 2
                    after.getToughness(broodstar) shouldBe 2
                }
            }

            test("an opponent's artifacts count for neither the size nor the discount") {
                val game = board(yourArtifacts = 1, opponentArtifacts = 4)
                val broodstar = game.findPermanent("Broodstar")!!

                withClue("only your one artifact sizes it") {
                    projector.project(game.state).getPower(broodstar) shouldBe 1
                }
                withClue("affinity counts artifacts you control, so {8} drops to {7}") {
                    costCalculator.calculateEffectiveCost(
                        game.state,
                        cardRegistry.requireCard("Broodstar"),
                        game.player1Id
                    ).genericAmount shouldBe 7
                }
            }

            test("with no artifacts it is a 0/0 and dies to state-based actions") {
                val game = board(yourArtifacts = 0)
                val broodstar = game.findPermanent("Broodstar")!!

                projector.project(game.state).getToughness(broodstar) shouldBe 0

                game.checkStateBasedActions()

                withClue("0 toughness is lethal (CR 704.5f)") {
                    game.findPermanent("Broodstar") shouldBe null
                    game.findCardsInGraveyard(1, "Broodstar").size shouldBe 1
                }
            }
        }
    }
}
