package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Mage-Ring Network (ORI #249).
 *
 * "{1}, {T}: Put a storage counter on this land.
 * {T}, Remove any number of storage counters from this land: Add {C} for each storage counter
 * removed this way."
 *
 * The first card whose *colorless* mana amount is the X of a variable counter-removal cost (the
 * storage lands before it all add coloured mana), so the X → {C} wiring is pinned here.
 */
class MageRingNetworkScenarioTest : ScenarioTestBase() {

    private val abilities = cardRegistry.getCard("Mage-Ring Network")!!.activatedAbilities
    private val chargeAbilityId = abilities[1].id
    private val cashOutAbilityId = abilities[2].id

    private fun storageCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STORAGE) ?: 0

    private fun colorlessInPool(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless ?: 0

    init {
        context("Mage-Ring Network") {

            test("{1}, {T} puts a storage counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mage-Ring Network")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val network = game.findPermanent("Mage-Ring Network")!!
                val activation = game.execute(
                    ActivateAbility(game.player1Id, network, chargeAbilityId)
                )
                withClue("Charging should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                storageCounters(game, network) shouldBe 1
            }

            test("removing storage counters adds that much colorless mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mage-Ring Network")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val network = game.findPermanent("Mage-Ring Network")!!
                game.state = game.state.updateEntity(network) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.STORAGE, 3))
                }

                val action = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.abilityId == cashOutAbilityId
                }
                withClue("the player picks X, capped by the counters on the land") {
                    action.hasXCost.shouldBeTrue()
                    action.maxAffordableX shouldBe 3
                }

                val activation = game.execute(
                    ActivateAbility(game.player1Id, network, cashOutAbilityId, xValue = 2)
                )
                withClue("Activating for X=2 should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("two storage counters paid for two colorless mana") {
                    colorlessInPool(game) shouldBe 2
                }
                withClue("only the counters spent are gone") {
                    storageCounters(game, network) shouldBe 1
                }
            }
        }
    }
}
