package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hollow One — {5} Artifact Creature — Golem 4/4
 *   "This spell costs {2} less to cast for each card you've cycled or discarded this turn."
 *   Cycling {2}
 *
 * Each test hands the caster exactly the mana the expected cost needs, so a wrong discount shows
 * up as a failed (or, one mana short, a succeeding) cast.
 */
class HollowOneScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("with nothing discarded this turn it costs the full {5}") {
        val d = driver()
        val you = d.player1
        val card = d.putCardInHand(you, "Hollow One")
        d.giveColorlessMana(you, 4)
        (d.castSpell(you, card).error != null) shouldBe true

        d.giveColorlessMana(you, 1)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Hollow One").shouldNotBeNull()
    }

    test("cycling a card counts as a discard — the next Hollow One costs {3}") {
        val d = driver()
        val you = d.player1
        val fodder = d.putCardInHand(you, "Hollow One")
        d.giveColorlessMana(you, 2)
        d.submit(CycleCard(you, fodder)).error shouldBe null

        val card = d.putCardInHand(you, "Hollow One")
        d.giveColorlessMana(you, 2)
        (d.castSpell(you, card).error != null) shouldBe true

        d.giveColorlessMana(you, 1)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Hollow One").shouldNotBeNull()
    }

    test("an opponent's discards don't reduce your Hollow One") {
        val d = driver()
        val you = d.player1
        d.addComponent(d.player2, CardsDiscardedThisTurnComponent(count = 3))

        val card = d.putCardInHand(you, "Hollow One")
        d.giveColorlessMana(you, 4)
        (d.castSpell(you, card).error != null) shouldBe true
    }

    test("after three discards it costs {0}, not less") {
        val d = driver()
        val you = d.player1
        d.addComponent(you, CardsDiscardedThisTurnComponent(count = 3))

        val card = d.putCardInHand(you, "Hollow One")
        d.castSpell(you, card).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Hollow One").shouldNotBeNull()
    }
})
