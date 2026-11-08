package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.RazorgrassInvoker
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Razorgrass Invoker (J25 #22) — {3}{G} Creature — Elf Scout 4/3.
 *
 *   "{8}: This creature and up to one other target creature each get +3/+3 until end of turn."
 *
 * The target is optional and excludes the Invoker. With no target the Invoker is still pumped; with
 * a target that becomes illegal before resolution the whole ability fizzles and the Invoker gets
 * nothing (ruling).
 */
class RazorgrassInvokerScenarioTest : FunSpec({

    val abilityId = RazorgrassInvoker.activatedAbilities.first().id

    data class Board(val driver: GameTestDriver, val me: EntityId, val invoker: EntityId, val bear: EntityId)

    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RazorgrassInvoker))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val invoker = driver.putCreatureOnBattlefield(me, "Razorgrass Invoker")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.giveColorlessMana(me, 8)
        return Board(driver, me, invoker, bear)
    }

    test("with no target, the Invoker alone gets +3/+3") {
        val (driver, me, invoker, bear) = setup()

        driver.submitSuccess(ActivateAbility(me, invoker, abilityId, emptyList()))
        driver.bothPass()

        driver.state.projectedState.getPower(invoker) shouldBe 7
        driver.state.projectedState.getToughness(invoker) shouldBe 6
        driver.state.projectedState.getPower(bear) shouldBe 2
    }

    test("with another target, both creatures get +3/+3") {
        val (driver, me, invoker, bear) = setup()

        driver.submitSuccess(ActivateAbility(me, invoker, abilityId, listOf(ChosenTarget.Permanent(bear))))
        driver.bothPass()

        driver.state.projectedState.getPower(invoker) shouldBe 7
        driver.state.projectedState.getToughness(invoker) shouldBe 6
        driver.state.projectedState.getPower(bear) shouldBe 5
        driver.state.projectedState.getToughness(bear) shouldBe 5
    }

    test("the Invoker can't target itself") {
        val (driver, me, invoker, _) = setup()

        driver.submitExpectFailure(ActivateAbility(me, invoker, abilityId, listOf(ChosenTarget.Permanent(invoker))))
    }

    test("if the chosen target is gone on resolution, the ability fizzles and the Invoker gets nothing") {
        val (driver, me, invoker, bear) = setup()

        driver.submitSuccess(ActivateAbility(me, invoker, abilityId, listOf(ChosenTarget.Permanent(bear))))
        driver.moveToGraveyard(bear)
        driver.bothPass()

        driver.state.stack.size shouldBe 0
        driver.state.projectedState.getPower(invoker) shouldBe 4
        driver.state.projectedState.getToughness(invoker) shouldBe 3
    }
})
