package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kheru Lich Lord.
 *
 * Kheru Lich Lord: {3}{B}{G}{U}
 * Creature — Zombie Wizard 4/4
 * At the beginning of your upkeep, you may pay {2}{B}. If you do, return a creature card
 * at random from your graveyard to the battlefield. It gains flying, trample, and haste.
 * Exile that card at the beginning of your next end step. If it would leave the battlefield,
 * exile it instead of putting it anywhere else.
 */
class KheruLichLordScenarioTest : ScenarioTestBase() {

    /**
     * Check if a card with the given name is in exile.
     */
    private fun TestGame.isInExile(cardName: String): Boolean {
        val exile1 = state.getZone(player1Id, Zone.EXILE)
        val exile2 = state.getZone(player2Id, Zone.EXILE)
        return (exile1 + exile2).any { entityId ->
            state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == cardName
        }
    }

    init {
        context("Kheru Lich Lord upkeep trigger") {

            test("returns a random creature from graveyard when you pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kheru Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                withClue("Graveyard should have Hill Giant") {
                    game.graveyardSize(1) shouldBe 1
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Player decides to pay {2}{B}
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                withClue("Hill Giant should be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Graveyard should be empty") {
                    game.graveyardSize(1) shouldBe 0
                }
            }

            test("does not return creature when declining to pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kheru Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Player declines to pay
                game.answerYesNo(false)

                withClue("Hill Giant should still be in graveyard") {
                    game.graveyardSize(1) shouldBe 1
                }
                withClue("Hill Giant should not be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }

            test("returned creature is exiled when destroyed (replacement effect)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kheru Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 8) // Enough for upkeep pay + Throttle
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInHand(1, "Throttle") // KTK sorcery: target creature gets -5/-5
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                withClue("Hill Giant should be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                // Advance to main phase and cast Throttle on the Hill Giant
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val hillGiant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Throttle", hillGiant)
                game.resolveStack()

                withClue("Hill Giant should not be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Hill Giant should be in exile (replacement effect), not graveyard") {
                    game.graveyardSize(1) shouldBe 1 // Throttle goes to graveyard after resolving
                    game.isInExile("Hill Giant") shouldBe true
                }
            }

            test("does nothing when graveyard has no creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kheru Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Player pays {2}{B} even though there are no creatures in graveyard
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                withClue("Only Kheru Lich Lord should be on the battlefield") {
                    game.isOnBattlefield("Kheru Lich Lord") shouldBe true
                    game.graveyardSize(1) shouldBe 0
                }
            }
        }
    }
}
