package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.AvenSurveyor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Aven Surveyor (Fate Reforged #31) — {3}{U}{U} Creature — Bird Scout 2/2, flying.
 *
 * "When this creature enters, choose one — • Put a +1/+1 counter on this creature.
 *  • Return target creature to its owner's hand."
 */
class AvenSurveyorScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AvenSurveyor)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Aven Surveyor, pick the mode starting with [modePrefix], target [target] if asked. */
    fun GameTestDriver.castSurveyor(me: EntityId, modePrefix: String, target: EntityId? = null): EntityId {
        val surveyor = putCardInHand(me, "Aven Surveyor")
        giveMana(me, Color.BLUE, 5)
        castSpell(me, surveyor).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 20) {
            when (val decision = pendingDecision) {
                is ChooseOptionDecision -> {
                    val index = decision.options.indexOfFirst { it.startsWith(modePrefix) }
                    check(index >= 0) { "$modePrefix not offered; options=${decision.options}" }
                    submitDecision(me, OptionChosenResponse(decision.id, index))
                }
                is ChooseTargetsDecision -> {
                    val chosen = checkNotNull(target) { "unexpected target prompt" }
                    decision.legalTargets.getValue(0) shouldContain chosen
                    submitTargetSelection(me, listOf(chosen)).error shouldBe null
                }
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> error("unexpected decision $decision")
            }
        }
        return surveyor
    }

    test("first mode puts a +1/+1 counter on it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val surveyor = driver.castSurveyor(me, "Put a +1/+1 counter")

        driver.state.projectedState.getPower(surveyor) shouldBe 3
        driver.state.projectedState.getToughness(surveyor) shouldBe 3
    }

    test("second mode returns the target creature to its owner's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val surveyor = driver.castSurveyor(me, "Return target creature", target = courser)

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getHand(opponent) shouldContain courser
        driver.state.projectedState.getPower(surveyor) shouldBe 2
    }
})
