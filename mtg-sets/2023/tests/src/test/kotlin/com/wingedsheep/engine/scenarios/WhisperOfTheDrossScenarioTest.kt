package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Whisper of the Dross (ONE #117) — {B} Instant.
 *
 *   Target creature gets -1/-1 until end of turn. Proliferate.
 *
 * Pins the -1/-1 shrinking or killing the target, and proliferate growing other permanents'
 * counters (including declining to proliferate anything).
 */
class WhisperOfTheDrossScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Whisper of the Dross")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("shrinks the target and proliferates another permanent's counters") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Whisper of the Dross", bears).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 1
            game.state.projectedState.getToughness(bears) shouldBe 1
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.getPower(giant) shouldBe 5
        }

        test("kills a 1/1 even when nothing is proliferated") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            // 2/2 with a -1/-1 counter is a 1/1.
            seed(game, bears, CounterType.MINUS_ONE_MINUS_ONE, 1)

            game.castSpell(1, "Whisper of the Dross", bears).error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("proliferate adds a -1/-1 counter to a different creature") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, giant, CounterType.MINUS_ONE_MINUS_ONE, 1)

            game.castSpell(1, "Whisper of the Dross", bears).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            count(game, giant, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
            game.state.projectedState.getToughness(giant) shouldBe 1
            game.state.projectedState.getToughness(bears) shouldBe 1
        }
    }
}
