package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Oath of Teferi's ExtraLoyaltyActivation static ability.
 *
 * Tests cover:
 * - Activating a planeswalker loyalty ability twice when Oath of Teferi is on the battlefield
 * - Normal limit of one activation per planeswalker per turn without Oath of Teferi
 * - Third activation is still blocked even with Oath of Teferi
 */
class OathOfTeferiScenarioTest : ScenarioTestBase() {

    init {
        context("Oath of Teferi extra loyalty activation") {

            test("can activate planeswalker loyalty ability twice with Oath of Teferi") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, Scion of Urza")
                    .withCardOnBattlefield(1, "Oath of Teferi")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val karnId = game.findPermanent("Karn, Scion of Urza")!!

                // Set Karn's loyalty to 5
                game.state = game.state.updateEntity(karnId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 5))
                }

                val cardDef = cardRegistry.getCard("Karn, Scion of Urza")!!
                val minusTwoAbility = cardDef.script.activatedAbilities[2] // -2: create Construct token

                // First activation: -2
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )
                game.resolveStack()

                // After first activation, loyalty should be 3 (5 - 2)
                val countersAfterFirst = game.state.getEntity(karnId)?.get<CountersComponent>()
                withClue("Karn should have 3 loyalty after first -2 activation") {
                    countersAfterFirst?.getCount(CounterType.LOYALTY) shouldBe 3
                }

                // Second activation: -2 (should succeed with Oath of Teferi)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )
                game.resolveStack()

                // After second activation, loyalty should be 1 (3 - 2)
                val countersAfterSecond = game.state.getEntity(karnId)?.get<CountersComponent>()
                withClue("Karn should have 1 loyalty after second -2 activation") {
                    countersAfterSecond?.getCount(CounterType.LOYALTY) shouldBe 1
                }

                // Verify we tracked 2 activations
                val tracker = game.state.getEntity(karnId)?.get<AbilityActivatedThisTurnComponent>()
                withClue("Karn should have 2 loyalty activations tracked") {
                    tracker?.loyaltyActivationCount shouldBe 2
                }
            }

            test("cannot activate planeswalker loyalty ability three times even with Oath of Teferi") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, Scion of Urza")
                    .withCardOnBattlefield(1, "Oath of Teferi")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val karnId = game.findPermanent("Karn, Scion of Urza")!!

                // Set Karn's loyalty to 10 (enough for three -2 activations)
                game.state = game.state.updateEntity(karnId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 10))
                }

                val cardDef = cardRegistry.getCard("Karn, Scion of Urza")!!
                val minusTwoAbility = cardDef.script.activatedAbilities[2] // -2

                // First activation
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )
                game.resolveStack()

                // Second activation (allowed by Oath of Teferi)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )
                game.resolveStack()

                // Third activation should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )

                withClue("Third loyalty activation should be rejected") {
                    result.error shouldBe "Loyalty abilities can only be activated 2 times per planeswalker each turn"
                }
            }

            test("without Oath of Teferi, only one loyalty activation allowed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, Scion of Urza")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val karnId = game.findPermanent("Karn, Scion of Urza")!!

                // Set Karn's loyalty to 5
                game.state = game.state.updateEntity(karnId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 5))
                }

                val cardDef = cardRegistry.getCard("Karn, Scion of Urza")!!
                val minusTwoAbility = cardDef.script.activatedAbilities[2] // -2

                // First activation
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )
                game.resolveStack()

                // Second activation should fail without Oath of Teferi
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = karnId,
                        abilityId = minusTwoAbility.id
                    )
                )

                withClue("Second loyalty activation should be rejected without Oath of Teferi") {
                    result.error shouldBe "Only one loyalty ability can be activated per planeswalker each turn"
                }
            }
        }
    }
}
