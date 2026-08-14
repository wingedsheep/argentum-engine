package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.GolemSkinGauntlets
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Golem-Skin Gauntlets.
 *
 * Golem-Skin Gauntlets ({1}): Artifact — Equipment
 *   "Equipped creature gets +1/+0 for each Equipment attached to it.
 *    Equip {2}"
 *
 * Exercises the Equipment-only attachment count read through the source's attachment link —
 * `DynamicAmounts.attachmentsOnEnchantedCreature(AttachmentKind.EQUIPMENT)`. The three things that
 * distinguish it from every existing attachment-count card and are therefore worth pinning:
 *
 *  1. The count is taken on the *equipped creature*, not on the Gauntlets, so a lone pair counts
 *     itself and gives +1/+0.
 *  2. Two pairs of Gauntlets are two separate static abilities that each see both, so the bonus is
 *     +4/+0 in total rather than +2/+0.
 *  3. `AttachmentKind.EQUIPMENT` excludes Auras attached to the same creature.
 */
class GolemSkinGauntletsScenarioTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Gauntlet Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

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

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Bear, GolemSkinGauntlets, Bonesplitter))
        return driver
    }

    val stateProjector = StateProjector()

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    test("a lone pair of Gauntlets counts itself for +1/+0") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)

        val projected = stateProjector.project(driver.state)
        projected.getPower(bear) shouldBe 3     // 2 + 1 (the Gauntlets themselves)
        projected.getToughness(bear) shouldBe 2 // never modified
    }

    test("a second Equipment on the same creature raises the bonus") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)
        driver.putEquipmentAttached(player, "Bonesplitter", bear)

        val projected = stateProjector.project(driver.state)
        // 2 base + 2 (Gauntlets: two Equipment attached) + 2 (Bonesplitter's own +2/+0)
        projected.getPower(bear) shouldBe 6
        projected.getToughness(bear) shouldBe 2
    }

    test("two pairs of Gauntlets each see both, for +4/+0") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)
        driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)

        val projected = stateProjector.project(driver.state)
        projected.getPower(bear) shouldBe 6     // 2 + 2 + 2
        projected.getToughness(bear) shouldBe 2
    }

    test("the bonus recomputes when Equipment falls off") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        val gauntlets = driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)
        val bonesplitter = driver.putEquipmentAttached(player, "Bonesplitter", bear)

        stateProjector.project(driver.state).getPower(bear) shouldBe 6

        // Bonesplitter leaves the battlefield: the Gauntlets now see only themselves.
        var newState = driver.state.updateEntity(bear) { c ->
            c.with(AttachmentsComponent(listOf(gauntlets)))
        }
        newState = newState.removeEntity(bonesplitter)
        driver.replaceState(newState)

        stateProjector.project(driver.state).getPower(bear) shouldBe 3 // 2 + 1
    }

    test("an unequipped creature gets no bonus at all") {
        val (driver, player) = setUp()
        val bear = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        val other = driver.putCreatureOnBattlefield(player, "Gauntlet Bear")
        driver.putEquipmentAttached(player, "Golem-Skin Gauntlets", bear)

        val projected = stateProjector.project(driver.state)
        projected.getPower(other) shouldBe 2
    }
})
