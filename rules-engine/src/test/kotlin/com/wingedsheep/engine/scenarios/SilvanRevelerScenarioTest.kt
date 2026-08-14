package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Silvan Reveler — ETB loot whose land branch reanimates the discarded land, plus a landfall
 * ability that only functions from the graveyard.
 *
 * The three things worth pinning:
 *  1. Discarding a **land** puts it onto the battlefield **tapped** and out of the graveyard.
 *  2. Discarding a **nonland** leaves it in the graveyard — the branch really is filtered.
 *  3. The landfall ability fires from the **graveyard**, and only returns the card when the
 *     optional {1}{G}{U} is actually paid.
 */
class SilvanRevelerScenarioTest : ScenarioTestBase() {

    init {
        context("Silvan Reveler") {

            test("discarding a land to the ETB puts it onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Silvan Reveler")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    // The only card to draw, so it is also the only card that can be discarded.
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Silvan Reveler").error shouldBe null
                game.resolveStack()

                withClue("the discarded Mountain came back onto the battlefield") {
                    game.isOnBattlefield("Mountain") shouldBe true
                    game.isInGraveyard(1, "Mountain") shouldBe false
                }
                withClue("it entered tapped") {
                    val mountain = game.findPermanent("Mountain")!!
                    game.state.getEntity(mountain)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the loot still drew and discarded — hand is empty again") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("discarding a nonland leaves it in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Silvan Reveler")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Silvan Reveler").error shouldBe null
                game.resolveStack()

                withClue("a nonland discard stays discarded") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                    game.isOnBattlefield("Lightning Bolt") shouldBe false
                }
            }

            test("landfall from the graveyard returns it to hand when the cost is paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Silvan Reveler")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Mountain").single())
                ).error shouldBe null

                game.resolveStack()
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("paying {1}{G}{U} returned the Reveler from the graveyard to hand") {
                    game.isInHand(1, "Silvan Reveler") shouldBe true
                    game.isInGraveyard(1, "Silvan Reveler") shouldBe false
                }
            }

            test("declining the landfall cost leaves it in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Silvan Reveler")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Mountain").single())
                ).error shouldBe null

                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("declining is free and changes nothing") {
                    game.isInGraveyard(1, "Silvan Reveler") shouldBe true
                    game.isInHand(1, "Silvan Reveler") shouldBe false
                }
            }
        }
    }
}
