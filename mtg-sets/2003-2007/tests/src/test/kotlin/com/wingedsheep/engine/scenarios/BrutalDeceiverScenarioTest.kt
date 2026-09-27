package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BrutalDeceiver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BrutalDeceiverScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BrutalDeceiver))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val lookAbility = BrutalDeceiver.activatedAbilities[0].id
    val revealAbility = BrutalDeceiver.activatedAbilities[1].id

    test("revealing a land gives +1/+0 and first strike, and the card stays on top") {
        val d = driver()
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Brutal Deceiver")
        val top = d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.giveMana(d.player1, Color.RED, 2)
        d.submitSuccess(ActivateAbility(d.player1, deceiver, revealAbility))
        d.bothPass()
        d.state.projectedState.getPower(deceiver) shouldBe 3
        d.state.projectedState.getToughness(deceiver) shouldBe 2
        d.state.projectedState.hasKeyword(deceiver, Keyword.FIRST_STRIKE) shouldBe true
        d.state.getLibrary(d.player1).first() shouldBe top
    }

    test("revealing a nonland card gives nothing") {
        val d = driver()
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Brutal Deceiver")
        val top = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 2)
        d.submitSuccess(ActivateAbility(d.player1, deceiver, revealAbility))
        d.bothPass()
        d.state.projectedState.getPower(deceiver) shouldBe 2
        d.state.projectedState.hasKeyword(deceiver, Keyword.FIRST_STRIKE) shouldBe false
        d.state.getLibrary(d.player1).first() shouldBe top
    }

    test("the reveal ability can be activated only once each turn") {
        val d = driver()
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Brutal Deceiver")
        d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.giveMana(d.player1, Color.RED, 4)
        d.submitSuccess(ActivateAbility(d.player1, deceiver, revealAbility))
        d.bothPass()
        d.submitExpectFailure(ActivateAbility(d.player1, deceiver, revealAbility))
        d.state.projectedState.getPower(deceiver) shouldBe 3
    }

    test("the look ability is repeatable and leaves the library unchanged") {
        val d = driver()
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Brutal Deceiver")
        d.putCardOnTopOfLibrary(d.player1, "Mountain")
        val before = d.state.getLibrary(d.player1)
        d.giveMana(d.player1, Color.RED, 2)
        d.submitSuccess(ActivateAbility(d.player1, deceiver, lookAbility))
        d.bothPass()
        d.submitSuccess(ActivateAbility(d.player1, deceiver, lookAbility))
        d.bothPass()
        d.state.getLibrary(d.player1) shouldBe before
        d.state.projectedState.getPower(deceiver) shouldBe 2
    }

    test("an empty library reveals nothing and grants nothing") {
        val d = driver()
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Brutal Deceiver")
        d.replaceState(d.state.copy(zones = d.state.zones + (ZoneKey(d.player1, Zone.LIBRARY) to emptyList())))
        d.giveMana(d.player1, Color.RED, 2)
        d.submitSuccess(ActivateAbility(d.player1, deceiver, revealAbility))
        d.bothPass()
        d.state.projectedState.getPower(deceiver) shouldBe 2
        d.state.projectedState.hasKeyword(deceiver, Keyword.FIRST_STRIKE) shouldBe false
    }
})
