package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Whispering Wizard (VOW #88) — {3}{U} Creature — Human Wizard, 3/2.
 *
 *   Whenever you cast a noncreature spell, create a 1/1 white Spirit creature token with
 *   flying. This ability triggers only once each turn.
 *
 * Exercises the cast-trigger token creation, the noncreature filter (a creature spell must not
 * trigger it), and the once-per-turn cap.
 */
class WhisperingWizardScenarioTest : ScenarioTestBase() {

    init {
        context("Whispering Wizard — Spirit token on noncreature cast") {

            test("casting a noncreature spell creates a 1/1 flying Spirit token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Whispering Wizard", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Spirit Token")
                withClue("exactly one Spirit token created") {
                    tokens.size shouldBe 1
                }
                val token = tokens.single()
                withClue("the token is a 1/1 flyer") {
                    game.state.projectedState.getPower(token) shouldBe 1
                    game.state.projectedState.getToughness(token) shouldBe 1
                    game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
                }
            }

            test("casting a creature spell does not create a token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Whispering Wizard", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("a creature spell must not create a Spirit token") {
                    game.findPermanents("Spirit Token").size shouldBe 0
                }
            }

            test("only creates one token per turn even after two noncreature spells") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Whispering Wizard", summoningSickness = false)
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("once-per-turn cap holds even for two noncreature spells") {
                    game.findPermanents("Spirit Token").size shouldBe 1
                }
            }
        }
    }
}
