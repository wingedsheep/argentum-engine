package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LifetapScenarioTest : FunSpec({
    val tap = card("Lifetap test tap") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.Tap(t) }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + tap)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("tapping an opposing Forest by a spell gains life but tapping it again does not") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Lifetap")
        val forest = d.putPermanentOnBattlefield(other, "Forest")
        d.castSpell(me, d.putCardInHand(me, tap.name), listOf(forest)).error shouldBe null
        d.bothPass(); d.bothPass()
        d.getLifeTotal(me) shouldBe 21
        d.castSpell(me, d.putCardInHand(me, tap.name), listOf(forest)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(me) shouldBe 21
    }
    test("your Forest and an opposing non-Forest do not trigger") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Lifetap")
        val lands = listOf(d.putPermanentOnBattlefield(me, "Forest"), d.putPermanentOnBattlefield(other, "Island"))
        lands.forEach {
            d.castSpell(me, d.putCardInHand(me, tap.name), listOf(it)).error shouldBe null
            d.bothPass()
            d.getLifeTotal(me) shouldBe 20
            d.state.stack.size shouldBe 0
        }
    }
})
