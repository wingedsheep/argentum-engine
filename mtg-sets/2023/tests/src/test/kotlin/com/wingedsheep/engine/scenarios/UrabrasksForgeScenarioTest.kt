package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Urabrask's Forge (ONE #153) — {2}{R} Artifact.
 *
 * "At the beginning of combat on your turn, put an oil counter on this artifact, then create an
 * X/1 red Phyrexian Horror creature token with trample and haste, where X is the number of oil
 * counters on this artifact. Sacrifice that token at the beginning of the next end step."
 */
class UrabrasksForgeScenarioTest : ScenarioTestBase() {

    private val tokenName = "Phyrexian Horror Token"

    private fun oil(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    private fun board(activePlayer: Int = 1) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Urabrask's Forge")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("first combat: one oil counter, a 1/1 red trample haste Horror") {
            val game = board()
            val forge = game.findPermanent("Urabrask's Forge")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            oil(game, forge) shouldBe 1
            val token = game.findPermanents(tokenName).single()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(token, Keyword.HASTE) shouldBe true
            projected.getColors(token) shouldBe setOf(Color.RED.name)
        }

        test("X counts the counter just added, and the token is sacrificed at the next end step") {
            val game = board()
            val forge = game.findPermanent("Urabrask's Forge")!!
            game.state = game.state.updateEntity(forge) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, 2))
            }

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            oil(game, forge) shouldBe 3
            val token = game.findPermanents(tokenName).single()
            withClue("X = 3 oil counters after the new one lands") {
                game.state.projectedState.getPower(token) shouldBe 3
                game.state.projectedState.getToughness(token) shouldBe 1
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the delayed trigger sacrifices the Horror") {
                game.findPermanents(tokenName).shouldBeEmpty()
            }
            game.isOnBattlefield("Urabrask's Forge") shouldBe true
        }

        test("does not trigger on the opponent's turn") {
            val game = board(activePlayer = 2)
            val forge = game.findPermanent("Urabrask's Forge")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            game.state.stack shouldHaveSize 0
            oil(game, forge) shouldBe 0
            game.findPermanents(tokenName).shouldBeEmpty()
        }
    }
}
