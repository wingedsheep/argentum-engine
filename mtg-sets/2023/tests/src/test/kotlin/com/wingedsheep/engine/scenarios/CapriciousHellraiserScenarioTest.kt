package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Capricious Hellraiser (ONE #125) — {3}{R}{R}{R} 4/4 flying Phyrexian Dragon.
 *
 * "This spell costs {3} less to cast if you have nine or more cards in your graveyard.
 * When this creature enters, exile three cards at random from your graveyard. Choose a
 * noncreature, nonland card from among them and copy it. You may cast the copy without paying
 * its mana cost."
 */
class CapriciousHellraiserScenarioTest : ScenarioTestBase() {

    init {
        context("Capricious Hellraiser ETB") {

            test("exiles the three cards and casts a copy of the noncreature, nonland one for free") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInGraveyard(1, "Shock")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Capricious Hellraiser").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("The Shock copy deals 2 to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("All three graveyard cards are exiled") {
                    game.state.getGraveyard(game.player1Id).size shouldBe 0
                    exileNames(game).sorted() shouldBe listOf("Forest", "Grizzly Bears", "Shock")
                }
                game.isOnBattlefield("Capricious Hellraiser") shouldBe true
            }

            test("with two eligible cards the player chooses which one to copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInGraveyard(1, "Shock")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Capricious Hellraiser").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options.size shouldBe 2
                val bolt = decision.options.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.selectCards(listOf(bolt))
                game.answerYesNo(true)
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("The chosen Lightning Bolt is copied, not the Shock") {
                    game.getLifeTotal(2) shouldBe 17
                }
                exileNames(game).sorted() shouldBe listOf("Forest", "Lightning Bolt", "Shock")
            }

            test("declining the cast leaves only the exiled originals") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInGraveyard(1, "Shock")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Capricious Hellraiser").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)

                game.getLifeTotal(2) shouldBe 20
                withClue("Only the original Shock is in exile — no phantom copy") {
                    exileNames(game) shouldBe listOf("Shock")
                }
            }

            test("all creature/land cards exiled — nothing to copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Capricious Hellraiser").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                exileNames(game).sorted() shouldBe listOf("Forest", "Grizzly Bears")
                game.state.getGraveyard(game.player1Id).size shouldBe 0
            }
        }

        context("Capricious Hellraiser cost reduction") {

            test("costs {R}{R}{R} with nine cards in graveyard and exiles three of them on entry") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(9) { builder.withCardInGraveyard(1, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Capricious Hellraiser").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Capricious Hellraiser") shouldBe true
                game.hasPendingDecision() shouldBe false
                game.state.getGraveyard(game.player1Id).size shouldBe 6
                exileNames(game).size shouldBe 3
            }

            test("can't be cast with three lands and only eight cards in graveyard") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Capricious Hellraiser")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(8) { builder.withCardInGraveyard(1, "Forest") }
                val game = builder.build()

                (game.castSpell(1, "Capricious Hellraiser").error != null) shouldBe true
            }
        }
    }

    private fun exileNames(game: TestGame): List<String> =
        game.state.getExile(game.player1Id).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }
}
