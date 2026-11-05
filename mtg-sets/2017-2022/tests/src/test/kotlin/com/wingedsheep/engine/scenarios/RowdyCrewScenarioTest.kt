package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rowdy Crew (XLN #159): the first card that reads the cards a random discard put away — the
 * +1/+1 counters are gated on whether the two randomly discarded cards share a card type.
 * Each library is built so the random choice can't change the outcome.
 */
class RowdyCrewScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame): Int {
        val crew = game.findPermanent("Rowdy Crew")!!
        return game.state.getEntity(crew)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun castCrew(vararg library: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Rowdy Crew")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        library.forEach { builder.withCardInLibrary(1, it) }
        val game = builder.build()
        game.castSpell(1, "Rowdy Crew").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("two discarded cards sharing a card type put two +1/+1 counters on it") {
            val game = castCrew("Grizzly Bears", "Grizzly Bears", "Grizzly Bears")

            game.handSize(1) shouldBe 1
            game.graveyardSize(1) shouldBe 2
            plusOnes(game) shouldBe 2
        }

        test("two discarded cards sharing no card type leave it without counters") {
            val game = castCrew("Mountain", "Lightning Bolt", "Divination")

            game.handSize(1) shouldBe 1
            game.graveyardSize(1) shouldBe 2
            plusOnes(game) shouldBe 0
        }
    }
}
