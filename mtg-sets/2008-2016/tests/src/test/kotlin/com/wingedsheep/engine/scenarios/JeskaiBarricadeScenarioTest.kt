package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.JeskaiBarricade
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jeskai Barricade (Fate Reforged #15) — {1}{W} Creature — Wall 0/4, flash, defender.
 *
 * "When this creature enters, you may return another target creature you control to its
 *  owner's hand."
 *
 * Pins the target restriction (another creature you control — not itself, not an opponent's) and
 * the optional return asked on resolution.
 */
class JeskaiBarricadeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + JeskaiBarricade)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Jeskai Barricade, target [target] with its enters trigger, and answer the "may" with [accept]. */
    fun GameTestDriver.castBarricade(me: EntityId, target: EntityId, forbidden: List<EntityId>, accept: Boolean) {
        val barricade = putCardInHand(me, "Jeskai Barricade")
        giveMana(me, Color.WHITE, 2)
        castSpell(me, barricade).outcome shouldBe Outcome.Done
        var asked = false
        var guard = 0
        while (guard++ < 20) {
            when (val decision = pendingDecision) {
                is ChooseTargetsDecision -> {
                    val legal = decision.legalTargets.getValue(0)
                    legal shouldContain target
                    withClue("Barricade can't target itself") {
                        legal shouldNotContain findPermanent(me, "Jeskai Barricade")!!
                    }
                    forbidden.forEach { withClue("only creatures you control") { legal shouldNotContain it } }
                    submitTargetSelection(me, listOf(target)).error shouldBe null
                }
                is YesNoDecision -> {
                    asked = true
                    submitYesNo(me, accept).error shouldBe null
                }
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> error("unexpected decision $decision")
            }
        }
        withClue("the return is optional") { asked shouldBe true }
    }

    test("accepting returns another creature you control to its owner's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castBarricade(me, bears, forbidden = listOf(theirs), accept = true)

        driver.findPermanent(me, "Grizzly Bears") shouldBe null
        driver.getHand(me) shouldContain bears
        driver.findPermanent(me, "Jeskai Barricade") shouldNotBe null
    }

    test("declining leaves the creature on the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.castBarricade(me, bears, forbidden = emptyList(), accept = false)

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        driver.getHand(me) shouldNotContain bears
    }
})
