package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LoxodonPunisher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Loxodon Punisher.
 *
 * Loxodon Punisher ({3}{W}): Creature — Elephant Soldier 2/2
 *   "This creature gets +2/+2 for each Equipment attached to it."
 *
 * The bonus is doubled per Equipment and applies to *both* stats, and it counts Equipment only —
 * so this pins the multiplier, the toughness half (which the existing power-only cards on this
 * shape never exercise), and that the Punisher is a plain 2/2 while unequipped.
 */
class LoxodonPunisherScenarioTest : FunSpec({

    fun GameTestDriver.putEquipmentAttached(
        playerId: EntityId,
        cardName: String,
        targetCreatureId: EntityId
    ): EntityId {
        val equipmentId = putPermanentOnBattlefield(playerId, cardName)
        var newState = state.updateEntity(equipmentId) { c -> c.with(AttachedToComponent(targetCreatureId)) }
        val existing = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existing + equipmentId))
        }
        replaceState(newState)
        return equipmentId
    }

    val stateProjector = StateProjector()

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoxodonPunisher, Bonesplitter))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    test("unequipped, it is a plain 2/2") {
        val (driver, player) = setUp()
        val punisher = driver.putCreatureOnBattlefield(player, "Loxodon Punisher")

        val projected = stateProjector.project(driver.state)
        projected.getPower(punisher) shouldBe 2
        projected.getToughness(punisher) shouldBe 2
    }

    test("one Equipment is +2/+2, on top of that Equipment's own bonus") {
        val (driver, player) = setUp()
        val punisher = driver.putCreatureOnBattlefield(player, "Loxodon Punisher")
        driver.putEquipmentAttached(player, "Bonesplitter", punisher)

        val projected = stateProjector.project(driver.state)
        projected.getPower(punisher) shouldBe 6     // 2 base + 2 (Punisher) + 2 (Bonesplitter)
        projected.getToughness(punisher) shouldBe 4 // 2 base + 2 (Punisher); Bonesplitter is +2/+0
    }

    test("two Equipment double the bonus") {
        val (driver, player) = setUp()
        val punisher = driver.putCreatureOnBattlefield(player, "Loxodon Punisher")
        driver.putEquipmentAttached(player, "Bonesplitter", punisher)
        driver.putEquipmentAttached(player, "Bonesplitter", punisher)

        val projected = stateProjector.project(driver.state)
        projected.getPower(punisher) shouldBe 10    // 2 + 4 (Punisher) + 4 (two Bonesplitters)
        projected.getToughness(punisher) shouldBe 6 // 2 + 4 (Punisher)
    }

    test("Equipment on another creature does not count") {
        val (driver, player) = setUp()
        val punisher = driver.putCreatureOnBattlefield(player, "Loxodon Punisher")
        val other = driver.putCreatureOnBattlefield(player, "Loxodon Punisher")
        driver.putEquipmentAttached(player, "Bonesplitter", other)

        val projected = stateProjector.project(driver.state)
        projected.getPower(punisher) shouldBe 2
        projected.getToughness(punisher) shouldBe 2
    }
})
