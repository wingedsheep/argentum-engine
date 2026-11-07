package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.BreachingHippocamp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Breaching Hippocamp (Theros #43) — {3}{U} Creature — Horse Fish 3/2.
 *
 * "Flash. When this creature enters, untap another target creature you control."
 *
 * Flashed in after attackers are declared, it untaps the attacker — the pseudo-vigilance trick.
 */
class BreachingHippocampScenarioTest : FunSpec({

    test("flashed in during combat, it untaps another creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BreachingHippocamp)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(theirs)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(bears), opponent)
        driver.isTapped(bears) shouldBe true

        val hippocamp = driver.putCardInHand(me, "Breaching Hippocamp")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, hippocamp).outcome shouldBe Outcome.Done
        driver.bothPass() // Hippocamp resolves; enters trigger asks for a target

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        legal shouldContain bears
        legal shouldNotContain hippocamp
        legal shouldNotContain theirs
        driver.submitTargetSelection(me, listOf(bears))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.isTapped(bears) shouldBe false
    }
})
