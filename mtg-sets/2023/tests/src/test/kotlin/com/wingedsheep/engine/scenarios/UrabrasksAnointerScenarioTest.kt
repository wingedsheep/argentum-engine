package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urabrask's Anointer (ONE #152) — {3}{R} 4/2 Artifact Creature — Phyrexian Wizard.
 *
 *   When this creature enters, it deals X damage to any target, where X is the number of
 *   permanents you control with oil counters on them.
 */
class UrabrasksAnointerScenarioTest : ScenarioTestBase() {

    private fun seedOil(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Urabrask's Anointer")
        .withCardOnBattlefield(1, "Mindsplice Apparatus")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("deals damage equal to the number of permanents you control with oil counters") {
            val game = board()
            // Apparatus carries three oil counters but counts once; Bears carries one.
            seedOil(game, game.findPermanent("Mindsplice Apparatus")!!, 3)
            seedOil(game, game.findPermanent("Grizzly Bears")!!, 1)
            // An opponent's oiled permanent does not count.
            seedOil(game, game.findPermanent("Hill Giant")!!, 2)

            game.castSpell(1, "Urabrask's Anointer").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
        }

        test("can target a creature and kill it") {
            val game = board()
            seedOil(game, game.findPermanent("Mindsplice Apparatus")!!, 1)
            seedOil(game, game.findPermanent("Grizzly Bears")!!, 1)
            seedOil(game, game.findPermanent("Ornithopter")!!, 1)
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Urabrask's Anointer").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
        }

        test("deals no damage with no oil counters around") {
            val game = board()

            game.castSpell(1, "Urabrask's Anointer").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Urabrask's Anointer") shouldBe true
        }
    }
}
