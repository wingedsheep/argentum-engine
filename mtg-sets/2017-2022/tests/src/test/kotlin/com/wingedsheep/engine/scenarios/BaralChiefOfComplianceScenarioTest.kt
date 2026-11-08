package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Baral, Chief of Compliance (AER #28).
 *
 * {1}{U} Legendary Creature — Human Wizard 1/3
 * "Instant and sorcery spells you cast cost {1} less to cast.
 *  Whenever a spell or ability you control counters a spell, you may draw a card. If you do,
 *  discard a card."
 *
 * Pins the counter trigger: it fires for your counter (and Cancel costs only {U}{U}), the loot is
 * optional, and it stays silent when an opponent's spell does the countering or when the spell
 * can't be countered (nothing was countered).
 */
class BaralChiefOfComplianceScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Baral with Cancel in hand and two Islands; player 2 is active. */
    private fun opponentCasts(creature: String, forests: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Baral, Chief of Compliance")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(1, "Cancel")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(2, creature)
            .withLandsOnBattlefield(2, "Forest", forests)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()

        game.castSpell(2, creature).error shouldBe null
        game.passPriority().error shouldBe null // opponent passes; you get priority
        withClue("Baral makes Cancel cost {U}{U}, payable with two Islands") {
            game.castSpellTargetingStackSpell(1, "Cancel", creature).error shouldBe null
        }
        game.resolveStack()
        return game
    }

    init {
        context("Baral, Chief of Compliance") {

            test("countering a spell lets you draw a card, then discard a card") {
                val game = opponentCasts("Grizzly Bears", forests = 2)
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true

                withClue("Baral's trigger asks whether to loot") {
                    game.getPendingDecision() shouldNotBe null
                }
                val handBefore = game.handSize(1)
                game.answerYesNo(true)
                withClue("after drawing, you choose a card to discard") {
                    game.getPendingDecision() shouldNotBe null
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("the loot is optional") {
                val game = opponentCasts("Grizzly Bears", forests = 2)
                val handBefore = game.handSize(1)
                val libraryBefore = game.librarySize(1)
                game.answerYesNo(false)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.librarySize(1) shouldBe libraryBefore
            }

            test("no trigger when the spell can't be countered") {
                val game = opponentCasts("Carnage Tyrant", forests = 6)
                withClue("Carnage Tyrant resolves; nothing was countered") {
                    game.findPermanent("Carnage Tyrant") shouldNotBe null
                    game.getPendingDecision() shouldBe null
                }
            }

            test("no trigger when an opponent's spell counters a spell") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Baral, Chief of Compliance")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(2, "Counterspell")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Island") }
                val game = builder.build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpellTargetingStackSpell(2, "Counterspell", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("the opponent controlled the counter, so Baral stays silent") {
                    game.getPendingDecision() shouldBe null
                }
            }
        }
    }
}
