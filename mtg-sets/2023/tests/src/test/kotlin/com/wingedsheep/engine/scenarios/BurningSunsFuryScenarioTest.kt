package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Burning Sun's Fury (MOM #133): up to two target creatures each get +2/+0 and haste. */
class BurningSunsFuryScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("two targets each get +2/+0 and haste") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val a = driver.putCreatureOnBattlefield(p, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.getOpponent(p), "Grizzly Bears")
        val spell = driver.putCardInHand(p, "Burning Sun's Fury")
        driver.giveMana(p, Color.RED, 2)

        driver.castSpellWithTargets(
            p, spell,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b))
        )
        driver.bothPass()

        val proj = projector.project(driver.state)
        for (id in listOf(a, b)) {
            proj.getPower(id) shouldBe 4
            proj.getToughness(id) shouldBe 2
            proj.hasKeyword(id, Keyword.HASTE) shouldBe true
        }
    }

    test("can be cast with no targets") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Burning Sun's Fury")
        driver.giveMana(p, Color.RED, 2)
        driver.castSpellWithTargets(p, spell, emptyList())
        driver.bothPass()
        driver.getGraveyardCardNames(p).contains("Burning Sun's Fury") shouldBe true
    }
})
