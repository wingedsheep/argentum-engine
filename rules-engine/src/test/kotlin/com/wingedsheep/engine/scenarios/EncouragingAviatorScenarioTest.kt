package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Encouraging Aviator. */
class EncouragingAviatorScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    init {
        // -------------------------------------------------------------------
        // Encouraging Aviator
        // -------------------------------------------------------------------
        test("Encouraging Aviator becomes prepared when it attacks and exposes the Jump copy") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Encouraging Aviator", summoningSickness = false) // 2/3 flyer
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            repeat(3) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            val aviator = game.findPermanent("Encouraging Aviator")!!
            withClue("does not start prepared") {
                game.state.getEntity(aviator)?.get<PreparedComponent>() shouldBe null
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Encouraging Aviator" to 2)).error shouldBe null
            game.resolveStack()

            withClue("attacking makes Encouraging Aviator prepared") {
                game.state.getEntity(aviator)?.get<PreparedComponent>() shouldNotBe null
            }
            withClue("a Jump prepare-spell copy now exists in exile") {
                game.findExileCopy(1, "Encouraging Aviator") shouldNotBe null
            }
        }

        test("casting the Jump copy grants flying until end of turn, then unprepares the Aviator") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Encouraging Aviator", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 grounded creature to receive flying
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            repeat(3) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            val aviator = game.findPermanent("Encouraging Aviator")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Encouraging Aviator" to 2)).error shouldBe null
            game.resolveStack()

            val copyId = game.findExileCopy(1, "Encouraging Aviator")!!
            withClue("Grizzly Bears has no flying before Jump") {
                game.state.projectedState.hasKeyword(bears, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe false
            }

            game.execute(
                CastSpell(
                    game.player1Id,
                    copyId,
                    targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears)),
                    faceIndex = 0
                )
            )
            game.resolveStack()

            withClue("Jump grants flying to the targeted creature") {
                game.state.projectedState.hasKeyword(bears, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe true
            }
            withClue("the Aviator is no longer prepared after casting the copy") {
                game.state.getEntity(aviator)?.get<PreparedComponent>() shouldBe null
            }
        }
    }
}
