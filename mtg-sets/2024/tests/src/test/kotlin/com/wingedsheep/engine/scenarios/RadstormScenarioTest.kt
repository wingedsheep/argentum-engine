package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Radstorm (PIP #37) — {3}{U} Instant.
 *
 *   Storm. Proliferate.
 *
 * Each storm copy proliferates on its own, so with two spells cast before it the counter
 * grows three times.
 */
class RadstormScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Radstorm")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Resolve the whole stack, choosing [chosen] at every proliferate prompt; returns the prompt count. */
    private fun resolveProliferating(game: TestGame, chosen: EntityId): Int {
        var prompts = 0
        game.resolveStack()
        while (game.state.pendingDecision != null && prompts < 10) {
            game.selectCards(listOf(chosen)).error shouldBe null
            prompts++
            game.resolveStack()
        }
        return prompts
    }

    init {
        test("storm copies each proliferate: two earlier spells means three proliferates") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)
            game.state = game.state.copy(spellsCastThisTurn = 2)

            game.castSpell(1, "Radstorm").error shouldBe null
            resolveProliferating(game, giant) shouldBe 3

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
            game.isInGraveyard(1, "Radstorm") shouldBe true
        }

        test("with no earlier spells it proliferates once") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Radstorm").error shouldBe null
            resolveProliferating(game, giant) shouldBe 1

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }
    }
}
