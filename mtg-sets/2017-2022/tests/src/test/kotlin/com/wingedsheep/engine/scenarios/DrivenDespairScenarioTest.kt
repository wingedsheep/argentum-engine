package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Driven // Despair (HOU #157).
 *
 *   Driven {1}{G}: Until end of turn, creatures you control gain trample and "Whenever this creature
 *   deals combat damage to a player, draw a card."
 *   Despair {1}{B}, Aftermath: Until end of turn, creatures you control gain menace and "Whenever
 *   this creature deals combat damage to a player, that player discards a card."
 *
 * Both halves grant a keyword plus a combat-damage trigger to each creature you control at
 * resolution. Despair's trigger must make the *damaged* player discard, not the caster.
 */
class DrivenDespairScenarioTest : ScenarioTestBase() {

    init {
        context("Driven // Despair") {

            test("Driven grants trample and a card draw per creature that connects, only to your creatures") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Driven // Despair")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val giant = game.findPermanent("Hill Giant")!!

                val driven = game.getLegalActions(1).firstOrNull { it.description == "Cast Driven" }
                withClue("Driven is offered from hand") { (driven != null) shouldBe true }
                game.execute(driven!!.action).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("your creatures gain trample") {
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(lions, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the opponent's creature does not") {
                    projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
                }

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("both attackers connected") { game.getLifeTotal(2) shouldBe 17 }
                withClue("each connecting creature draws its controller a card") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                withClue("the damaged player draws nothing") { game.handSize(2) shouldBe 0 }
            }

            test("Despair from the graveyard grants menace and makes the damaged player discard") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInGraveyard(1, "Driven // Despair")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(2, "Island")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val cardId = game.findCardsInGraveyard(1, "Driven // Despair").first()

                val despair = game.getLegalActions(1).firstOrNull { it.description == "Cast Despair (Aftermath)" }
                withClue("Despair is offered from the graveyard") { (despair != null) shouldBe true }
                game.execute(despair!!.action).error shouldBe null
                game.resolveStack()

                withClue("Despair, cast from the graveyard, is exiled") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe true
                }
                val projected = game.state.projectedState
                withClue("your creature gains menace; the opponent's does not") {
                    projected.hasKeyword(bears, Keyword.MENACE) shouldBe true
                    projected.hasKeyword(giant, Keyword.MENACE) shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("the menacing Bears connected unblocked") { game.getLifeTotal(2) shouldBe 18 }

                if (game.hasPendingDecision()) {
                    val island = game.state.getHand(game.player2Id).first()
                    game.selectCards(listOf(island)).error shouldBe null
                    game.resolveStack()
                }

                withClue("the damaged player discarded one card") { game.handSize(2) shouldBe 1 }
                withClue("the caster discarded nothing") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
