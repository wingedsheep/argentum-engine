package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Vaporous Djinn and the phasing mechanic (Rule 702.26).
 *
 * Vaporous Djinn: {2}{U}{U} Creature — Djinn 3/4, Flying
 * At the beginning of your upkeep, this creature phases out unless you pay {U}{U}.
 */
class VaporousDjinnScenarioTest : ScenarioTestBase() {

    /** True if the named permanent is physically present but phased out (treated as nonexistent). */
    private fun TestGame.isPhasedOut(cardName: String): Boolean =
        state.allBattlefieldEntities().any { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == cardName &&
                state.getEntity(id)?.has<PhasedOutComponent>() == true
        }

    init {
        context("Vaporous Djinn upkeep phasing") {

            test("phases out when the controller declines to pay {U}{U}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vaporous Djinn")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Upkeep trigger should ask whether to pay") {
                    game.hasPendingDecision() shouldBe true
                }

                // Decline to pay → the Djinn phases out.
                game.answerYesNo(false)

                withClue("Phased-out permanent is treated as though it doesn't exist") {
                    game.isOnBattlefield("Vaporous Djinn") shouldBe false
                }
                withClue("It is phased out, not destroyed — still physically present") {
                    game.isPhasedOut("Vaporous Djinn") shouldBe true
                }
                withClue("Phasing is not a zone change, so it is not in the graveyard") {
                    game.isInGraveyard(1, "Vaporous Djinn") shouldBe false
                }
            }

            test("phases back in before the controller's next untap step, not the opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vaporous Djinn")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardsInHand(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardsInHand(2, "Forest", 1)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Turn 1 (Player): decline → phase out.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.answerYesNo(false)
                game.isPhasedOut("Vaporous Djinn") shouldBe true

                // Advance through the opponent's whole turn.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Opponent's upkeep (turn 2)

                withClue("Opponent's untap step must NOT phase the Djinn back in (CR 702.26e)") {
                    game.isPhasedOut("Vaporous Djinn") shouldBe true
                    game.isOnBattlefield("Vaporous Djinn") shouldBe false
                }

                // Advance to the controller's next turn.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Player's upkeep (turn 3)

                withClue("Controller's untap step phases the Djinn back in (trigger not yet resolved)") {
                    game.isOnBattlefield("Vaporous Djinn") shouldBe true
                    game.isPhasedOut("Vaporous Djinn") shouldBe false
                }
            }

            test("stays on the battlefield when the controller pays {U}{U}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vaporous Djinn")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Pay {U}{U} → the Djinn does not phase out. The PayOrSuffer mana path
                // auto-taps and deducts the cost, so no separate mana-source decision follows.
                game.answerYesNo(true)

                withClue("Paying the upkeep cost keeps the Djinn in play") {
                    game.isOnBattlefield("Vaporous Djinn") shouldBe true
                }
                withClue("It is not phased out") {
                    game.isPhasedOut("Vaporous Djinn") shouldBe false
                }
            }
        }
    }
}
