package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rabble Rousing (SNC #24) — {4}{W} Enchantment.
 *
 *   Hideaway 5. Whenever you attack with one or more creatures, create that many 1/1 green and
 *   white Citizen creature tokens. Then if you control ten or more creatures, you may play the
 *   exiled card without paying its mana cost.
 *
 * Covers: "that many" counts the creatures declared as attackers (not every creature you control),
 * stays frozen when an attacker dies before the trigger resolves, and the free play is offered
 * only once the new Citizens bring you to ten creatures.
 */
class RabbleRousingScenarioTest : ScenarioTestBase() {

    init {
        context("Rabble Rousing") {

            test("creates one Citizen per creature you attacked with; no offer below ten creatures") {
                val game = rabbleGame(bears = 3)
                resolveHideaway(game)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                attackWith(game, game.findAllPermanents("Grizzly Bears").take(2))
                game.resolveStack()

                withClue("two Citizens for the two attackers, not one per creature you control") {
                    game.findAllPermanents("Citizen Token").size shouldBe 2
                }
                withClue("five creatures is short of ten: no free-play offer") {
                    game.getPendingDecision() shouldBe null
                }
            }

            test("the count is locked in at declaration even if an attacker dies in response") {
                val game = rabbleGame(bears = 0, extra = listOf("Grizzly Bears", "Hill Giant"))
                resolveHideaway(game)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt killed an attacker before the trigger resolved") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("two creatures were declared as attackers, so two Citizens") {
                    game.findAllPermanents("Citizen Token").size shouldBe 2
                }
            }

            test("reaching ten creatures offers the exiled card for free") {
                val game = rabbleGame(bears = 8)
                resolveHideaway(game)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                attackWith(game, game.findAllPermanents("Grizzly Bears").take(2))
                game.resolveStack()

                withClue("two Citizens bring you from eight creatures to ten") {
                    game.findAllPermanents("Citizen Token").size shouldBe 2
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the hidden Savannah Lions was played without paying its mana cost") {
                    game.state.getExile(game.player1Id) shouldBe emptyList()
                    game.findPermanent("Savannah Lions") shouldNotBe null
                }
            }
        }
    }

    private fun rabbleGame(bears: Int, extra: List<String> = emptyList()): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Rabble Rousing")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Plains", 5)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInLibrary(1, "Savannah Lions")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Sol Ring")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(bears) { builder = builder.withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) }
        extra.forEach { builder = builder.withCardOnBattlefield(1, it, summoningSickness = false) }
        return builder.build()
    }

    private fun attackWith(game: TestGame, attackers: List<EntityId>) {
        game.execute(DeclareAttackers(game.player1Id, attackers.associateWith { game.player2Id })).error shouldBe null
    }

    /** Cast Rabble Rousing and resolve Hideaway, exiling the top card (Savannah Lions). */
    private fun resolveHideaway(game: TestGame) {
        game.castSpell(1, "Rabble Rousing").error shouldBe null
        game.resolveStack()
        val decision = game.getPendingDecision() as? SelectCardsDecision
            ?: error("expected a hideaway selection, got ${game.getPendingDecision()}")
        game.selectCards(listOf(decision.options.first()))
    }
}
