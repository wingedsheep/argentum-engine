package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CoralSword
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Coral Sword — {R} Artifact — Equipment
 *
 * "Flash
 *  When this Equipment enters, attach it to target creature you control. That creature
 *  gains first strike until end of turn.
 *  Equipped creature gets +1/+0.
 *  Equip {1}"
 */
class CoralSwordTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CoralSword)
        return driver
    }

    test("ETB auto-attaches to a chosen creature, granting +1/+0 and first strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        // Base 3/3, no first strike before equipping.
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
        projector.project(driver.state).hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false

        // Cast Coral Sword; its ETB trigger targets a creature you control.
        val sword = driver.putCardInHand(me, "Coral Sword")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, sword)
        driver.bothPass() // resolve the artifact spell -> it enters -> ETB trigger goes on stack
        driver.bothPass() // resolve ETB trigger -> pauses for target selection

        driver.submitTargetSelection(me, listOf(courser))
        driver.bothPass()

        // Attached to the chosen creature.
        val swordId = driver.findPermanent(me, "Coral Sword")!!
        driver.state.getEntity(swordId)?.get<AttachedToComponent>()?.targetId shouldBe courser

        // +1/+0 from the equip static, and first strike until end of turn.
        projector.getProjectedPower(driver.state, courser) shouldBe 4
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
        projector.project(driver.state).hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe true
    }
})
