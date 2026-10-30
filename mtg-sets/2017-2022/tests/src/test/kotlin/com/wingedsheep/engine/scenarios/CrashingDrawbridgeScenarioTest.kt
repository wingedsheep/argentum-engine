package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.CrashingDrawbridge
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Crashing Drawbridge (ELD #217) — "Defender. {T}: Creatures you control gain haste until end of
 * turn."
 */
class CrashingDrawbridgeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CrashingDrawbridge))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("tapping it gives your creatures haste, not the opponent's, and a fresh creature can attack") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val bridge = driver.putCreatureOnBattlefield(me, "Crashing Drawbridge")
        driver.removeSummoningSickness(bridge)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(foe, "Grizzly Bears")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = bridge,
                abilityId = CrashingDrawbridge.activatedAbilities.first().id,
            )
        ).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
        driver.state.projectedState.hasKeyword(theirBears, Keyword.HASTE) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(bears), foe).error shouldBe null
    }
})
