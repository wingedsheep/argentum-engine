package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Serum Snare (ONE #68) — {1}{U} Instant.
 *
 * "Return target nonland permanent to its owner's hand. If that permanent had mana value 3 or
 * less, proliferate."
 */
class SerumSnareScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Resolve everything, answering each proliferate prompt with [choice]; returns the prompt count. */
    private fun resolveAll(game: TestGame, choice: EntityId): Int {
        var prompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) { game.selectCards(listOf(choice)); prompts++ } else game.resolveStack()
        }
        return prompts
    }

    private fun board(targetName: String): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Serum Snare")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, targetName)
        .withLandsOnBattlefield(1, "Island", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("bouncing a permanent with mana value exactly 3 proliferates") {
            val game = board("Gray Ogre")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val ogre = game.findPermanent("Gray Ogre")!!

            game.castSpell(1, "Serum Snare", targetId = ogre).error shouldBe null
            val prompts = resolveAll(game, bears)

            withClue("Gray Ogre returned to its owner's hand") { game.isInHand(2, "Gray Ogre") shouldBe true }
            withClue("Proliferate prompted once") { prompts shouldBe 1 }
            withClue("Our creature got another +1/+1 counter") { counters(game, bears) shouldBe 2 }
        }

        test("bouncing a permanent with mana value 4 or more does not proliferate") {
            val game = board("Hill Giant")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Serum Snare", targetId = giant).error shouldBe null
            val prompts = resolveAll(game, bears)

            withClue("Hill Giant returned to hand") { game.isInHand(2, "Hill Giant") shouldBe true }
            withClue("No proliferate prompt") { prompts shouldBe 0 }
            withClue("Counters unchanged") { counters(game, bears) shouldBe 1 }
        }
    }
}
