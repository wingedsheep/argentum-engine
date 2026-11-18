package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Warden of the Beyond (M15 #42).
 *
 * "Vigilance. This creature gets +2/+2 as long as an opponent owns a card in exile."
 *
 * The condition reads *ownership* of exiled cards, not who exiled them: the last case has the
 * opponent exile the Warden controller's own creature, which must not turn the bonus on.
 */
class WardenOfTheBeyondScenarioTest : ScenarioTestBase() {

    init {
        context("Warden of the Beyond — +2/+2 while an opponent owns a card in exile") {
            test("nothing in exile: a 2/2 with vigilance") {
                val game = scenario()
                    .withPlayers("Warden", "Opponent")
                    .withCardOnBattlefield(1, "Warden of the Beyond")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warden = game.findPermanent("Warden of the Beyond").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.hasKeyword(warden, Keyword.VIGILANCE) shouldBe true
                projected.getPower(warden) shouldBe 2
                projected.getToughness(warden) shouldBe 2
            }

            test("exiling an opponent's creature makes it a 4/4") {
                val game = scenario()
                    .withPlayers("Warden", "Opponent")
                    .withCardOnBattlefield(1, "Warden of the Beyond")
                    .withCardInHand(1, "Swords to Plowshares")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warden = game.findPermanent("Warden of the Beyond").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                val cast = game.castSpell(1, "Swords to Plowshares", targetId = bears)
                withClue("Swords to Plowshares should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the opponent now owns a card in exile") {
                    game.state.projectedState.getPower(warden) shouldBe 4
                    game.state.projectedState.getToughness(warden) shouldBe 4
                }
            }

            test("the bonus does not stack with more exiled cards") {
                val game = scenario()
                    .withPlayers("Warden", "Opponent")
                    .withCardOnBattlefield(1, "Warden of the Beyond")
                    .withCardInExile(2, "Grizzly Bears")
                    .withCardInExile(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warden = game.findPermanent("Warden of the Beyond").shouldNotBeNull()
                game.state.projectedState.getPower(warden) shouldBe 4
                game.state.projectedState.getToughness(warden) shouldBe 4
            }

            test("a card you own in exile does not count, even when the opponent exiled it") {
                val game = scenario()
                    .withPlayers("Warden", "Opponent")
                    .withCardOnBattlefield(1, "Warden of the Beyond")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Swords to Plowshares")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warden = game.findPermanent("Warden of the Beyond").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                val cast = game.castSpell(2, "Swords to Plowshares", targetId = bears)
                withClue("Swords to Plowshares should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears is in exile but its owner is the Warden's controller") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.state.projectedState.getPower(warden) shouldBe 2
                    game.state.projectedState.getToughness(warden) shouldBe 2
                }
            }
        }
    }
}
