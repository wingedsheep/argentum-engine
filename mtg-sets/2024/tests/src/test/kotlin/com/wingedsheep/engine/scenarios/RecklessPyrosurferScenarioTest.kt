package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RecklessPyrosurfer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reckless Pyrosurfer — Haste. Landfall — this creature gains battle cry until end of turn.
 * Each landfall grants a separate battle cry instance, and each instance triggers separately.
 */
class RecklessPyrosurferScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RecklessPyrosurfer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, p1, p2)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("no land: attacking gives no battle cry pump") {
        val (d, p1, p2) = setup()
        val pyro = d.putCreatureOnBattlefield(p1, "Reckless Pyrosurfer")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(pyro, bear), p2).error shouldBe null
        d.state.stack.size shouldBe 0
        d.state.projectedState.getPower(bear) shouldBe 2
    }

    test("one landfall: haste attack pumps each other attacker +1/+0, not itself") {
        val (d, p1, p2) = setup()
        val pyro = d.putCreatureOnBattlefield(p1, "Reckless Pyrosurfer")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        val land = d.putCardInHand(p1, "Mountain")
        d.playLand(p1, land).error shouldBe null
        d.resolveStack()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(pyro, bear), p2).error shouldBe null
        d.resolveStack()

        d.state.projectedState.getPower(bear) shouldBe 3
        d.state.projectedState.getToughness(bear) shouldBe 2
        d.state.projectedState.getPower(pyro) shouldBe 2
    }

    test("two landfalls give two battle cry instances that trigger separately") {
        val (d, p1, p2) = setup()
        val pyro = d.putCreatureOnBattlefield(p1, "Reckless Pyrosurfer")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        d.replaceState(d.state.updateEntity(p1) { it.with(LandDropsComponent(remaining = 2, maxPerTurn = 2)) })

        d.playLand(p1, d.putCardInHand(p1, "Mountain")).error shouldBe null
        d.resolveStack()
        d.playLand(p1, d.putCardInHand(p1, "Mountain")).error shouldBe null
        d.resolveStack()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(pyro, bear), p2).error shouldBe null
        d.state.stack.size shouldBe 2
        d.resolveStack()

        d.state.projectedState.getPower(bear) shouldBe 4
        d.state.projectedState.getPower(pyro) shouldBe 2
    }

    test("battle cry granted by landfall ends at end of turn") {
        val (d, p1, p2) = setup()
        val pyro = d.putCreatureOnBattlefield(p1, "Reckless Pyrosurfer")
        d.playLand(p1, d.putCardInHand(p1, "Mountain")).error shouldBe null
        d.resolveStack()
        d.state.grantedTriggeredAbilities.count { it.entityId == pyro } shouldBe 1

        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.state.activePlayerId shouldBe p2
        d.state.grantedTriggeredAbilities.count { it.entityId == pyro } shouldBe 0
    }
})
