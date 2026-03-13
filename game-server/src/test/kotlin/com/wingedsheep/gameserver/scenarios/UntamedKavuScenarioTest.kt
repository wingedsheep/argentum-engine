package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Untamed Kavu with kicker.
 *
 * Card reference:
 * - Untamed Kavu ({1}{G}): Creature — Kavu 2/2
 *   Kicker {3}
 *   Vigilance, trample
 *   If this creature was kicked, it enters with three +1/+1 counters on it.
 */
class UntamedKavuScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Untamed Kavu kicker") {

            test("unkicked enters as 2/2 with no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Untamed Kavu")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Untamed Kavu")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Untamed Kavu should be on battlefield") {
                    game.isOnBattlefield("Untamed Kavu") shouldBe true
                }

                val kavuId = game.findPermanent("Untamed Kavu")!!
                withClue("Unkicked Untamed Kavu should have no +1/+1 counters") {
                    game.getCounters(kavuId) shouldBe 0
                }
            }

            test("kicked enters with three +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Untamed Kavu")
                    .withLandsOnBattlefield(1, "Forest", 5) // {1}{G} + {3} kicker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Untamed Kavu"
                }!!

                val castResult = game.execute(
                    CastSpell(playerId, cardId, wasKicked = true)
                )
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Verify it's on the stack with wasKicked
                val stackTopId = game.state.getTopOfStack()
                withClue("Spell should be on stack") {
                    (stackTopId != null) shouldBe true
                }

                // Resolve the spell (permanent enters battlefield)
                game.resolveStack()

                withClue("Untamed Kavu should be on battlefield") {
                    game.isOnBattlefield("Untamed Kavu") shouldBe true
                }

                val kavuId = game.findPermanent("Untamed Kavu")!!

                // Check WasKickedComponent is present
                withClue("Permanent should have WasKickedComponent") {
                    (game.state.getEntity(kavuId)?.has<WasKickedComponent>() == true) shouldBe true
                }

                withClue("Kicked Untamed Kavu should have three +1/+1 counters") {
                    game.getCounters(kavuId) shouldBe 3
                }
            }
        }
    }
}
