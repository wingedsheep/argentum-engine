package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.SublimeEpiphany
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sublime Epiphany (M21 #74) — {4}{U}{U} Instant, "Choose one or more" over five modes.
 *
 * What the snapshot can't show is that several chosen modes each bind their *own* target: a
 * bounce, a token copy and a targeted draw in one cast must hit three different objects, and a
 * counter mode chosen alongside a draw mode must still find the spell it targeted.
 */
class SublimeEpiphanyScenarioTest : FunSpec({

    // Mode order follows the printed bullets.
    val counterSpell = 0
    val bounce = 2
    val copyCreature = 3
    val targetDraws = 4

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SublimeEpiphany))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castEpiphany(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): ExecutionResult {
        giveMana(caster, Color.BLUE, 6)
        val spell = putCardInHand(caster, "Sublime Epiphany")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
            )
        )
    }

    test("bounce, copy and targeted draw each resolve against their own target") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val oppHandBefore = driver.getHandSize(opp)

        driver.castEpiphany(
            me,
            modes = listOf(bounce, copyCreature, targetDraws),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(bears)),
                listOf(ChosenTarget.Permanent(courser)),
                listOf(ChosenTarget.Player(opp)),
            ),
        ).error shouldBe null
        driver.bothPass()

        driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        // The bounced Bears and the drawn card both land in the opponent's hand.
        driver.getHandSize(opp) shouldBe oppHandBefore + 2
        driver.getPermanents(me).count { driver.getCardName(it) == "Centaur Courser" } shouldBe 2
    }

    test("counter target spell alongside a draw — the spell is countered and the caster draws") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        driver.giveMana(opp, Color.RED, 1)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.passPriority(me)
        driver.submit(
            CastSpell(playerId = opp, cardId = bolt, targets = listOf(ChosenTarget.Permanent(courser)))
        ).error shouldBe null
        driver.passPriority(opp)

        val stackSpell = driver.getTopOfStack()!!
        val handBefore = driver.getHandSize(me)
        driver.castEpiphany(
            me,
            modes = listOf(counterSpell, targetDraws),
            modeTargets = listOf(listOf(ChosenTarget.Spell(stackSpell)), listOf(ChosenTarget.Player(me))),
        ).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.assertInGraveyard(opp, "Lightning Bolt")
        driver.findPermanent(me, "Centaur Courser") shouldBe courser
        // Epiphany entered and left the hand on the cast, so only the draw shows against the count.
        driver.getHandSize(me) shouldBe handBefore + 1
    }
})
