package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Poison the Cup (KHM #103) — {1}{B}{B} Instant. "Destroy target creature. If this spell was
 * foretold, scry 2." Foretell {1}{B}.
 *
 * Cast from hand it only destroys; cast from exile after being foretold (CR 702.143c) it also
 * scries 2, which surfaces as a pending decision for the caster.
 */
class PoisonTheCupScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast from hand, it destroys the creature and does not scry") {
        val d = driver()
        val p = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(d.getOpponent(p), "Grizzly Bears")
        val cup = d.putCardInHand(p, "Poison the Cup")
        d.giveMana(p, Color.BLACK, 3)
        d.castSpell(p, cup, listOf(bears)).error shouldBe null
        d.bothPass()
        d.state.pendingDecision shouldBe null
        d.getCreatures(d.getOpponent(p)).contains(bears) shouldBe false
    }

    test("foretold and cast for its foretell cost, it destroys the creature and scries 2") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val cup = d.putCardInHand(p, "Poison the Cup")
        d.giveMana(p, Color.BLACK, 2)
        d.submitSuccess(ForetellCard(p, cup))
        // An instant: cast it on the opponent's turn, the turn after it was foretold.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.passPriority(opp) // opponent hands priority to the foreteller
        d.giveMana(p, Color.BLACK, 2)
        d.castSpell(p, cup, listOf(bears)).error shouldBe null
        d.bothPass()
        d.getCreatures(opp).contains(bears) shouldBe false
        val scry = d.state.pendingDecision
        scry shouldNotBe null
        scry!!.playerId shouldBe p
    }
})
