package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.LimitsOfSolidarity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Limits of Solidarity: gain control of target creature until end of turn, untap it, and it gains
 * haste until end of turn — so a tapped, freshly stolen creature can attack this turn.
 */
class LimitsOfSolidarityScenarioTest : FunSpec({

    val Victim = CardDefinition.creature(
        name = "Solidarity Victim",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = emptySet(),
        power = 3,
        toughness = 3
    )

    test("steals a tapped creature, untaps it, and it can attack with haste") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(LimitsOfSolidarity, Victim))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirs = d.putCreatureOnBattlefield(opp, "Solidarity Victim")
        d.removeSummoningSickness(theirs)
        d.tapPermanent(theirs)

        val spell = d.putCardInHand(me, "Limits of Solidarity")
        d.giveMana(me, Color.RED, 4)
        d.castSpell(me, spell, listOf(theirs))
        d.bothPass()

        d.state.projectedState.getController(theirs) shouldBe me
        d.isTapped(theirs) shouldBe false
        d.state.projectedState.hasKeyword(theirs, Keyword.HASTE) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(theirs), opp).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)
        d.getLifeTotal(opp) shouldBe 17

        // Control returns at end of turn.
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.state.projectedState.getController(theirs) shouldBe opp
    }
})
