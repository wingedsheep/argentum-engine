package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.GoldenHind
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Golden Hind (JOU #124) — {1}{G} Creature — Elk 2/1.
 *
 *   {T}: Add {G}.
 */
class GoldenHindScenarioTest : ScenarioTestBase() {
    init {
        test("tapping it adds one green mana") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Golden Hind")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val hind = game.findPermanent("Golden Hind")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = hind,
                    abilityId = GoldenHind.activatedAbilities.first().id,
                )
            ).error shouldBe null

            withClue("one {G} in the pool, the Hind is tapped") {
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                pool.green shouldBe 1
                game.state.getEntity(hind)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
