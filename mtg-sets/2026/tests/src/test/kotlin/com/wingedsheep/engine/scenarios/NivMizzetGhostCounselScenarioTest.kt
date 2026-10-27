package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frc.cards.NivMizzetGhostCounsel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Niv-Mizzet, Ghost Counsel — "Whenever you gain life, you may pay that much life. If you do, draw
 * that many cards." The trigger's amount has to survive the may-pay pause and price both the life
 * payment and the draw.
 */
class NivMizzetGhostCounselScenarioTest : FunSpec({

    val healingSalve = CardDefinition.instant(
        name = "Healing Salve",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "You gain 3 life.",
        script = CardScript.spell(effect = GainLifeEffect(3))
    )

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NivMizzetGhostCounsel, healingSalve))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "Niv-Mizzet, Ghost Counsel")
        val salve = driver.putCardInHand(p1, "Healing Salve")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.castSpell(p1, salve)
        driver.bothPass() // resolve Healing Salve — Niv-Mizzet triggers
        driver.bothPass() // resolve the trigger — pauses on the may-pay
        return driver to p1
    }

    test("paying that much life draws that many cards") {
        val (driver, p1) = setUp()
        val handBefore = driver.getHandSize(p1)
        driver.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        driver.submitYesNo(p1, true)

        driver.assertLifeTotal(p1, 20) // 20 + 3 gained - 3 paid
        driver.getHandSize(p1) shouldBe handBefore + 3
    }

    test("declining keeps the life and draws nothing") {
        val (driver, p1) = setUp()
        val handBefore = driver.getHandSize(p1)
        driver.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        driver.submitYesNo(p1, false)

        driver.assertLifeTotal(p1, 23)
        driver.getHandSize(p1) shouldBe handBefore
    }
})
