package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DefilerOfDreams
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DefilerOfDreamsScenarioTest : FunSpec({
    val land = card("Island") { typeLine = "Basic Land — Island" }
    val permanent = card("Blue Test Artifact") { manaCost = "{U}"; typeLine = "Artifact" }
    val instant = card("Blue Test Instant") { manaCost = "{U}"; typeLine = "Instant" }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(land, permanent, instant, DefilerOfDreams))
        initMirrorMatch(Deck.of("Island" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (payLife in listOf(false, true)) test("blue permanent draw trigger fires with life payment $payLife") {
        val d = setup(); val p = d.activePlayer!!
        val source = d.putCreatureOnBattlefield(p, DefilerOfDreams.name)
        val id = d.putCardInHand(p, permanent.name)
        d.giveMana(p, Color.BLUE)
        val action = d.legalActions(p).single {
            (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() == payLife } == true
        }.action
        val handAfterCast = d.getHand(p).size - 1
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe if (payLife) 18 else 20
        d.state.stack.size shouldBe 2
        d.moveToGraveyard(source)
        d.bothPass().error shouldBe null
        d.getHand(p).size shouldBe handAfterCast + 1
        d.state.stack shouldBe listOf(id)
        d.bothPass().error shouldBe null
        d.state.getBattlefield().contains(id) shouldBe true
    }
    test("a blue instant has no discount and no draw trigger") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfDreams.name)
        val id = d.putCardInHand(p, instant.name); d.giveMana(p, Color.BLUE)
        d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == id }.all {
            (it.action as CastSpell).optionalCostPayments.isEmpty()
        } shouldBe true
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack shouldBe listOf(id)
    }
})
