package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.MagitekScythe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Magitek Scythe — {4} Artifact — Equipment.
 *
 * "A Test of Your Reflexes! — When this Equipment enters, you may attach it to target creature you
 *  control. If you do, that creature gains first strike until end of turn and must be blocked this
 *  turn if able.
 *  Equipped creature gets +2/+1.
 *  Equip {2}"
 *
 * Exercises the optional ETB attach + first-strike rider (the must-be-blocked rider is the engine's
 * already-tested `MustBeBlockedEffect`).
 */
class MagitekScytheScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MagitekScythe)
        return driver
    }

    test("accepting the may-attach equips the chosen creature with +2/+1 and first strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.project(driver.state).hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false

        val scythe = driver.putCardInHand(me, "Magitek Scythe")
        driver.giveMana(me, Color.RED, 4) // {4} generic
        driver.castSpell(me, scythe)
        driver.bothPass() // resolve the artifact -> it enters -> ETB trigger on stack

        // Resolve the ETB trigger, answering its decisions: accept the "you may attach" and pick the
        // creature to equip (order-agnostic).
        repeat(8) {
            when (driver.state.pendingDecision) {
                is YesNoDecision -> driver.submitYesNo(me, true)
                is ChooseTargetsDecision -> driver.submitTargetSelection(me, listOf(courser))
                else -> if (driver.stackSize > 0) driver.bothPass() else return@repeat
            }
        }

        val swordId = driver.findPermanent(me, "Magitek Scythe")!!
        driver.state.getEntity(swordId)?.get<AttachedToComponent>()?.targetId shouldBe courser

        projector.getProjectedPower(driver.state, courser) shouldBe 5  // +2
        projector.getProjectedToughness(driver.state, courser) shouldBe 4 // +1
        projector.project(driver.state).hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("declining the may-attach leaves the Equipment unattached") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        val scythe = driver.putCardInHand(me, "Magitek Scythe")
        driver.giveMana(me, Color.RED, 4)
        driver.castSpell(me, scythe)
        driver.bothPass()
        if (driver.stackSize > 0) driver.bothPass()

        driver.submitTargetSelection(me, listOf(courser)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.submitYesNo(me, false).error shouldBe null
        if (driver.isPaused) driver.bothPass()

        val swordId = driver.findPermanent(me, "Magitek Scythe")!!
        driver.state.getEntity(swordId)?.get<AttachedToComponent>() shouldBe null
        // The creature gained nothing.
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.project(driver.state).hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false
    }
})
