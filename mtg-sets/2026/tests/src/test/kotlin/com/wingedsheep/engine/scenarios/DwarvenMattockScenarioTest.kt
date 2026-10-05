package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DwarvenMattock
import com.wingedsheep.mtg.sets.definitions.hob.cards.DwarvenMauler
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dwarven Mattock — {2} Artifact — Equipment
 *
 * "When this Equipment enters, attach it to target Dwarf you control.
 *  Equipped creature gets +2/+2 and has ward {1}.
 *  Equip {3}"
 */
class DwarvenMattockScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(DwarvenMattock, DwarvenMauler))
        return driver
    }

    test("ETB attaches to a target Dwarf, granting +2/+2 and ward") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val mauler = driver.putCreatureOnBattlefield(me, "Dwarven Mauler") // 2/1 Dwarf

        val mattock = driver.putCardInHand(me, "Dwarven Mattock")
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, mattock)
        driver.bothPass() // resolve the equipment spell -> it enters -> ETB trigger
        driver.bothPass() // pauses for target selection

        driver.submitTargetSelection(me, listOf(mauler))
        driver.bothPass()

        val mattockId = driver.findPermanent(me, "Dwarven Mattock")!!
        driver.state.getEntity(mattockId)?.get<AttachedToComponent>()?.targetId shouldBe mauler
        projector.getProjectedPower(driver.state, mauler) shouldBe 4
        projector.getProjectedToughness(driver.state, mauler) shouldBe 3
        driver.state.projectedState.hasKeyword(mauler, Keyword.WARD) shouldBe true
    }

    test("with no Dwarf to target, the Equipment stays unattached") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val mattock = driver.putCardInHand(me, "Dwarven Mattock")
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, mattock)
        driver.bothPass()
        driver.bothPass()

        val mattockId = driver.findPermanent(me, "Dwarven Mattock")!!
        driver.state.getEntity(mattockId)?.get<AttachedToComponent>() shouldBe null
        projector.getProjectedPower(driver.state, bears) shouldBe 2
    }
})
