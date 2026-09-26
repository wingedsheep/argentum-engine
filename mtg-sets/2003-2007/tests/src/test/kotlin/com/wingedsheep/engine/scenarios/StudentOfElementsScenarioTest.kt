package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.StudentOfElements
import com.wingedsheep.mtg.sets.definitions.ulg.cards.Levitation
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Student of Elements // Tobita, Master of Winds (CHK) — a flip card.
 *
 * "When this creature has flying, flip it." (state trigger, CR 603.8)
 * Tobita: "Creatures you control have flying."
 */
class StudentOfElementsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + StudentOfElements + Levitation)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    test("without flying it stays a Student of Elements") {
        val d = driver()
        val me = d.player1

        val student = d.putCreatureOnBattlefield(me, "Student of Elements")
        d.bothPass()
        d.drainStack()

        d.name(student) shouldBe "Student of Elements"
        d.state.projectedState.hasKeyword(student, Keyword.FLYING) shouldBe false
    }

    test("gaining flying flips it into Tobita, whose static keeps flying on after the source leaves") {
        val d = driver()
        val me = d.player1
        val opp = d.getOpponent(me)

        val student = d.putCreatureOnBattlefield(me, "Student of Elements")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val levitation = d.putPermanentOnBattlefield(me, "Levitation")

        d.state.projectedState.hasKeyword(student, Keyword.FLYING) shouldBe true
        d.bothPass() // state trigger goes on the stack at the priority check
        d.drainStack()

        d.name(student) shouldBe "Tobita, Master of Winds"
        d.state.projectedState.isLegendary(student) shouldBe true
        d.state.projectedState.getPower(student) shouldBe 3
        d.state.projectedState.getToughness(student) shouldBe 3

        d.moveToGraveyard(levitation)
        d.drainStack()

        withClue("Tobita alone grants flying to creatures its controller controls") {
            d.state.projectedState.hasKeyword(student, Keyword.FLYING) shouldBe true
            d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            d.state.projectedState.hasKeyword(oppBears, Keyword.FLYING) shouldBe false
        }
        d.name(student) shouldBe "Tobita, Master of Winds"
    }
})
