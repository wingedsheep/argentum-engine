package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Corpse Explosion (SNC #179) — {1}{B}{R} Sorcery.
 *
 *   As an additional cost to cast this spell, exile a creature card from your graveyard.
 *   Corpse Explosion deals damage equal to the exiled card's power to each creature and each
 *   planeswalker.
 *
 * The damage reads the power of the card actually exiled to pay the cost, so choosing a different
 * card changes which creatures die.
 */
class CorpseExplosionScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Corpse Explosion")
        .withCardInGraveyard(1, "Hill Giant") // 3/3
        .withCardInGraveyard(1, "Grizzly Bears") // 2/2
        .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false) // 6/4
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Corpse Explosion") {

            test("exiling a 3-power card deals 3 to each creature") {
                val game = board()
                val exiled = game.findCardsInGraveyard(1, "Hill Giant").first()
                val card = game.findCardsInHand(1, "Corpse Explosion").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the cost exiled Hill Giant from the graveyard") {
                    game.isInExile(1, "Hill Giant") shouldBe true
                }
                withClue("opponent's 3/3 and 2/2 die to 3 damage") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("caster's own 6/4 is hit too but survives") {
                    val wurm = game.findPermanent("Craw Wurm")
                    wurm shouldNotBe null
                }
            }

            test("exiling a 2-power card deals only 2") {
                val game = board()
                val exiled = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                val card = game.findCardsInHand(1, "Corpse Explosion").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(exiled)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (3/3) survives 2 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("opponent's Grizzly Bears (2/2) dies") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("cannot be cast without a creature card to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Corpse Explosion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Corpse Explosion").first()

                game.execute(CastSpell(game.player1Id, card)).error shouldNotBe null
            }
        }
    }
}
