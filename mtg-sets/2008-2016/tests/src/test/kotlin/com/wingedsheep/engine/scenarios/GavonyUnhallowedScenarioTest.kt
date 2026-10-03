package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Gavony Unhallowed (EMN #89) — "Whenever another creature you control dies, put a +1/+1 counter on this creature." */
class GavonyUnhallowedScenarioTest : ScenarioTestBase() {
    init {
        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("another creature you control dying puts a +1/+1 counter on it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Gavony Unhallowed")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gavony = game.findPermanent("Gavony Unhallowed")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            counters(game, gavony) shouldBe 1
            game.state.projectedState.getPower(gavony) shouldBe 3
            game.state.projectedState.getToughness(gavony) shouldBe 5
        }

        test("an opponent's creature dying does not trigger it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Gavony Unhallowed")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gavony = game.findPermanent("Gavony Unhallowed")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the opponent's Bears died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
            counters(game, gavony) shouldBe 0
        }
    }
}
