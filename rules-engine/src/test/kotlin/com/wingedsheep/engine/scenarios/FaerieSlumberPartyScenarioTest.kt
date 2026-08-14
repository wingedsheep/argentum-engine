package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Faerie Slumber Party (WOE #311).
 *
 *   {4}{U}{U} Sorcery
 *   Return all creatures to their owners' hands. For each opponent who controlled a creature
 *   returned this way, you create two 1/1 blue Faerie creature tokens with flying and
 *   "This token can block only creatures with flying."
 *
 * The ordering is the trap: the controllers have to be snapshotted *before* the bounce (afterwards
 * `ControllerComponent` is gone and a hand is keyed by owner) while the tokens have to be created
 * *after* it (created first, they are creatures and would be returned themselves, ceasing to
 * exist). Both directions are pinned here — "two tokens survive on the battlefield" fails if the
 * tokens are made too early, and "two tokens" rather than "zero" fails if the count is taken too
 * late. The first test also separates "per opponent" from "per creature": one opponent with two
 * creatures is still two Faeries, which is what de-duplicating the captured controller list buys.
 */
class FaerieSlumberPartyScenarioTest : ScenarioTestBase() {

    init {
        context("Faerie Slumber Party") {

            test("bounces every creature and pays off two Faeries for the one opponent hit") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Faerie Slumber Party")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Faerie Slumber Party").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("your own creature goes back to your hand too") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("both of the opponent's creatures go back to their hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.isInHand(2, "Savannah Lions") shouldBe true
                }
                withClue(
                    "one opponent controlled a returned creature → two Faerie tokens, and they are " +
                        "still on the battlefield because they were made after the bounce"
                ) {
                    game.findAllPermanents("Faerie Token").size shouldBe 2
                }
            }

            test("an opponent with no creatures pays off nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Faerie Slumber Party")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Faerie Slumber Party").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("your creature still bounces") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("no opponent controlled a returned creature → no tokens") {
                    game.findAllPermanents("Faerie Token").size shouldBe 0
                }
            }

            test("an empty board resolves cleanly with no tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Faerie Slumber Party")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Faerie Slumber Party").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("nothing to return and nothing to pay off") {
                    game.findAllPermanents("Faerie Token").size shouldBe 0
                }
                withClue("the sorcery itself is in the graveyard") {
                    game.isInGraveyard(1, "Faerie Slumber Party") shouldBe true
                }
            }
        }
    }
}
