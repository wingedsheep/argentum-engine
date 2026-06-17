package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the four Secrets of Strixhaven prepare cards added together:
 *  - Studious First-Year // Rampant Growth   (enters prepared; search a basic land)
 *  - Spellbook Seeker // Careful Study        (enters prepared; draw 2, discard 2)
 *  - Pigment Wrangler // Striking Palette      (enters prepared; copy next spell)
 *  - Joined Researchers // Secret Rendezvous   (becomes prepared via end-step trigger)
 *
 * The first three "enter prepared" (they carry `Keyword.PREPARED`). Joined Researchers does NOT
 * enter prepared — it only becomes prepared via its end-step trigger ("if an opponent has more
 * cards in hand than you"), exercising the new `Effects.MakePrepared` effect.
 */
class SosPrepareCardsScenarioTest : ScenarioTestBase() {

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): com.wingedsheep.sdk.model.EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    init {
        context("Studious First-Year — Rampant Growth (enters prepared)") {
            test("enters prepared and the copy fetches a basic land onto the battlefield tapped") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Studious First-Year")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Studious First-Year")
                game.resolveStack()

                val firstYear = game.findPermanent("Studious First-Year")!!
                withClue("Studious First-Year should be prepared on ETB") {
                    game.state.getEntity(firstYear)?.get<PreparedComponent>() shouldNotBe null
                }
                val copyId = game.findExileCopy(1, "Studious First-Year")!!

                val landsBefore = game.state.getBattlefield()
                    .count { game.state.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true }

                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                // Rampant Growth pauses to search the library for a basic land — pick a Plains.
                val plains = game.findCardsInLibrary(1, "Plains").first()
                game.selectCards(listOf(plains))
                game.resolveStack()

                val landsAfter = game.state.getBattlefield()
                    .count { game.state.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true }
                withClue("Rampant Growth puts a basic land onto the battlefield") {
                    landsAfter shouldBe landsBefore + 1
                }
                withClue("Studious First-Year is no longer prepared after casting the copy") {
                    game.state.getEntity(firstYear)?.get<PreparedComponent>() shouldBe null
                }
            }
        }

        context("Spellbook Seeker — Careful Study (enters prepared)") {
            test("enters prepared and the copy draws two then discards two") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spellbook Seeker")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Spellbook Seeker")
                game.resolveStack()

                val seeker = game.findPermanent("Spellbook Seeker")!!
                withClue("Spellbook Seeker should be prepared on ETB") {
                    game.state.getEntity(seeker)?.get<PreparedComponent>() shouldNotBe null
                }
                val copyId = game.findExileCopy(1, "Spellbook Seeker")!!
                val handBefore = game.handSize(1)

                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                withClue("Careful Study draws two then discards two — net zero hand size change") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("Spellbook Seeker is no longer prepared after casting the copy") {
                    game.state.getEntity(seeker)?.get<PreparedComponent>() shouldBe null
                }
            }
        }

        context("Pigment Wrangler — Striking Palette (enters prepared)") {
            test("enters prepared and exposes a castable prepare-spell copy") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pigment Wrangler")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Pigment Wrangler")
                game.resolveStack()

                val wrangler = game.findPermanent("Pigment Wrangler")!!
                withClue("Pigment Wrangler should be prepared on ETB") {
                    game.state.getEntity(wrangler)?.get<PreparedComponent>() shouldNotBe null
                }
                val copyId = game.findExileCopy(1, "Pigment Wrangler")!!
                val prepareAction = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is CastSpell && a.cardId == copyId
                }
                withClue("Striking Palette should be offered as a {R} cast of face 0 from exile") {
                    prepareAction shouldNotBe null
                    (prepareAction!!.action as CastSpell).faceIndex shouldBe 0
                    prepareAction.sourceZone shouldBe "EXILE"
                    prepareAction.manaCostString shouldBe "{R}"
                }
            }
        }

        context("Joined Researchers — Secret Rendezvous (becomes prepared via trigger)") {
            test("does NOT enter prepared on ETB") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Joined Researchers")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Joined Researchers")
                game.resolveStack()

                val researchers = game.findPermanent("Joined Researchers")!!
                withClue("Joined Researchers must NOT enter prepared (no PREPARED keyword)") {
                    game.state.getEntity(researchers)?.get<PreparedComponent>() shouldBe null
                }
                withClue("No prepare-spell copy should exist in exile yet") {
                    game.findExileCopy(1, "Joined Researchers") shouldBe null
                }
            }

            test("becomes prepared at end step when an opponent has more cards in hand than you") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Joined Researchers", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                // Opponent hoards more cards in hand than the controller.
                repeat(4) { builder = builder.withCardInHand(2, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researchers = game.findPermanent("Joined Researchers")!!
                withClue("Sanity: opponent should have more cards in hand than the controller") {
                    (game.handSize(2) > game.handSize(1)) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("End-step trigger should make Joined Researchers prepared") {
                    game.state.getEntity(researchers)?.get<PreparedComponent>() shouldNotBe null
                }
                withClue("A Secret Rendezvous prepare-spell copy should now exist in exile") {
                    game.findExileCopy(1, "Joined Researchers") shouldNotBe null
                }
            }

            test("does NOT become prepared when you have at least as many cards as each opponent") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Joined Researchers", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                // Controller has more cards in hand than the opponent.
                repeat(4) { builder = builder.withCardInHand(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researchers = game.findPermanent("Joined Researchers")!!
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Trigger condition fails — Joined Researchers stays unprepared") {
                    game.state.getEntity(researchers)?.get<PreparedComponent>() shouldBe null
                }
                withClue("No prepare-spell copy should exist") {
                    game.findExileCopy(1, "Joined Researchers") shouldBe null
                }
            }

            test("casting the Secret Rendezvous copy draws three for you and the target opponent, then unprepares") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Joined Researchers", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(4) { builder = builder.withCardInHand(2, "Forest") }
                repeat(8) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(8) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researchers = game.findPermanent("Joined Researchers")!!
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Joined Researchers should be prepared after the end-step trigger") {
                    game.state.getEntity(researchers)?.get<PreparedComponent>() shouldNotBe null
                }

                // Secret Rendezvous is a sorcery — advance to the controller's *own* next precombat
                // main (an empty stack, sorcery-speed window) before casting the prepare-spell copy.
                // Step through end steps so each loop actually advances to a fresh turn.
                var guard = 0
                while (!(game.state.activePlayerId == game.player1Id &&
                        game.state.phase == Phase.PRECOMBAT_MAIN) && guard++ < 6
                ) {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.resolveStack()
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    game.resolveStack()
                }

                val copyId = game.findExileCopy(1, "Joined Researchers")!!
                val myHandBefore = game.handSize(1)
                val oppHandBefore = game.handSize(2)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        faceIndex = 0
                    )
                )
                game.resolveStack()

                withClue("You draw three cards") {
                    game.handSize(1) shouldBe myHandBefore + 3
                }
                withClue("Target opponent draws three cards") {
                    game.handSize(2) shouldBe oppHandBefore + 3
                }
                withClue("Joined Researchers is no longer prepared after casting the copy") {
                    game.state.getEntity(researchers)?.get<PreparedComponent>() shouldBe null
                }
                withClue("The prepare-spell copy should be gone from exile") {
                    game.findExileCopy(1, "Joined Researchers") shouldBe null
                }
            }
        }
    }
}
