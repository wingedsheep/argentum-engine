package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Metastatic Evangel (MH3 #35) — {1}{W} 3/1.
 *
 *   Whenever another nontoken creature you control enters, proliferate.
 *
 * Pins the trigger on your own nontoken creature entering, and that neither the Evangel itself,
 * a creature token, nor an opponent's creature triggers it.
 */
class MetastaticEvangelScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("another nontoken creature you control entering proliferates") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Metastatic Evangel")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("the Evangel entering itself does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Metastatic Evangel")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Metastatic Evangel").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.state.stack.isEmpty() shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("a creature token entering under your control does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Metastatic Evangel")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.state.stack.isEmpty() shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("an opponent's creature entering does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Metastatic Evangel")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.state.stack.isEmpty() shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
