package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skrelv's Hive (ONE #34) — {1}{W} Enchantment.
 *
 * "At the beginning of your upkeep, you lose 1 life and create a 1/1 colorless Phyrexian Mite
 *  artifact creature token with toxic 1 and 'This token can't block.'
 *  Corrupted — As long as an opponent has three or more poison counters, creatures you control
 *  with toxic have lifelink."
 */
class SkrelvsHiveScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun board(opponentPoison: Int = 0, ownPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Skrelv's Hive")
            .withCardOnBattlefield(1, "Crawling Chorus")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Crawling Chorus")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game
    }

    private fun TestGame.controlledBy(name: String, player: EntityId): EntityId =
        findPermanents(name).single { state.projectedState.getController(it) == player }

    init {
        test("at the beginning of your upkeep you lose 1 life and create a Phyrexian Mite") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Skrelv's Hive")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.findPermanent("Phyrexian Mite") shouldBe null
            game.getLifeTotal(1) shouldBe 20

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            val mite = game.findPermanent("Phyrexian Mite")
            mite shouldNotBe null
            game.state.projectedState.getController(mite!!) shouldBe game.player1Id
            game.state.projectedState.hasKeyword(mite, Keyword.TOXIC) shouldBe true
        }

        test("with corrupted, creatures you control with toxic have lifelink") {
            val game = board(opponentPoison = 3)
            val projected = game.state.projectedState
            projected.hasKeyword(game.controlledBy("Crawling Chorus", game.player1Id), Keyword.LIFELINK) shouldBe true
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.LIFELINK) shouldBe false
            projected.hasKeyword(game.controlledBy("Crawling Chorus", game.player2Id), Keyword.LIFELINK) shouldBe false
        }

        test("without an opponent at three poison, nothing has lifelink") {
            val game = board(opponentPoison = 2, ownPoison = 5)
            game.state.projectedState.hasKeyword(
                game.controlledBy("Crawling Chorus", game.player1Id),
                Keyword.LIFELINK
            ) shouldBe false
        }
    }
}
