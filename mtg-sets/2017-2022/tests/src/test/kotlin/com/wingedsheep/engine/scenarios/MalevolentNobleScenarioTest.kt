package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Malevolent Noble (ELD #95) — "{2}, Sacrifice an artifact or another creature: Put a +1/+1
 * counter on this creature."
 *
 * The sacrifice filter is a heterogeneous union (Artifact | Creature-that-isn't-the-source), so
 * the tests pin each branch and that the Noble itself is never a candidate.
 */
class MalevolentNobleScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.nobleActivation() = getLegalActions(1).singleOrNull {
        (it.action as? ActivateAbility)?.sourceId == findPermanent("Malevolent Noble")
    }

    private fun TestGame.activateSacrificing(fodder: EntityId) {
        val option = nobleActivation().shouldNotBeNull()
        val result = execute(
            (option.action as ActivateAbility).copy(
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Malevolent Noble") {

            test("offers artifacts and other creatures, but not itself, as sacrifice candidates") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Malevolent Noble")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val info = game.nobleActivation().shouldNotBeNull().additionalCostInfo.shouldNotBeNull()
                info.validSacrificeTargets shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Grizzly Bears")!!,
                    game.findPermanent("Food")!!,
                )
            }

            test("sacrificing another creature puts a +1/+1 counter on the Noble") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Malevolent Noble")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val noble = game.findPermanent("Malevolent Noble")!!
                game.activateSacrificing(game.findPermanent("Grizzly Bears")!!)

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.plusOneCounters(noble) shouldBe 1
                game.state.projectedState.getPower(noble) shouldBe 3
            }

            test("sacrificing a noncreature artifact puts a +1/+1 counter on the Noble") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Malevolent Noble")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val noble = game.findPermanent("Malevolent Noble")!!
                game.activateSacrificing(game.findPermanent("Food")!!)

                game.isOnBattlefield("Food") shouldBe false
                game.plusOneCounters(noble) shouldBe 1
            }

            test("with no artifact or other creature, the ability cannot be activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Malevolent Noble")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the Noble must not be able to sacrifice itself") {
                    game.nobleActivation()?.isAffordable shouldNotBe true
                }
            }
        }
    }
}
