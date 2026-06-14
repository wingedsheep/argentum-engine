package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thunder Lasso — {2}{W} Artifact — Equipment
 *
 * "When this Equipment enters, attach it to target creature you control.
 *  Equipped creature gets +1/+1.
 *  Whenever equipped creature attacks, tap target creature defending player controls.
 *  Equip {2}"
 *
 * Verifies the +1/+1 static buff through the attachment, and that the attack trigger
 * taps a creature the defending player controls.
 */
class ThunderLassoScenarioTest : FunSpec({

    val stateProjector = StateProjector()

    /**
     * Put the equipment on the battlefield and attach it to a creature directly
     * (mirrors ForebearsBladeTriggerTest), so static + attack-trigger behaviour can be
     * exercised without re-testing the ETB attach.
     */
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

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("equipped creature gets +1/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // base 2/2
        driver.putEquipmentAttached(me, "Thunder Lasso", creature)

        val projected = stateProjector.project(driver.state)
        projected.getPower(creature) shouldBe 3   // 2 + 1
        projected.getToughness(creature) shouldBe 3 // 2 + 1
    }

    test("when equipped creature attacks, tap target creature defending player controls") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val equipped = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(equipped)
        driver.putEquipmentAttached(attacker, "Thunder Lasso", equipped)

        // Defender controls a creature to be tapped.
        val victim = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.isTapped(victim) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(equipped), defender)

        // Attack trigger asks for a target — choose the defender's creature.
        driver.submitTargetSelection(attacker, listOf(victim))

        // Resolve the triggered ability.
        driver.bothPass()

        driver.isTapped(victim) shouldBe true
    }
})
