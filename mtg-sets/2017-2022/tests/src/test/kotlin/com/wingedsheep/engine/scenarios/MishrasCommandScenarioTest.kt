package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.MishrasCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mishra's Command (BRO #141) — {X}{R} Sorcery, choose two.
 *
 * The rummage mode is the one whose correctness depends on how the engine runs the script: the
 * *targeted* player (not the caster) chooses up to X cards from their own hand, inside a modal
 * spell whose per-mode target lists decide who "they" are, and the draw counts exactly the cards
 * discarded — so a decline draws nothing.
 */
class MishrasCommandScenarioTest : FunSpec({

    val projector = StateProjector()

    val rummage = 0
    val damageCreature = 1
    val pump = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MishrasCommand))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(
        caster: EntityId,
        x: Int,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): ExecutionResult {
        giveMana(caster, Color.RED, x + 1)
        val spell = putCardInHand(caster, "Mishra's Command")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
                xValue = x,
            )
        )
    }

    test("the targeted opponent discards up to X of their own cards and draws that many") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears") // 2/2
        val a = driver.putCardInHand(opp, "Grizzly Bears")
        val b = driver.putCardInHand(opp, "Grizzly Bears")
        driver.putCardInHand(opp, "Grizzly Bears")
        val handBefore = driver.getHandSize(opp)

        driver.castCommand(
            me,
            x = 2,
            modes = listOf(rummage, damageCreature),
            modeTargets = listOf(
                listOf(ChosenTarget.Player(opp)),
                listOf(ChosenTarget.Permanent(bear)),
            ),
        ).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opp
        decision.maxSelections shouldBe 2
        driver.submitCardSelection(opp, listOf(a, b)).error shouldBe null

        driver.getGraveyard(opp) shouldContainAll listOf(a, b)
        driver.getHandSize(opp) shouldBe handBefore
        // X = 2 damage to the 2/2.
        driver.findPermanent(opp, "Grizzly Bears") shouldBe null
    }

    test("declining the discard draws nothing; +X/+0 and haste land on the pumped creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val myBear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCardInHand(opp, "Grizzly Bears")
        val handBefore = driver.getHandSize(opp)

        driver.castCommand(
            me,
            x = 3,
            modes = listOf(rummage, pump),
            modeTargets = listOf(
                listOf(ChosenTarget.Player(opp)),
                listOf(ChosenTarget.Permanent(myBear)),
            ),
        ).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opp, emptyList()).error shouldBe null

        driver.getHandSize(opp) shouldBe handBefore
        projector.getProjectedPower(driver.state, myBear) shouldBe 5
        projector.getProjectedToughness(driver.state, myBear) shouldBe 2
        driver.state.projectedState.hasKeyword(myBear, Keyword.HASTE) shouldBe true
    }
})
