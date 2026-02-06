package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Annex.
 *
 * Card reference:
 * - Annex (2UU): Enchantment — Aura
 *   "Enchant land"
 *   "You control enchanted land."
 */
class AnnexScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Annex steals control of enchanted land") {
            test("casting Annex on opponent's land gives you control of it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                // Cast Annex targeting opponent's Forest
                val castResult = game.castSpell(1, "Annex", opponentForest)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Annex should be on the battlefield
                withClue("Annex should be on the battlefield") {
                    game.isOnBattlefield("Annex") shouldBe true
                }

                // Annex should be attached to the Forest
                val annexId = game.findPermanent("Annex")!!
                val annexEntity = game.state.getEntity(annexId)!!
                val attachedTo = annexEntity.get<AttachedToComponent>()
                withClue("Annex should be attached to the Forest") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe opponentForest
                }

                // The projected state should show P1 as controller of the Forest
                val projected = stateProjector.project(game.state)
                val forestController = projected.getController(opponentForest)
                withClue("Player 1 should control the enchanted Forest") {
                    forestController shouldBe game.player1Id
                }
            }

            test("Annex on own land keeps control unchanged") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownForest = game.findPermanent("Forest")!!

                // Cast Annex targeting own Forest
                val castResult = game.castSpell(1, "Annex", ownForest)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // The projected state should still show P1 as controller
                val projected = stateProjector.project(game.state)
                val forestController = projected.getController(ownForest)
                withClue("Player 1 should still control own Forest") {
                    forestController shouldBe game.player1Id
                }
            }
        }
    }
}
