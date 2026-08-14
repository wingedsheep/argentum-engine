package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Pictures of Spider-Man (SPM #109) — {2}{G} Artifact.
 *
 *   When this artifact enters, look at the top five cards of your library. You may reveal up to
 *   two creature cards from among them and put them into your hand. Put the rest on the bottom of
 *   your library in a random order.
 *   {1}, {T}, Sacrifice this artifact: Create a Treasure token.
 *
 * Covers (1) the ETB dig — look at top five, keep up to two creatures (revealed) to hand and bottom
 * the rest, and (2) the {1}, {T}, Sacrifice activated ability creating a Treasure token.
 */
class PicturesOfSpiderManScenarioTest : ScenarioTestBase() {

    init {
        context("Pictures of Spider-Man") {

            test("ETB: look at top five, keep up to two creatures, bottom the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pictures of Spider-Man")
                    .withLandsOnBattlefield(1, "Forest", 3) // {2}{G}
                    // Library is exactly the five looked-at cards: two creatures + three lands.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Pictures of Spider-Man")
                withClue("Casting Pictures of Spider-Man should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                // Resolve the artifact; the ETB trigger pauses on the "up to two creatures" reveal.
                game.resolveStack()

                withClue("The artifact resolved onto the battlefield") {
                    game.isOnBattlefield("Pictures of Spider-Man") shouldBe true
                }

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The reveal is optional (\"may\") and caps at two creatures") {
                    decision.minSelections shouldBe 0
                    decision.maxSelections shouldBe 2
                }

                // Only the two creature cards are eligible; take both.
                val creatureOptions = decision.options.filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                creatureOptions.size shouldBe 2
                game.selectCards(creatureOptions)

                withClue("Both revealed creatures were put into hand") {
                    game.handSize(1) shouldBe 2
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 2
                }
                withClue("The three non-creature cards were bottomed, none kept") {
                    game.isInHand(1, "Forest") shouldBe false
                    game.librarySize(1) shouldBe 3
                    game.findCardsInLibrary(1, "Forest").size shouldBe 3
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 0
                }
            }

            test("Activated ability: {1}, {T}, Sacrifice creates a Treasure token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pictures of Spider-Man")
                    .withLandsOnBattlefield(1, "Forest", 1) // {1}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val abilityId = cardRegistry.getCard("Pictures of Spider-Man")!!
                    .activatedAbilities.first().id
                val artifact = game.findPermanent("Pictures of Spider-Man")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = artifact,
                        abilityId = abilityId
                    )
                )
                withClue("Activating {1}, {T}, Sacrifice should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Sacrifice is a cost, so the artifact leaves the battlefield immediately.
                withClue("Pictures of Spider-Man was sacrificed to pay the cost") {
                    game.isOnBattlefield("Pictures of Spider-Man") shouldBe false
                }

                game.resolveStack()

                withClue("A Treasure token was created") {
                    game.findPermanent("Treasure").shouldNotBeNull()
                }
            }
        }
    }
}
