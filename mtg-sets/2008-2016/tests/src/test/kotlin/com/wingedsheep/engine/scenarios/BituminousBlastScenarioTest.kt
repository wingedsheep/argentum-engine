package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arb.cards.BituminousBlast
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bituminous Blast (Alara Reborn #34) — {3}{B}{R} Instant.
 *
 * "Cascade. Bituminous Blast deals 4 damage to target creature."
 *
 * Cascade is the cast trigger (CR 702.85a): it resolves before the Blast, so the cascaded
 * Lightning Bolt hits first, then the Blast deals its 4.
 */
class BituminousBlastScenarioTest : FunSpec({

    test("casting it cascades into a cheaper spell, then it deals 4 damage to the target creature") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BituminousBlast)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        // Cascade walks from the top: a land, then Lightning Bolt (MV 1 < 5) as the hit.
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        driver.putCardOnTopOfLibrary(me, "Mountain")

        val blast = driver.putCardInHand(me, "Bituminous Blast")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, blast, targets = listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass() // cascade trigger resolves -> finds Lightning Bolt -> may-cast pause

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(opponent))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getGraveyard(opponent) shouldContain bears
        driver.getGraveyard(me) shouldContain blast
    }
})
