package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scorching Dragonfire (canonical ELD #139, reprinted in DSK #156) — {1}{R} Instant.
 *
 * "Scorching Dragonfire deals 3 damage to target creature or planeswalker. If that creature or
 *  planeswalker would die this turn, exile it instead."
 *
 * The spell deals 3 damage then marks the target with [MarkExileOnDeathEffect], a death-replacement
 * that sends it to exile instead of the graveyard if it dies this turn.
 */
class ScorchingDragonfireScenarioTest : ScenarioTestBase() {

    init {
        context("Scorching Dragonfire") {

            test("kills a 2/2 creature and exiles it instead of putting it in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scorching Dragonfire")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Scorching Dragonfire", bears).error shouldBe null
                game.resolveStack()

                withClue("the 2/2 should be exiled, not in the graveyard") {
                    game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)) shouldContain bears
                    game.state.getZone(ZoneKey(game.player2Id, Zone.GRAVEYARD)) shouldNotContain bears
                }
            }

            test("a creature that survives the 3 damage stays on the battlefield with damage marked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scorching Dragonfire")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4, survives 3 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Scorching Dragonfire", wurm).error shouldBe null
                game.resolveStack()

                withClue("Craw Wurm survives 3 damage and stays in play") {
                    game.findPermanent("Craw Wurm") shouldBe wurm
                    game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
                }
            }
        }
    }
}
