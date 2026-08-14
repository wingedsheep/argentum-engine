package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Unassuming Sage. */
class UnassumingSageScenarioTest : ScenarioTestBase() {

    private val outriderAbilityId by lazy {
        cardRegistry.requireCard("Verdant Outrider").activatedAbilities[0].id
    }

    init {
        context("Unassuming Sage — optional {2} for a self-attached Sorcerer Role") {
            test("paying {2} crowns the Sage itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Unassuming Sage")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unassuming Sage")
                game.resolveStack()

                // The ETB trigger offers the {2}: say yes, then auto-tap for it.
                game.answerYesNo(true)
                if (game.getPendingDecision() != null) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val sage = game.findPermanent("Unassuming Sage")!!
                val role = game.findPermanent("Sorcerer Role")
                withClue("the Sorcerer Role token was created") { role shouldNotBe null }
                withClue("\"attached to it\" means the Sage itself, not a target") {
                    game.state.getEntity(role!!)?.get<AttachedToComponent>()?.targetId shouldBe sage
                }
                withClue("2/2 base + the Sorcerer Role's +1/+1 = 3/3") {
                    game.state.projectedState.getPower(sage) shouldBe 3
                    game.state.projectedState.getToughness(sage) shouldBe 3
                }
            }

            test("declining the {2} leaves the Sage a plain 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Unassuming Sage")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unassuming Sage")
                game.resolveStack()

                game.answerYesNo(false)
                game.resolveStack()

                val sage = game.findPermanent("Unassuming Sage")!!
                withClue("declining skips the Role entirely") {
                    game.findPermanent("Sorcerer Role") shouldBe null
                    game.state.projectedState.getPower(sage) shouldBe 2
                    game.state.projectedState.getToughness(sage) shouldBe 2
                }
            }
        }
    }
}
