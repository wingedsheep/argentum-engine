package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.DaghatarTheAdamant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Daghatar the Adamant — {1}{B/G}{B/G}: Move a +1/+1 counter from target creature onto a second
 * target creature.
 *
 * Pins the rulings: the two targets must differ (either may be Daghatar, or any player's creature),
 * and the counter moves only if both targets are still legal on resolution.
 */
class DaghatarTheAdamantScenarioTest : FunSpec({

    val moveAbility = DaghatarTheAdamant.activatedAbilities[0].id

    fun GameTestDriver.addCounters(entityId: EntityId, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        })
    }

    fun GameTestDriver.plusOnes(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.payForMove(player: EntityId) {
        giveColorlessMana(player, 1)
        giveMana(player, Color.BLACK, 1)
        giveMana(player, Color.GREEN, 1)
    }

    fun GameTestDriver.activateMove(player: EntityId, daghatar: EntityId, from: EntityId, onto: EntityId) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = daghatar,
                abilityId = moveAbility,
                targets = listOf(entityIdToChosenTarget(state, from), entityIdToChosenTarget(state, onto))
            )
        )

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DaghatarTheAdamant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val daghatar = driver.putCreatureOnBattlefield(active, "Daghatar the Adamant")
        driver.addCounters(daghatar, 4)
        return Triple(driver, active, daghatar)
    }

    test("moves a counter from an opponent's creature onto Daghatar") {
        val (driver, active, daghatar) = setup()
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(active), "Grizzly Bears")
        driver.addCounters(theirs, 2)
        driver.payForMove(active)

        driver.activateMove(active, daghatar, theirs, daghatar).error shouldBe null
        driver.bothPass()

        driver.plusOnes(theirs) shouldBe 1
        driver.plusOnes(daghatar) shouldBe 5
    }

    test("the two targets must be different creatures") {
        val (driver, active, daghatar) = setup()
        driver.payForMove(active)

        driver.activateMove(active, daghatar, daghatar, daghatar).error shouldNotBe null
    }

    test("no counter moves when the second target is gone on resolution") {
        val (driver, active, daghatar) = setup()
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.payForMove(active)

        driver.activateMove(active, daghatar, daghatar, bears).error shouldBe null
        driver.moveToGraveyard(bears)
        driver.bothPass()

        driver.plusOnes(daghatar) shouldBe 4
    }
})
