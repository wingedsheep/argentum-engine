package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tendershoot Dryad and the Ascend / city's blessing mechanic.
 *
 * Tendershoot Dryad ({4}{G}, 2/2 Dryad):
 *   Ascend (CR 702.131): when this enters, if you control 10+ permanents, you get
 *   the city's blessing for the rest of the game.
 *   At the beginning of each upkeep, create a 1/1 green Saproling creature token.
 *   Saprolings you control get +2/+2 as long as you have the city's blessing.
 */
class TendershootDryadScenarioTest : ScenarioTestBase() {

    init {
        context("Ascend — ETB grants the city's blessing") {

            test("controller does NOT get the blessing with fewer than 10 permanents") {
                // 8 Forests + Dryad cast = 9 permanents total → no blessing.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInHand(1, "Tendershoot Dryad")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tendershoot Dryad")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Dryad should be on the battlefield") {
                    game.isOnBattlefield("Tendershoot Dryad") shouldBe true
                }
                withClue("P1 controls only 9 permanents, no blessing") {
                    game.state.getEntity(game.player1Id)
                        ?.has<PlayerCitysBlessingComponent>() shouldBe false
                }
            }

            test("controller gets the blessing with exactly 10 permanents on resolution") {
                // 9 Forests + Dryad cast = 10 permanents → blessing granted.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withLandsOnBattlefield(1, "Forest", 9)
                    .withCardInHand(1, "Tendershoot Dryad")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tendershoot Dryad")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("P1 should have the city's blessing") {
                    game.state.getEntity(game.player1Id)
                        ?.has<PlayerCitysBlessingComponent>() shouldBe true
                }
                withClue("Only the controller gets the blessing — opponents do not") {
                    game.state.getEntity(game.player2Id)
                        ?.has<PlayerCitysBlessingComponent>() shouldBe false
                }
            }
        }

        context("City's blessing is permanent for the rest of the game (CR 702.131c)") {

            test("blessing persists when the granting permanent leaves the battlefield") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withLandsOnBattlefield(1, "Forest", 9)
                    .withCardInHand(1, "Tendershoot Dryad")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tendershoot Dryad")
                game.resolveStack()

                withClue("blessing was granted") {
                    game.state.getEntity(game.player1Id)
                        ?.has<PlayerCitysBlessingComponent>() shouldBe true
                }

                // Wipe every permanent P1 controls. Blessing must remain.
                val battlefieldEntities = game.state.getBattlefield()
                    .filter { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.ownerId == game.player1Id
                    }
                game.state = battlefieldEntities.fold(game.state) { acc, id ->
                    acc.removeFromZone(
                        com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD),
                        id
                    )
                }

                withClue("blessing is permanent — survives loss of all permanents") {
                    game.state.getEntity(game.player1Id)
                        ?.has<PlayerCitysBlessingComponent>() shouldBe true
                }
            }
        }

        context("Lord effect — Saprolings get +2/+2 while you have the city's blessing") {

            test("Saproling tokens produced after blessing are 3/3 (1/1 base + 2/2 lord)") {
                // Set up post-blessing world: Dryad in play, blessing granted, P1 with
                // enough Forests that the upkeep token-creation trigger has a permanent
                // source. Lord effect must apply once the upkeep Saproling token enters.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withLandsOnBattlefield(1, "Forest", 9)
                    .withCardOnBattlefield(1, "Tendershoot Dryad", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Pre-grant the blessing as if Ascend had already fired. This isolates
                // the lord-effect projection from the ETB-trigger path tested above.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(PlayerCitysBlessingComponent)
                }

                // Advance to upkeep — the "At the beginning of each upkeep" trigger fires
                // and creates a 1/1 green Saproling token.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val saprolings = game.findAllPermanents("Saproling Token")
                withClue("upkeep trigger should have created a Saproling token") {
                    saprolings.isNotEmpty() shouldBe true
                }

                val saprolingId = saprolings.first()
                val projected = game.state.projectedState
                withClue("Saproling is base 1/1 + lord (+2/+2) = 3/3 with blessing active") {
                    projected.getPower(saprolingId) shouldBe 3
                    projected.getToughness(saprolingId) shouldBe 3
                }
            }

            test("Saproling is base 1/1 when controller does not have the blessing") {
                // Same board, but no blessing — lord should not apply.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Tendershoot Dryad", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val saprolings = game.findAllPermanents("Saproling Token")
                withClue("upkeep trigger should have created a Saproling token") {
                    saprolings.isNotEmpty() shouldBe true
                }

                val saprolingId = saprolings.first()
                val projected = game.state.projectedState
                withClue("Saproling stays base 1/1 without the blessing") {
                    projected.getPower(saprolingId) shouldBe 1
                    projected.getToughness(saprolingId) shouldBe 1
                }
            }
        }
    }
}
