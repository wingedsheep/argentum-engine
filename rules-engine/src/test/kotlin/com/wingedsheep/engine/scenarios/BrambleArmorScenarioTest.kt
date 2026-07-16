package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.BrambleArmor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bramble Armor (MID #171, reprinted VOW #188) — {1}{G} Artifact — Equipment.
 *
 * "When this Equipment enters, attach it to target creature you control.
 *  Equipped creature gets +2/+1.
 *  Equip {4}"
 */
class BrambleArmorScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BrambleArmor)
        return driver
    }

    test("ETB attaches to a target creature you control, granting +2/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2

        projector.getProjectedPower(driver.state, bear) shouldBe 2
        projector.getProjectedToughness(driver.state, bear) shouldBe 2

        val armor = driver.putCardInHand(me, "Bramble Armor")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 2)
        driver.castSpell(me, armor)
        driver.bothPass() // resolve the equipment spell -> it enters -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> pauses for target selection

        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass()

        val armorId = driver.findPermanent(me, "Bramble Armor")!!
        driver.state.getEntity(armorId)?.get<AttachedToComponent>()?.targetId shouldBe bear
        projector.getProjectedPower(driver.state, bear) shouldBe 4
        projector.getProjectedToughness(driver.state, bear) shouldBe 3
    }

    test("with no creature you control, the ETB trigger has no legal target and Bramble Armor stays unattached") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        // No creatures on the battlefield at all.

        val armor = driver.putCardInHand(me, "Bramble Armor")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 2)
        driver.castSpell(me, armor)
        driver.bothPass() // resolve the spell -> it enters; the ETB trigger has no legal target
        driver.bothPass() // trigger doesn't fire (no legal target) or fizzles harmlessly

        val armorId = driver.findPermanent(me, "Bramble Armor")!!
        driver.state.getEntity(armorId)?.get<AttachedToComponent>() shouldBe null
    }
})
