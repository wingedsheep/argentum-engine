package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * The group rule behind `CostAtom.TapPermanents.sharedCreatureType` — which candidates can be part
 * of a legal payment, and whether a chosen set shares a creature type.
 */
class SharedCreatureTypeTapCostTest : FunSpec({

    fun creature(name: String, typeLine: String, changeling: Boolean = false) = card(name) {
        manaCost = "{1}"
        this.typeLine = typeLine
        power = 1
        toughness = 1
        if (changeling) keywords(Keyword.CHANGELING)
    }

    val elf = creature("Test Elf", "Creature — Elf")
    val goblin = creature("Test Goblin", "Creature — Goblin")
    val goblinWarrior = creature("Test Goblin Warrior", "Creature — Goblin Warrior")
    val shapeshifter = creature("Test Shapeshifter", "Creature — Shapeshifter", changeling = true)

    val pair = CostAtom.TapPermanents(count = 2, filter = GameObjectFilter.Creature, sharedCreatureType = true)

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(elf, goblin, goblinWarrior, shapeshifter))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("candidates with no type partner are dropped; the axis off is a no-op") {
        val d = driver()
        val me = d.activePlayer!!
        val e = d.putCreatureOnBattlefield(me, "Test Elf")
        val g = d.putCreatureOnBattlefield(me, "Test Goblin")
        val gw = d.putCreatureOnBattlefield(me, "Test Goblin Warrior")

        SharedCreatureTypeTapCost.eligible(d.state, pair, listOf(e, g, gw)) shouldContainExactlyInAnyOrder listOf(g, gw)
        SharedCreatureTypeTapCost.eligible(d.state, pair.copy(sharedCreatureType = false), listOf(e, g, gw)) shouldBe listOf(e, g, gw)
    }

    test("the largest group leads, so taking the first count candidates is a legal payment") {
        val d = driver()
        val me = d.activePlayer!!
        val e1 = d.putCreatureOnBattlefield(me, "Test Elf")
        val g = d.putCreatureOnBattlefield(me, "Test Goblin")
        val e2 = d.putCreatureOnBattlefield(me, "Test Elf")
        val gw = d.putCreatureOnBattlefield(me, "Test Goblin Warrior")
        val e3 = d.putCreatureOnBattlefield(me, "Test Elf")

        val pool = SharedCreatureTypeTapCost.eligible(d.state, pair, listOf(g, e1, gw, e2, e3))
        SharedCreatureTypeTapCost.satisfiedBy(d.state, pair, pool.take(2)) shouldBe true
    }

    test("a changeling shares a creature type with anything") {
        val d = driver()
        val me = d.activePlayer!!
        val e = d.putCreatureOnBattlefield(me, "Test Elf")
        val g = d.putCreatureOnBattlefield(me, "Test Goblin")
        val s = d.putCreatureOnBattlefield(me, "Test Shapeshifter")

        SharedCreatureTypeTapCost.eligible(d.state, pair, listOf(e, g, s)) shouldContainExactlyInAnyOrder listOf(e, g, s)
        SharedCreatureTypeTapCost.satisfiedBy(d.state, pair, listOf(e, s)) shouldBe true
        SharedCreatureTypeTapCost.satisfiedBy(d.state, pair, listOf(e, g)) shouldBe false
    }
})
