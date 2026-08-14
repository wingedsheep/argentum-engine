package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class FalkenrathNobleScenarioTest : ScenarioTestBase() {
    init {
        test("when another creature dies, target player loses 1 and you gain 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Falkenrath Noble")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")
            bears.shouldNotBeNull()
            val shock = game.findCardsInHand(1, "Shock").single()

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = shock,
                    targets = listOf(entityIdToChosenTarget(game.state, bears)),
                ),
            )
            withClue("Shock cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            // After Shock resolves, Bears die and Noble triggers — choose player 2
            withClue("Noble trigger should ask for a player target") {
                (game.state.pendingDecision as? ChooseTargetsDecision).shouldNotBeNull()
            }
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
            game.isOnBattlefield("Falkenrath Noble") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("the Noble's own death triggers it — 'this creature or another'") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Falkenrath Noble")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val noble = game.findPermanent("Falkenrath Noble")
            noble.shouldNotBeNull()
            val shock = game.findCardsInHand(1, "Shock").single()

            // Shock deals 2 to the 2/2 Noble — it dies to its own trigger's source leaving play.
            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = shock,
                    targets = listOf(entityIdToChosenTarget(game.state, noble)),
                ),
            )
            withClue("Shock cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("TriggerBinding.ANY means the Noble sees itself die") {
                (game.state.pendingDecision as? ChooseTargetsDecision).shouldNotBeNull()
            }
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.isOnBattlefield("Falkenrath Noble") shouldBe false
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
