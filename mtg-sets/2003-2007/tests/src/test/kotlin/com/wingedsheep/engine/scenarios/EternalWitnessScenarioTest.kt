package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Eternal Witness (5DN #86) — {1}{G}{G} 2/1 Human Shaman, "When this creature enters, you may return
 * target card from **your** graveyard to your hand."
 *
 * The same bug Recollect shipped with, in a trigger: the definition filtered on
 * `TargetFilter.CardInGraveyard`, which is *any* graveyard, so the witness could recur out of an
 * opponent's. Found by the Argentum Assay differential gate.
 *
 * The third test is the half that fails without the fix — with only the opponent's graveyard
 * populated the trigger has no legal target at all, so it never goes on the stack (CR 603.3d) and
 * the game does not stop to ask.
 */
class EternalWitnessScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 30))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * The witness has to be **cast**: putting a permanent straight onto the battlefield emits no
     * zone-change event, so no enters trigger fires and there is nothing to target.
     */
    fun castWitness(d: GameTestDriver) {
        val card = d.putCardInHand(d.player1, "Eternal Witness")
        d.giveMana(d.player1, Color.GREEN, 3)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature; its enters trigger goes on the stack and wants a target
        // Announce the target when the trigger is stacked; answer consent when it resolves.
        // for, so accept it here and leave the target decision pending for the caller. A trigger
        // with no legal target never asks (CR 603.3d), which is what the third test relies on.
    }

    test("the enters trigger returns a card from your own graveyard") {
        val d = driver()
        val mine = d.putCardInGraveyard(d.player1, "Grizzly Bears")

        castWitness(d)
        d.findPermanent(d.player1, "Eternal Witness") shouldNotBe null

        val decision = d.pendingDecision.shouldNotBeNull() as ChooseTargetsDecision
        decision.legalTargets.getValue(0) shouldContain mine

        d.submitTargetSelection(d.player1, listOf(mine))
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, true).error shouldBe null

        d.getGraveyardCardNames(d.player1) shouldNotContain "Grizzly Bears"
        d.getHand(d.player1) shouldContain mine
    }

    test("a card in an opponent's graveyard is not a legal target") {
        val d = driver()
        val mine = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val theirs = d.putCardInGraveyard(d.player2, "Centaur Courser")

        castWitness(d)

        val decision = d.pendingDecision.shouldNotBeNull() as ChooseTargetsDecision
        val legal = decision.legalTargets.getValue(0)

        legal shouldContain mine
        legal shouldNotContain theirs
    }

    // The regression. With the unowned filter, an opponent's graveyard alone was enough to give the
    // trigger a target, and the game stopped to ask for one.
    test("the trigger has no target when only an opponent's graveyard has cards") {
        val d = driver()
        d.putCardInGraveyard(d.player2, "Centaur Courser")
        d.getGraveyardCardNames(d.player1).size shouldBe 0

        castWitness(d)
        d.findPermanent(d.player1, "Eternal Witness") shouldNotBe null

        d.isPaused shouldBe false
    }
})
