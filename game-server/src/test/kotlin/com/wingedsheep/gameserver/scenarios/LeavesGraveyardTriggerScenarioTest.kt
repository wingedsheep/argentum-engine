package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the "leave your graveyard during your turn" batching trigger
 * (CardsLeftYourGraveyardEvent), exercised through the Tarkir: Dragonstorm cards that use it.
 *
 * The trigger fires once per event batch when one or more cards leave the controller's
 * graveyard. The "during your turn" restriction is `triggerCondition = Conditions.IsYourTurn`;
 * "only once each turn" is `oncePerTurn = true`. A card leaving the graveyard is driven here
 * by Raise Dead ("return target creature card from your graveyard to your hand").
 */
class LeavesGraveyardTriggerScenarioTest : ScenarioTestBase() {

    init {
        context("Attuned Hunter — put a +1/+1 counter when a card leaves your graveyard during your turn") {

            test("gains a +1/+1 counter when a creature returns from your graveyard on your turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Attuned Hunter")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", graveyardOwnerNumber = 1, targetCardName = "Grizzly Bears")
                game.resolveStack()

                withClue("Grizzly Bears should have left the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                val hunterId = game.findPermanent("Attuned Hunter")
                hunterId shouldNotBe null
                val hunter = game.getClientState(1).cards[hunterId!!]
                hunter shouldNotBe null
                withClue("Attuned Hunter should have a +1/+1 counter (3/3 -> 4/4)") {
                    hunter!!.power shouldBe 4
                    hunter.toughness shouldBe 4
                }
            }

            test("does not trigger off the opponent's graveyard on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Attuned Hunter")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", graveyardOwnerNumber = 2, targetCardName = "Grizzly Bears")
                game.resolveStack()

                val hunterId = game.findPermanent("Attuned Hunter")
                hunterId shouldNotBe null
                val hunter = game.getClientState(1).cards[hunterId!!]
                withClue("Attuned Hunter should stay 3/3 — the trigger watches its controller's own graveyard on their turn") {
                    hunter!!.power shouldBe 3
                    hunter.toughness shouldBe 3
                }
            }
        }

        context("Kishla Skimmer — draw a card, but only once each turn") {

            test("draws once when a card leaves your graveyard, and not again on a second leave the same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kishla Skimmer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLibrary = game.librarySize(1)

                // First leave-graveyard event: Kishla triggers and draws one card.
                game.castSpellTargetingGraveyardCard(1, "Raise Dead", graveyardOwnerNumber = 1, targetCardName = "Grizzly Bears")
                game.resolveStack()
                withClue("first card leaving the graveyard should draw one card") {
                    game.librarySize(1) shouldBe startingLibrary - 1
                }

                // Second leave-graveyard event the same turn: triggers only once each turn, so no draw.
                game.castSpellTargetingGraveyardCard(1, "Raise Dead", graveyardOwnerNumber = 1, targetCardName = "Grizzly Bears")
                game.resolveStack()
                withClue("second card leaving the graveyard the same turn should NOT draw again (only once each turn)") {
                    game.librarySize(1) shouldBe startingLibrary - 1
                }
            }
        }
    }
}
