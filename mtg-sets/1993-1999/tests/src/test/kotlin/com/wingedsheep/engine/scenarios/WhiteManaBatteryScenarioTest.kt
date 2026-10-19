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
import io.kotest.matchers.shouldBe

/**
 * White Mana Battery (LEG #299).
 *
 * "{T}, Remove any number of charge counters from this artifact: Add {W}, then add an additional
 * {W} for each charge counter removed this way."
 *
 * Unlike the storage lands, the battery always makes one {W} on top of the counters removed, so
 * the amount is X + 1 — and removing no counters at all still pays out a single {W}.
 */
class WhiteManaBatteryScenarioTest : ScenarioTestBase() {

    private val chargeAbilityId =
        cardRegistry.getCard("White Mana Battery")!!.activatedAbilities.first().id
    private val manaAbilityId =
        cardRegistry.getCard("White Mana Battery")!!.activatedAbilities.last().id

    private fun chargeCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    private fun whiteMana(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.white ?: 0

    private fun batteryWithCounters(counters: Int): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "White Mana Battery")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val battery = game.findPermanent("White Mana Battery")!!
        if (counters > 0) {
            game.state = game.state.updateEntity(battery) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withAdded(CounterType.CHARGE, counters))
            }
        }
        return game to battery
    }

    init {
        context("White Mana Battery") {

            test("{2}, {T} puts a charge counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "White Mana Battery")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val battery = game.findPermanent("White Mana Battery")!!

                val activation = game.execute(ActivateAbility(game.player1Id, battery, chargeAbilityId))
                withClue("charging should succeed: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                chargeCounters(game, battery) shouldBe 1
            }

            test("removing two charge counters adds three white mana") {
                val (game, battery) = batteryWithCounters(3)

                val activation = game.execute(
                    ActivateAbility(game.player1Id, battery, manaAbilityId, xValue = 2)
                )
                withClue("activating for X=2 should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("one {W} plus one more per counter removed") { whiteMana(game) shouldBe 3 }
                withClue("only the counters spent are gone") { chargeCounters(game, battery) shouldBe 1 }
            }

            test("removing no counters still adds one white mana") {
                val (game, battery) = batteryWithCounters(0)

                val activation = game.execute(
                    ActivateAbility(game.player1Id, battery, manaAbilityId, xValue = 0)
                )
                withClue("X=0 is a legal activation: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                whiteMana(game) shouldBe 1
            }
        }
    }
}
