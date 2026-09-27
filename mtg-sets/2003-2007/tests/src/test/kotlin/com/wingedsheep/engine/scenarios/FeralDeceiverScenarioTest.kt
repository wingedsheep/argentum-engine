package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.FeralDeceiver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feral Deceiver (CHK #208) — "{1}: Look at the top card of your library. {2}: Reveal the top card
 * of your library. If it's a land card, this creature gets +2/+2 and gains trample until end of
 * turn. Activate only once each turn."
 */
class FeralDeceiverScenarioTest : FunSpec({

    val lookAbility = FeralDeceiver.activatedAbilities[0].id
    val revealAbility = FeralDeceiver.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + FeralDeceiver)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("revealing a land pumps +2/+2 and grants trample; the land stays on top") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Forest")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Feral Deceiver")
        d.giveMana(d.player1, Color.GREEN, 2)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()

        val projected = d.state.projectedState
        projected.getPower(deceiver) shouldBe 5
        projected.getToughness(deceiver) shouldBe 4
        projected.hasKeyword(deceiver, Keyword.TRAMPLE) shouldBe true
        withClue("the revealed card is not moved") {
            d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top
        }
    }

    test("revealing a nonland card does nothing") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Feral Deceiver")
        d.giveMana(d.player1, Color.GREEN, 2)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()

        val projected = d.state.projectedState
        projected.getPower(deceiver) shouldBe 3
        projected.getToughness(deceiver) shouldBe 2
        projected.hasKeyword(deceiver, Keyword.TRAMPLE) shouldBe false
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top
    }

    test("the reveal ability can be activated only once each turn") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Forest")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Feral Deceiver")
        d.giveMana(d.player1, Color.GREEN, 4)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.submitExpectFailure(ActivateAbility(d.player1, deceiver, revealAbility))
        d.state.projectedState.getPower(deceiver) shouldBe 5
    }

    test("the look ability leaves the top card in place and can be activated repeatedly") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Feral Deceiver")
        d.giveMana(d.player1, Color.GREEN, 2)

        repeat(2) {
            d.submit(ActivateAbility(d.player1, deceiver, lookAbility)).outcome shouldBe Outcome.Done
            d.bothPass()
        }

        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top
        d.state.projectedState.getPower(deceiver) shouldBe 3
    }
})
