package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.WingmantleChaplain
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MarchOfTheMachines
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WingmantleChaplainScenarioTest : FunSpec({
    val wall = card("Chaplain Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
        keywords(Keyword.DEFENDER)
    }
    val bear = card("Chaplain Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val barricade = card("Chaplain Test Barricade") {
        manaCost = "{2}"
        typeLine = "Artifact"
        keywords(Keyword.DEFENDER)
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(WingmantleChaplain, MarchOfTheMachines, wall, bear, barricade))
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.birds() = getPermanents(player1).count { getCardName(it)?.contains("Bird") == true }

    fun GameTestDriver.cast(name: String, color: Color, mana: Int) {
        giveMana(player1, color, mana)
        castSpell(player1, putCardInHand(player1, name)).error shouldBe null
        bothPass()
        if (stackSize > 0) bothPass()
    }

    test("entering creates a Bird for each creature with defender you control, itself included") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, wall.name)
        d.putCreatureOnBattlefield(d.player1, bear.name)
        d.cast(WingmantleChaplain.name, Color.WHITE, 4)
        d.birds() shouldBe 2
    }

    test("another creature with defender entering creates a Bird; one without defender doesn't") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, WingmantleChaplain.name)
        d.cast(bear.name, Color.WHITE, 1)
        d.birds() shouldBe 0
        d.cast(wall.name, Color.WHITE, 1)
        d.birds() shouldBe 1
    }

    test("a noncreature artifact with defender is not a creature, so it makes no Bird") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, WingmantleChaplain.name)
        d.cast(barricade.name, Color.WHITE, 2)
        d.birds() shouldBe 0
    }

    test("an artifact with defender animated by March of the Machines enters as a creature and makes a Bird") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, WingmantleChaplain.name)
        d.putPermanentOnBattlefield(d.player1, MarchOfTheMachines.name)
        d.cast(barricade.name, Color.WHITE, 2)
        d.birds() shouldBe 1
    }
})
