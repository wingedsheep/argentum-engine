package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.StaffOfNin
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Staff of Nin {6} — Artifact
 *   At the beginning of your upkeep, draw a card.
 *   {T}: This artifact deals 1 damage to any target.
 */
class StaffOfNinScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StaffOfNin))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("draws an extra card in your upkeep, not in your opponent's") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Staff of Nin")

        driver.passPriorityUntil(Step.UPKEEP, opp)
        driver.assertStackSize(0, "no trigger on the opponent's upkeep")
        val handBefore = driver.getHandSize(me)

        driver.passPriorityUntil(Step.DRAW, me)
        driver.getHandSize(me) shouldBe handBefore + 2 // upkeep trigger + normal draw
    }

    test("{T}: deals 1 damage to any target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val staff = driver.putPermanentOnBattlefield(me, "Staff of Nin")

        val abilityId = StaffOfNin.script.activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = staff,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opp))
            )
        )
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 19
        driver.isTapped(staff) shouldBe true
    }
})
