package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gloomfang Mauler (MOM #108) — {5}{B}{B} 5/5. Swampcycling {2}; Backup 2; Menace.
 *
 * Backup always puts two counters; only *another* creature also gains menace until end of turn.
 */
class GloomfangMaulerScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castMauler(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Gloomfang Mauler")
            .withLandsOnBattlefield(1, "Swamp", 7)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Gloomfang Mauler").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Gloomfang Mauler") {

            test("backup on another creature: two counters and menace until end of turn") {
                val game = castMauler()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                plusOnes(game, bears) shouldBe 2
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
                val mauler = game.findPermanent("Gloomfang Mauler")!!
                game.state.projectedState.hasKeyword(mauler, Keyword.MENACE) shouldBe true
            }

            test("backup on itself: only the counters, Grizzly Bears gains nothing") {
                val game = castMauler()
                val mauler = game.findPermanent("Gloomfang Mauler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(mauler)).error shouldBe null
                game.resolveStack()
                plusOnes(game, mauler) shouldBe 2
                game.state.projectedState.getPower(mauler) shouldBe 7
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe false
            }
        }
    }
}
