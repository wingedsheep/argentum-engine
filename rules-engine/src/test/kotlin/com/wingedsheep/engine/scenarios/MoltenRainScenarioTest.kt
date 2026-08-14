package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Molten Rain — {1}{R}{R} Sorcery (Mirrodin #101)
 *
 * "Destroy target land. If that land was nonbasic, Molten Rain deals 2 damage to the land's
 *  controller."
 *
 * The land always dies; only the 2 damage is conditional. These tests pin both branches, since a
 * conditional that silently picks the wrong side is invisible in the card snapshot.
 */
class MoltenRainScenarioTest : ScenarioTestBase() {

    init {
        context("Molten Rain") {

            test("destroying a nonbasic land also deals 2 damage to its controller") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Molten Rain")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Great Furnace") // nonbasic artifact land
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                val furnace = game.findPermanent("Great Furnace")!!
                val cast = game.castSpell(1, "Molten Rain", furnace)
                withClue("Casting Molten Rain should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Great Furnace should be destroyed") {
                    game.isOnBattlefield("Great Furnace") shouldBe false
                    game.isInGraveyard(2, "Great Furnace") shouldBe true
                }
                withClue("Nonbasic land: its controller takes 2") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("destroying a basic land deals no damage") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Molten Rain")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val forest = game.findPermanent("Forest")!!
                val cast = game.castSpell(1, "Molten Rain", forest)
                withClue("Casting Molten Rain should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Forest should be destroyed") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }
                withClue("Basic land: no damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
