package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Cynical Loner. */
class CynicalLonerScenarioTest : ScenarioTestBase() {

    init {
        context("Cynical Loner — Survival (mill self to graveyard)") {
            test("a tapped Cynical Loner searches a card from library to graveyard at second main") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cynical Loner", tapped = true)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                val p1 = com.wingedsheep.sdk.model.EntityId.of("player-1")
                // Drive: the Survival ability is a MayEffect → first a YesNo ("you may search"),
                // then a SelectCards library search. Answer yes, then pick the Swamp.
                var guard = 0
                while (!game.isInGraveyard(1, "Swamp") && guard < 30) {
                    val decision = game.state.pendingDecision
                    when (decision) {
                        is com.wingedsheep.engine.core.YesNoDecision -> game.answerYesNo(true)
                        is com.wingedsheep.engine.core.SelectCardsDecision -> {
                            val swampInLib = game.state.getLibrary(p1).first { id ->
                                game.state.getEntity(id)
                                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Swamp"
                            }
                            game.selectCards(listOf(swampInLib))
                        }
                        else -> game.resolveStack()
                    }
                    guard++
                }

                withClue("the chosen card is now in the graveyard") {
                    game.isInGraveyard(1, "Swamp") shouldBe true
                }
            }

            test("an untapped Cynical Loner does NOT fire Survival") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cynical Loner", tapped = false)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { if (!game.hasPendingDecision()) game.resolveStack() }

                withClue("no Survival selection — the Loner is untapped") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Swamp") shouldBe false
                }
            }
        }
    }
}
