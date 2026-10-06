package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AnimateWall
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AnimateWallScenarioTest : FunSpec({
    val testWall = card("Animate Wall Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
        keywords(Keyword.DEFENDER)
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AnimateWall)
        driver.registerCard(testWall)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("only the enchanted Wall can attack despite defender, even under an opponent's Aura") {
        val driver = setup()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val enchanted = driver.putCreatureOnBattlefield(active, testWall.name)
        val other = driver.putCreatureOnBattlefield(active, testWall.name)
        driver.removeSummoningSickness(enchanted)
        driver.removeSummoningSickness(other)
        val aura = driver.putPermanentOnBattlefield(opponent, AnimateWall.name)
        driver.addComponent(aura, AttachedToComponent(enchanted))
        driver.addComponent(enchanted, AttachmentsComponent(listOf(aura)))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(other), opponent).error shouldNotBe null
        driver.declareAttackers(active, listOf(enchanted), opponent).error shouldBe null
    }

    test("an unattached Animate Wall lifts defender from nothing") {
        val driver = setup()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val wall = driver.putCreatureOnBattlefield(active, testWall.name)
        driver.removeSummoningSickness(wall)
        driver.putPermanentOnBattlefield(active, AnimateWall.name)

        // A non-defender creature keeps the declare-attackers step from being skipped.
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(wall), opponent).error shouldNotBe null
    }
})
