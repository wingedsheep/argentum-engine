package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MornsongAriaScenarioTest : ScenarioTestBase() {

    init {
        context("Mornsong Aria") {
            test("draw step draw is prevented and active player searches instead") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Mornsong Aria")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withLifeTotal(2, 20)
                    .withTurnNumber(2)
                    .withActivePlayer(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)

                withClue("Opponent's normal draw should be prevented") {
                    game.handSize(2) shouldBe 0
                }

                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(priorityPlayer))
                }

                val decision = game.getPendingDecision()
                withClue("Mornsong Aria should ask the active player to search their library") {
                    decision shouldNotBe null
                }
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player2Id
                decision.minSelections shouldBe 1
                decision.maxSelections shouldBe 1

                val swamp = decision.options.first { cardId ->
                    game.state.getEntity(cardId)?.get<CardComponent>()?.name == "Swamp"
                }
                game.selectCards(listOf(swamp))

                withClue("The active player should lose 3 life") {
                    game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life shouldBe 17
                }
                withClue("The selected card should be put into the active player's hand") {
                    game.isInHand(2, "Swamp") shouldBe true
                }
                withClue("Only the unselected card should remain in that player's library") {
                    game.librarySize(2) shouldBe 1
                }
            }
        }
    }
}
