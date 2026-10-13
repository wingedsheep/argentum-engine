package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.big.cards.LostJitte
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Lost Jitte — {1} Legendary Artifact — Equipment
 *
 * "Whenever equipped creature deals combat damage, put a charge counter on Lost Jitte.
 *  Remove a charge counter from Lost Jitte: Choose one —
 *  • Untap target land.
 *  • Target creature can't block this turn.
 *  • Put a +1/+1 counter on equipped creature.
 *  Equip {1}"
 */
class LostJitteScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.putEquipmentAttached(
        playerId: EntityId,
        cardName: String,
        targetCreatureId: EntityId
    ): EntityId {
        val equipmentId = putPermanentOnBattlefield(playerId, cardName)
        var newState = state.updateEntity(equipmentId) { c ->
            c.with(AttachedToComponent(targetCreatureId))
        }
        val existing = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existing + equipmentId))
        }
        replaceState(newState)
        return equipmentId
    }

    fun GameTestDriver.chargeCounters(equipmentId: EntityId): Int =
        state.getEntity(equipmentId)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun GameTestDriver.setCharge(equipmentId: EntityId, count: Int) {
        replaceState(state.updateEntity(equipmentId) { c ->
            c.with(CountersComponent(mapOf(CounterType.CHARGE to count)))
        })
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LostJitte))
        return driver
    }

    test("equipped creature dealing combat damage to a player puts a charge counter on Lost Jitte") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val equipped = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(equipped)
        val jitte = driver.putEquipmentAttached(attacker, "Lost Jitte", equipped)
        driver.chargeCounters(jitte) shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(equipped), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Advance through the combat-damage step so the trigger fires and resolves.
        var safety = 0
        while (driver.chargeCounters(jitte) == 0 && safety++ < 20) driver.bothPass()

        driver.chargeCounters(jitte) shouldBe 1
    }

    test("remove a charge counter: put a +1/+1 counter on equipped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val equipped = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val jitte = driver.putEquipmentAttached(me, "Lost Jitte", equipped)
        driver.setCharge(jitte, 1)

        val abilityId = LostJitte.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = jitte, abilityId = abilityId)
        )
        (result.outcome is Outcome.Paused) shouldBe true


        // Choose "Put a +1/+1 counter on equipped creature" (mode 2)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, modeDecision.options.indexOfFirst { it.contains("+1/+1") }))
        driver.bothPass()

        // The charge counter was removed as the cost, and the creature grew.
        driver.chargeCounters(jitte) shouldBe 0
        val projected = projector.project(driver.state)
        projected.getPower(equipped) shouldBe 3
        projected.getToughness(equipped) shouldBe 3
    }

    test("remove a charge counter: untap target land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val equipped = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val jitte = driver.putEquipmentAttached(me, "Lost Jitte", equipped)
        driver.setCharge(jitte, 1)

        val land = driver.putPermanentOnBattlefield(me, "Plains")
        driver.replaceState(driver.state.updateEntity(land) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
        })
        driver.isTapped(land) shouldBe true

        val abilityId = LostJitte.activatedAbilities[0].id
        driver.submit(ActivateAbility(playerId = me, sourceId = jitte, abilityId = abilityId))

        // Choose "Untap target land" (mode 0)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, 0))

        // Per-mode target selection follows.
        driver.submitTargetSelection(me, listOf(land))
        driver.bothPass()

        driver.isTapped(land) shouldBe false
        driver.chargeCounters(jitte) shouldBe 0
    }

    test("ability can't be activated with no charge counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val equipped = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val jitte = driver.putEquipmentAttached(me, "Lost Jitte", equipped)
        driver.chargeCounters(jitte) shouldBe 0

        val abilityId = LostJitte.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = jitte, abilityId = abilityId)
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("Lost Jitte is legendary") {
        LostJitte.typeLine.isLegendary shouldBe true
        LostJitte.activatedAbilities.firstOrNull() shouldNotBe null
    }
})
