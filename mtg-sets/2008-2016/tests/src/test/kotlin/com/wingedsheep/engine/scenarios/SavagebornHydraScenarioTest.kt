package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.SavagebornHydra
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Savageborn Hydra (DGM #100) — {X}{R}{G} 0/0 Creature — Hydra.
 *
 *   Double strike
 *   This creature enters with X +1/+1 counters on it.
 *   {1}{R/G}: Put a +1/+1 counter on this creature. Activate only as a sorcery.
 */
class SavagebornHydraScenarioTest : ScenarioTestBase() {
    init {
        test("enters with X counters and grows at sorcery speed") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Savageborn Hydra")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Savageborn Hydra", 3).error shouldBe null
            game.resolveStack()

            val hydra = game.findPermanent("Savageborn Hydra")!!
            fun counters() = game.state.getEntity(hydra)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            withClue("X = 3 → three +1/+1 counters") { counters() shouldBe 3 }
            game.state.projectedState.getPower(hydra) shouldBe 3
            game.state.projectedState.hasKeyword(hydra, Keyword.DOUBLE_STRIKE) shouldBe true

            val pump = game.execute(
                ActivateAbility(game.player1Id, hydra, SavagebornHydra.activatedAbilities.single().id)
            )
            withClue("activation succeeds in the main phase: ${pump.error}") { pump.error shouldBe null }
            game.resolveStack()
            withClue("one more counter") { counters() shouldBe 4 }
        }
    }
}
