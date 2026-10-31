package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Barkhide Troll (M20 #165) — {G}{G} 2/2 Creature — Troll.
 *
 *   This creature enters with a +1/+1 counter on it.
 *   {1}, Remove a +1/+1 counter from this creature: This creature gains hexproof until end of turn.
 *
 * Enters as a 3/3; spending the counter makes it a hexproof 2/2, and with no counter left the
 * ability can't be activated again.
 */
class BarkhideTrollScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Barkhide Troll") {
            test("enters with a counter; removing it grants hexproof, and it can't be paid twice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Barkhide Troll")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("cast should succeed") { game.castSpell(1, "Barkhide Troll").error shouldBe null }
                game.resolveStack()

                val troll = game.findPermanent("Barkhide Troll")!!
                withClue("enters with one +1/+1 counter as a 3/3") {
                    game.plusOneCounters(troll) shouldBe 1
                    game.state.projectedState.getPower(troll) shouldBe 3
                    game.state.projectedState.getToughness(troll) shouldBe 3
                    game.state.projectedState.hasKeyword(troll, Keyword.HEXPROOF) shouldBe false
                }

                val abilityId = cardRegistry.getCard("Barkhide Troll")!!.script.activatedAbilities[0].id
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troll, abilityId = abilityId)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the counter is removed as a cost") { game.plusOneCounters(troll) shouldBe 0 }
                game.resolveStack()

                withClue("gains hexproof and is back to 2/2") {
                    game.state.projectedState.hasKeyword(troll, Keyword.HEXPROOF) shouldBe true
                    game.state.projectedState.getPower(troll) shouldBe 2
                    game.state.projectedState.getToughness(troll) shouldBe 2
                }

                val again = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troll, abilityId = abilityId)
                )
                withClue("no counter left to remove, so the cost can't be paid") { again.error shouldNotBe null }
            }
        }
    }
}
