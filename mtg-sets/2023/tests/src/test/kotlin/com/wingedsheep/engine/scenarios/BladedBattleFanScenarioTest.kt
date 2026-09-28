package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.BladedBattleFan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bladed Battle-Fan (MOM #91) — Flash; ETB attach to target creature you control and it gains
 * indestructible until end of turn; equipped creature gets +1/+0; Equip {1}.
 */
class BladedBattleFanScenarioTest : FunSpec({

    val projector = StateProjector()

    test("flash ETB attaches to target creature, +1/+0 and indestructible this turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BladedBattleFan))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.UPKEEP) // not a main phase: flash only

        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val fan = driver.putCardInHand(p1, "Bladed Battle-Fan")
        driver.giveMana(p1, Color.BLACK, 2)
        driver.castSpell(p1, fan, emptyList()).error shouldBe null

        driver.bothPass() // resolve the artifact; trigger asks for a target
        driver.submitTargetSelection(p1, listOf(bears)).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.state.getEntity(fan)!!.get<AttachedToComponent>()?.targetId shouldBe bears
        val projected = projector.project(driver.state)
        projected.getPower(bears) shouldBe 3
        projected.getToughness(bears) shouldBe 2
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
    }
})
