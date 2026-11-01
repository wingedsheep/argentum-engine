package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Biogenic Ooze (RNA #122) — ETB Ooze token, end-step +1/+1 counter on each Ooze you control,
 * and a {1}{G}{G}{G} token maker.
 */
class BiogenicOozeScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.counters(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("enters with a token, the activated ability makes another, and the end step grows every Ooze") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Biogenic Ooze")
                .withLandsOnBattlefield(1, "Forest", 9)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Biogenic Ooze").error shouldBe null
            game.resolveStack()
            game.findPermanents("Ooze Token").size shouldBe 1

            val ooze = game.findPermanent("Biogenic Ooze")!!
            val ability = cardRegistry.getCard("Biogenic Ooze")!!.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, ooze, ability)).error shouldBe null
            game.resolveStack()
            val tokens = game.findPermanents("Ooze Token")
            tokens.size shouldBe 2

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.counters(ooze) shouldBe 1
            tokens.forEach { game.counters(it) shouldBe 1 }
            game.counters(game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
