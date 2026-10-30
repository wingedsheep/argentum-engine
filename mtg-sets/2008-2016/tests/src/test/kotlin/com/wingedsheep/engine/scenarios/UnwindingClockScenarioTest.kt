package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nph.cards.UnwindingClock
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unwinding Clock — {4} Artifact.
 *   "Untap all artifacts you control during each other player's untap step."
 *
 * Ruling: "doesn't untap during its controller's untap step" effects (Rust Tick) don't stop the
 * artifact untapping during another player's untap step — they still apply on your own.
 */
class UnwindingClockScenarioTest : FunSpec({

    val PlainRock = card("Clock Test Plain Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    // "This artifact doesn't untap during your untap step." (Colossus of Sardia's clause)
    val StuckRock = card("Clock Test Stuck Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        flags(AbilityFlag.DOESNT_UNTAP)
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(UnwindingClock, PlainRock, StuckRock))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    test("untaps your artifacts, including a doesn't-untap one, during another player's untap step") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val clock = d.putPermanentOnBattlefield(you, "Unwinding Clock")
        val plain = d.putPermanentOnBattlefield(you, "Clock Test Plain Rock")
        val stuck = d.putPermanentOnBattlefield(you, "Clock Test Stuck Rock")
        val bears = d.putCreatureOnBattlefield(you, "Centaur Courser")
        listOf(clock, plain, stuck, bears).forEach { d.tapPermanent(it) }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opponent

        d.isTapped(clock) shouldBe false
        d.isTapped(plain) shouldBe false
        d.isTapped(stuck) shouldBe false
        // Only artifacts: the nonartifact creature stays tapped.
        d.isTapped(bears) shouldBe true
    }

    test("a doesn't-untap artifact still stays tapped through your own untap step") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(you, "Unwinding Clock")
        val stuck = d.putPermanentOnBattlefield(you, "Clock Test Stuck Rock")

        // Tap it during the opponent's turn, after their untap step.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opponent
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapPermanent(stuck)

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe you
        d.isTapped(stuck) shouldBe true
    }
})
