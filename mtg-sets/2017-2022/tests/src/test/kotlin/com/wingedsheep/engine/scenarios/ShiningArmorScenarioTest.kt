package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.ShiningArmor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shining Armor (ELD #29) — {1}{W} Artifact — Equipment.
 *
 * "Flash. When this Equipment enters, attach it to target Knight you control.
 *  Equipped creature gets +0/+2 and has vigilance. Equip {3}"
 */
class ShiningArmorScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShiningArmor, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("flash: cast on the opponent's turn, ETB attaches to a Knight for +0/+2 and vigilance") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val me = driver.getOpponent(active)
        val knight = driver.putCreatureOnBattlefield(me, "Test Knight")

        val armor = driver.putCardInHand(me, "Shining Armor")
        driver.giveMana(me, Color.WHITE, 2)
        driver.passPriority(active)
        driver.castSpell(me, armor)
        driver.bothPass() // resolve the spell -> ETB trigger
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(knight))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val armorId = driver.findPermanent(me, "Shining Armor")!!
        driver.state.getEntity(armorId)?.get<AttachedToComponent>()?.targetId shouldBe knight
        driver.state.projectedState.getPower(knight) shouldBe 2
        driver.state.projectedState.getToughness(knight) shouldBe 4
        driver.state.projectedState.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
    }

    test("a non-Knight creature isn't a legal target for the ETB, so the armor stays unattached") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val armor = driver.putCardInHand(me, "Shining Armor")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, armor)
        driver.bothPass()
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val armorId = driver.findPermanent(me, "Shining Armor")!!
        driver.state.getEntity(armorId)?.get<AttachedToComponent>() shouldBe null
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }
})
