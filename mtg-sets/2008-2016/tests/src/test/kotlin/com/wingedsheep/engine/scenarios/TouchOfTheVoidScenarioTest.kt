package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Touch of the Void (BFZ #134) — {2}{R} Sorcery, devoid.
 *
 *   Touch of the Void deals 3 damage to any target. If a creature dealt damage this way would die
 *   this turn, exile it instead.
 *
 * The exile-instead mark is gated on the target being a creature: a creature it kills is exiled,
 * a creature it only wounds is exiled when it dies later that turn, but a planeswalker it kills
 * still goes to the graveyard, and a player simply takes 3.
 */
class TouchOfTheVoidScenarioTest : ScenarioTestBase() {

    init {
        context("Touch of the Void") {

            test("a creature it kills is exiled instead of going to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch of the Void")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Touch of the Void", bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears died to Touch of the Void and were exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
            }

            test("a creature it only wounds is exiled when it dies later this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch of the Void")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Siege Mastodon")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mastodon = game.findPermanent("Siege Mastodon")!!
                game.castSpell(1, "Touch of the Void", mastodon).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Siege Mastodon") shouldBe true

                game.castSpell(1, "Lightning Bolt", mastodon).error shouldBe null
                game.resolveStack()

                withClue("the 3/5 Mastodon was dealt damage by Touch of the Void, so dying this turn exiles it") {
                    game.isOnBattlefield("Siege Mastodon") shouldBe false
                    game.isInGraveyard(2, "Siege Mastodon") shouldBe false
                    game.isInExile(2, "Siege Mastodon") shouldBe true
                }
            }

            test("a planeswalker it kills goes to the graveyard, not exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch of the Void")
                    .withCardOnBattlefield(2, "Jace, the Mind Sculptor")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, the Mind Sculptor")!!
                game.castSpell(1, "Touch of the Void", jace).error shouldBe null
                game.resolveStack()

                withClue("Jace (3 loyalty) dies, but only a creature dealt damage this way is exiled") {
                    game.isOnBattlefield("Jace, the Mind Sculptor") shouldBe false
                    game.isInExile(2, "Jace, the Mind Sculptor") shouldBe false
                    game.isInGraveyard(2, "Jace, the Mind Sculptor") shouldBe true
                }
            }

            test("a player target takes 3 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Touch of the Void")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val touch = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Touch of the Void"
                }
                val cast = game.execute(
                    CastSpell(game.player1Id, touch, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
