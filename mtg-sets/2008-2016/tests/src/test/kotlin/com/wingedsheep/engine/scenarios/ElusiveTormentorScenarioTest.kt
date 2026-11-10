package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elusive Tormentor // Insidious Mist (SOI #108).
 *
 *   Front (4/4) — "{1}, Discard a card: Transform this creature."
 *   Back  (0/1) — "Hexproof, indestructible / This creature can't block and can't be blocked. /
 *                  Whenever this creature attacks and isn't blocked, you may pay {2}{B}. If you
 *                  do, transform it."
 *
 * What depends on the engine rather than the script: flipping back to the 4/4 mid-combat keeps it
 * an unblocked attacker, so it deals 4 combat damage (the second ruling); declining leaves a 0/1
 * that deals nothing; and two stacked activations transform it only once (CR 701.27g, the first
 * ruling).
 */
class ElusiveTormentorScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        context("Insidious Mist — attacks and isn't blocked") {

            fun mistAttacks(): Pair<TestGame, EntityId> {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Insidious Mist", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mist = game.findPermanent("Insidious Mist")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Insidious Mist" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.resolveStack()
                return game to mist
            }

            test("paying {2}{B} flips it back to a 4/4 that still deals 4 combat damage") {
                val (game, mist) = mistAttacks()

                withClue("the unblocked trigger offers the {2}{B} payment") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                withClue("transformed to the front face") {
                    game.nameOf(mist) shouldBe "Elusive Tormentor"
                    game.state.projectedState.getPower(mist) shouldBe 4
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("still an unblocked attacker after transforming — 4 damage to the defender") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("declining leaves an unblocked 0/1 that deals no damage") {
                val (game, mist) = mistAttacks()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.nameOf(mist) shouldBe "Insidious Mist"
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("Elusive Tormentor — {1}, Discard a card") {

            test("two stacked activations discard two cards but transform it only once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elusive Tormentor", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tormentor = game.findPermanent("Elusive Tormentor")!!
                val abilityId = cardRegistry.getCard("Elusive Tormentor")!!.activatedAbilities.first().id
                val (first, second) = game.findCardsInHand(1, "Grizzly Bears")

                for (discard in listOf(first, second)) {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = tormentor,
                            abilityId = abilityId,
                            costPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
                        )
                    ).error shouldBe null
                    game.payIfAsked()
                }

                withClue("both cards were discarded as costs") {
                    game.handSize(1) shouldBe 0
                }

                game.resolveStack()

                withClue("only the first ability to resolve transforms it; the second is ignored") {
                    game.nameOf(tormentor) shouldBe "Insidious Mist"
                    game.state.projectedState.getPower(tormentor) shouldBe 0
                    game.state.projectedState.getToughness(tormentor) shouldBe 1
                }
            }
        }
    }
}
