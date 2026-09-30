package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.NestingGrounds
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nesting Grounds (Land)
 *
 * {T}: Add {C}.
 * {1}, {T}: Move a counter from target permanent you control onto a second target permanent.
 * Activate only as a sorcery.
 *
 * Exactly one counter moves; the player picks which kind only when the first target carries more
 * than one.
 */
class NestingGroundsScenarioTest : FunSpec({

    val moveAbility = NestingGrounds.activatedAbilities[1].id

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NestingGrounds))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val grounds = driver.putLandOnBattlefield(active, "Nesting Grounds")
        return Triple(driver, active, grounds)
    }

    fun GameTestDriver.addCounters(entityId: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    fun GameTestDriver.count(entityId: EntityId, type: CounterType): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.activateMove(player: EntityId, grounds: EntityId, from: EntityId, onto: EntityId) =
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = grounds,
                abilityId = moveAbility,
                targets = listOf(entityIdToChosenTarget(state, from), entityIdToChosenTarget(state, onto))
            )
        )

    test("a single kind moves exactly one counter without asking") {
        val (driver, active, grounds) = setup()
        val from = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val onto = driver.putCreatureOnBattlefield(driver.getOpponent(active), "Grizzly Bears")
        driver.addCounters(from, CounterType.PLUS_ONE_PLUS_ONE, 3)
        driver.giveColorlessMana(active, 1)

        driver.activateMove(active, grounds, from, onto).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.count(from, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        driver.count(onto, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("with several kinds the player picks which one moves, and only one moves") {
        val (driver, active, grounds) = setup()
        val from = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val onto = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.addCounters(from, CounterType.PLUS_ONE_PLUS_ONE, 2)
        driver.addCounters(from, CounterType.CHARGE, 2)
        driver.giveColorlessMana(active, 1)

        driver.activateMove(active, grounds, from, onto)
        driver.bothPass()

        // First kind: optional (the other kind can still cover the one counter), capped at one.
        val first = driver.pendingDecision
        first.shouldBeInstanceOf<ChooseNumberDecision>()
        first.minValue shouldBe 0
        first.maxValue shouldBe 1
        // Declining it forces the other kind — the move itself can't be skipped.
        driver.submitDecision(active, NumberChosenResponse(first.id, 0))

        driver.pendingDecision shouldBe null
        val movedPlus = driver.count(onto, CounterType.PLUS_ONE_PLUS_ONE)
        val movedCharge = driver.count(onto, CounterType.CHARGE)
        (movedPlus + movedCharge) shouldBe 1
        driver.count(from, CounterType.PLUS_ONE_PLUS_ONE) + driver.count(from, CounterType.CHARGE) shouldBe 3
    }

    test("choosing the first kind stops the walk after one counter") {
        val (driver, active, grounds) = setup()
        val from = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val onto = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.addCounters(from, CounterType.PLUS_ONE_PLUS_ONE, 2)
        driver.addCounters(from, CounterType.CHARGE, 2)
        driver.giveColorlessMana(active, 1)

        driver.activateMove(active, grounds, from, onto)
        driver.bothPass()

        val first = driver.pendingDecision
        first.shouldBeInstanceOf<ChooseNumberDecision>()
        driver.submitDecision(active, NumberChosenResponse(first.id, 1))

        driver.pendingDecision shouldBe null
        (driver.count(onto, CounterType.PLUS_ONE_PLUS_ONE) + driver.count(onto, CounterType.CHARGE)) shouldBe 1
    }

    test("Nesting Grounds itself may be the second target, but not the same permanent twice") {
        val (driver, active, grounds) = setup()
        val from = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.addCounters(from, CounterType.CHARGE, 1)
        driver.giveColorlessMana(active, 1)

        val action = driver.legalActions(active).first {
            (it.action as? ActivateAbility)?.abilityId == moveAbility
        }
        val secondSlot = action.targetRequirements!![1]
        secondSlot.validTargets shouldContain grounds
        secondSlot.mustDifferFromEarlier shouldBe true

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = active,
                sourceId = grounds,
                abilityId = moveAbility,
                targets = listOf(entityIdToChosenTarget(driver.state, from), entityIdToChosenTarget(driver.state, from))
            )
        ).error shouldNotBe null

        driver.activateMove(active, grounds, from, grounds).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.count(grounds, CounterType.CHARGE) shouldBe 1
        driver.count(from, CounterType.CHARGE) shouldBe 0
    }

    test("the first target must be a permanent you control") {
        val (driver, active, grounds) = setup()
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(active), "Grizzly Bears")
        driver.giveColorlessMana(active, 1)

        val action = driver.legalActions(active).first {
            (it.action as? ActivateAbility)?.abilityId == moveAbility
        }
        action.targetRequirements!![0].validTargets shouldNotContain theirs
    }
})
