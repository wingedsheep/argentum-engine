package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.CrystalSlipper
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Crystal Slipper (ELD #119) — {1}{R} Artifact — Equipment.
 *
 * "Equipped creature gets +1/+0 and has haste. Equip {1}"
 */
class CrystalSlipperScenarioTest : FunSpec({

    test("equipping a summoning-sick creature gives +1/+0 and haste, so it can attack this turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CrystalSlipper))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val slipper = driver.putPermanentOnBattlefield(me, "Crystal Slipper")

        driver.giveColorlessMana(me, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = slipper,
                abilityId = CrystalSlipper.activatedAbilities.single { it.isEquipAbility }.id,
                targets = listOf(ChosenTarget.Permanent(bears)),
            )
        ).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 2
        driver.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, mapOf(bears to opponent)).outcome shouldBe Outcome.Done
    }
})
