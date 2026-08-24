package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Deep Water (DRK #23).
 *
 * {U}{U} Enchantment
 * "{U}: Until end of turn, if you tap a land you control for mana, it produces {U} instead of any
 *  other type."
 *
 * Two things distinguish this from Pulse of Llanowar, and both are tested: the colour is fixed
 * rather than chosen, and the rule is *granted* for a turn rather than printed — so it has to reach
 * the mana path through `grantedStaticAbilities`, which the layer projector doesn't carry.
 */
class DeepWaterScenarioTest : ScenarioTestBase() {

    init {
        fun deepWaterAbilityId() =
            cardRegistry.getCard("Deep Water")!!.script.activatedAbilities[0].id

        fun ScenarioTestBase.TestGame.manaPool(playerNumber: Int): ManaPoolComponent {
            val playerId = if (playerNumber == 1) player1Id else player2Id
            return state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        }

        context("Deep Water") {

            test("after activating, a Mountain taps for blue instead of red") {
                val game = scenario()
                    .withPlayers("Merfolk", "Opponent")
                    .withCardOnBattlefield(1, "Deep Water")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deepWater = game.findPermanent("Deep Water")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = deepWater,
                        abilityId = deepWaterAbilityId()
                    )
                ).error shouldBe null
                game.resolveStack()

                // Tap the Mountain for mana; the replacement should make it blue.
                val mountain = game.findPermanent("Mountain")!!
                val mountainMana = cardRegistry.getCard("Mountain")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = mountain, abilityId = mountainMana)
                ).error shouldBe null

                val pool = game.manaPool(1)
                withClue("the Mountain produced {U}, not {R}") {
                    pool.getAmount(Color.BLUE) shouldBe 1
                    pool.getAmount(Color.RED) shouldBe 0
                }
            }

            test("without activating, a Mountain still taps for red") {
                // The rule is durational and opt-in — the enchantment sitting on the battlefield
                // does nothing on its own.
                val game = scenario()
                    .withPlayers("Merfolk", "Opponent")
                    .withCardOnBattlefield(1, "Deep Water")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findPermanent("Mountain")!!
                val mountainMana = cardRegistry.getCard("Mountain")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = mountain, abilityId = mountainMana)
                ).error shouldBe null

                val pool = game.manaPool(1)
                withClue("no grant is active, so the Mountain is a Mountain") {
                    pool.getAmount(Color.RED) shouldBe 1
                    pool.getAmount(Color.BLUE) shouldBe 0
                }
            }
        }
    }
}
