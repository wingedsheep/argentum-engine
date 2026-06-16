package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SandstormVerge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sandstorm Verge (OTJ #263) — Land — Desert.
 *
 * "{T}: Add {C}.
 *  {3}, {T}: Target creature can't block this turn. Activate only as a sorcery."
 *
 * Verifies the sorcery-speed {3},{T} ability makes a target creature unable to block this turn
 * (projected `cantBlock`).
 */
class SandstormVergeScenarioTest : FunSpec({

    val cantBlockAbilityId = SandstormVerge.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SandstormVerge)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the {3},{T} ability makes a target creature unable to block this turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val verge = driver.putPermanentOnBattlefield(me, "Sandstorm Verge")
        // Lands aren't summoning-sick for tapping, but clear it to be safe for the {T} cost.
        driver.removeSummoningSickness(verge)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        driver.giveColorlessMana(me, 3)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = verge,
                abilityId = cantBlockAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
            ),
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the ability

        driver.state.projectedState.cantBlock(bears) shouldBe true
    }
})
