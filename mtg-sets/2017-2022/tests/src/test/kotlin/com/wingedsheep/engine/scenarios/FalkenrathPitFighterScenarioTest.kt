package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Falkenrath Pit Fighter (MID #137) — {R} 2/1.
 *
 *   {1}{R}, Discard a card, Sacrifice a Vampire: Draw two cards. Activate only if an opponent lost
 *   life this turn.
 *
 * Proves the activation gate and the three-part cost (it may sacrifice itself — it is a Vampire).
 */
class FalkenrathPitFighterScenarioTest : ScenarioTestBase() {

    init {
        context("Falkenrath Pit Fighter") {

            fun builder() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Falkenrath Pit Fighter", summoningSickness = false)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            fun activate(game: TestGame) = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Falkenrath Pit Fighter")!!,
                    abilityId = cardRegistry.getCard("Falkenrath Pit Fighter")!!.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(
                        discardedCards = listOf(game.findCardsInHand(1, "Grizzly Bears").single()),
                        sacrificedPermanents = listOf(game.findPermanent("Falkenrath Pit Fighter")!!)
                    )
                )
            )

            test("can't be activated when no opponent lost life this turn") {
                val game = builder().build()

                activate(game).error shouldNotBe null
                game.isOnBattlefield("Falkenrath Pit Fighter") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("after an opponent lost life, discard and sacrifice itself to draw two") {
                val game = builder().build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                activate(game).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Falkenrath Pit Fighter") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
            }
        }
    }
}
