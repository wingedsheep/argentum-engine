package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kaervek, the Punisher (OTJ #92, {1}{B}{B} 3/3).
 *
 *   Whenever you commit a crime, exile up to one target black card from your graveyard and copy
 *   it. You may cast the copy. If you do, you lose 2 life.
 *
 * Exercises the new `Effects.CastFromCollection(payManaCost)` path (the copy is cast paying its
 * normal mana cost, unlike Shiko's free cast) and the `IfYouDoEffect(..., CollectionNonEmpty)`
 * gate on the 2-life loss.
 */
class KaervekThePunisherScenarioTest : ScenarioTestBase() {

    init {
        context("Kaervek, the Punisher") {

            test("committing a crime copies a black graveyard card, casts it for its cost, and loses 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kaervek, the Punisher")
                    .withCardInGraveyard(1, "Skeletal Snake") // {1}{B} 2/1 black creature
                    .withCardInHand(1, "Lightning Bolt")      // the crime: target the opponent
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)     // to pay the {1}{B} copy
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Commit a crime: Lightning Bolt targeting the opponent. The crime trigger's
                // up-to-one target (the black graveyard card) is chosen as it's put on the stack.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Kaervek's crime trigger prompts for a target") {
                    game.hasPendingDecision().shouldBeTrue()
                }
                val snakeInGrave = game.findCardsInGraveyard(1, "Skeletal Snake").first()
                game.selectTargets(listOf(snakeInGrave))

                // Drain Bolt + the trigger up to the "you may cast the copy" prompt, then accept.
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("The copy resolved as a token on the battlefield") {
                    val snakeTokens = game.findPermanents("Skeletal Snake").filter {
                        game.state.getEntity(it)?.has<TokenComponent>() == true
                    }
                    snakeTokens.size shouldBe 1
                }
                withClue("Casting the copy costs the controller 2 life") {
                    game.state.lifeTotal(game.player1Id) shouldBe 18
                }
            }

            test("declining to cast the copy loses no life and leaves no token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kaervek, the Punisher")
                    .withCardInGraveyard(1, "Skeletal Snake")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                val snakeInGrave = game.findCardsInGraveyard(1, "Skeletal Snake").first()
                game.selectTargets(listOf(snakeInGrave))

                // Drain to the "you may cast the copy" prompt, then decline.
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("No life lost when the copy isn't cast") {
                    game.state.lifeTotal(game.player1Id) shouldBe 20
                }
                withClue("No Skeletal Snake token entered") {
                    game.findPermanents("Skeletal Snake").none {
                        game.state.getEntity(it)?.has<TokenComponent>() == true
                    }.shouldBeTrue()
                }
            }
        }
    }
}
