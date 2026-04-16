package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Glen Elendra's Answer.
 *
 * Card reference:
 * - Glen Elendra's Answer ({2}{U}{U}): Instant
 *   "This spell can't be countered.
 *    Counter all spells your opponents control and all abilities your opponents control.
 *    Create a 1/1 blue and black Faerie creature token with flying for each spell and
 *    ability countered this way."
 */
class GlenElendrasAnswerScenarioTest : ScenarioTestBase() {

    init {
        context("Glen Elendra's Answer — counter and token creation") {

            test("counters a single opponent spell and creates one Faerie token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Glen Elendra's Answer")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.execute(PassPriority(game.player1Id))

                val answerResult = game.castSpell(2, "Glen Elendra's Answer")
                withClue("Glen Elendra's Answer should be cast: ${answerResult.error}") {
                    answerResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should be countered into owner's graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not resolve onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("One Faerie token should be created for Player 2") {
                    game.findAllPermanents("Faerie Token").size shouldBe 1
                }
            }

            test("counters multiple opponent spells and creates a token for each") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Glory Seeker")
                    .withCardInHand(2, "Glen Elendra's Answer")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast: ${bearsResult.error}") {
                    bearsResult.error shouldBe null
                }
                game.execute(PassPriority(game.player1Id))
                game.execute(PassPriority(game.player2Id))

                val seekerResult = game.castSpell(1, "Glory Seeker")
                withClue("Glory Seeker should be cast on top of Grizzly Bears: ${seekerResult.error}") {
                    seekerResult.error shouldBe null
                }
                game.execute(PassPriority(game.player1Id))

                val answerResult = game.castSpell(2, "Glen Elendra's Answer")
                withClue("Glen Elendra's Answer should be cast: ${answerResult.error}") {
                    answerResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should have been countered") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Glory Seeker should have been countered") {
                    game.isInGraveyard(1, "Glory Seeker") shouldBe true
                }
                withClue("Two Faerie tokens should be created for Player 2") {
                    game.findAllPermanents("Faerie Token").size shouldBe 2
                }
            }

            test("creates no tokens when there are no opponent spells on the stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Glen Elendra's Answer")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val answerResult = game.castSpell(2, "Glen Elendra's Answer")
                withClue("Glen Elendra's Answer should be cast even with nothing to counter: ${answerResult.error}") {
                    answerResult.error shouldBe null
                }

                game.resolveStack()

                withClue("No Faerie tokens should be created when there was nothing to counter") {
                    game.findAllPermanents("Faerie Token").size shouldBe 0
                }
                withClue("Glen Elendra's Answer should be in the graveyard") {
                    game.isInGraveyard(2, "Glen Elendra's Answer") shouldBe true
                }
            }

            test("Glen Elendra's Answer can't be countered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cancel")
                    .withCardInHand(2, "Glen Elendra's Answer")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val answerResult = game.castSpell(2, "Glen Elendra's Answer")
                withClue("Glen Elendra's Answer should be cast: ${answerResult.error}") {
                    answerResult.error shouldBe null
                }
                game.execute(PassPriority(game.player2Id))

                val cancelResult = game.castSpellTargetingStackSpell(1, "Cancel", "Glen Elendra's Answer")
                withClue("Cancel should resolve normally but fail to counter Glen Elendra's Answer") {
                    cancelResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Glen Elendra's Answer should resolve and go to the graveyard (not countered)") {
                    game.isInGraveyard(2, "Glen Elendra's Answer") shouldBe true
                }
            }
        }
    }
}
