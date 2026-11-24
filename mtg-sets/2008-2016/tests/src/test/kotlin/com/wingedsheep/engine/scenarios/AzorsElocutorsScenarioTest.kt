package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Azor's Elocutors — a filibuster counter each upkeep, the game won when the upkeep trigger resolves
 * with five or more, and one counter removed per source that deals damage to its controller.
 */
class AzorsElocutorsScenarioTest : ScenarioTestBase() {

    private fun filibuster(game: TestGame): Int =
        game.state.getEntity(game.findPermanent("Azor's Elocutors")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.FILIBUSTER) ?: 0

    private fun setFilibuster(game: TestGame, amount: Int) {
        val elocutors = game.findPermanent("Azor's Elocutors")!!
        game.state = game.state.updateEntity(elocutors) { c ->
            c.with(CountersComponent().withAdded(CounterType.FILIBUSTER, amount))
        }
    }

    private fun TestGame.passToPlayerOneUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    private fun upkeepGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Azor's Elocutors")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each upkeep adds a filibuster counter; three becomes four without winning") {
            val game = upkeepGame()
            setFilibuster(game, 3)

            game.passToPlayerOneUpkeep()

            filibuster(game) shouldBe 4
            game.state.gameOver shouldBe false
        }

        test("the fifth counter is checked on resolution and wins the game") {
            val game = upkeepGame()
            setFilibuster(game, 4)

            game.passToPlayerOneUpkeep()

            filibuster(game) shouldBe 5
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player1Id
        }

        test("each source dealing damage to you removes one counter, however much damage it dealt") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Azor's Elocutors", tapped = true)
                .withCardOnBattlefield(2, "Colossal Dreadmaw", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            setFilibuster(game, 4)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Colossal Dreadmaw" to 1, "Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(1) shouldBe 12
            withClue("two sources dealt damage at once: two counters, not eight") {
                filibuster(game) shouldBe 2
            }
        }
    }
}
