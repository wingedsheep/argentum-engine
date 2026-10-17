package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.GeothermalKami
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Geothermal Kami — {3}{G} Creature — Spirit 4/3
 * When this creature enters, you may return an enchantment you control to its owner's hand.
 * If you do, you gain 3 life.
 */
class GeothermalKamiScenarioTest : FunSpec({

    val charm = card("Kami Test Charm") {
        manaCost = "{1}"
        typeLine = "Enchantment"
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + GeothermalKami + charm)
        initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castKami(d: GameTestDriver) {
        val kami = d.putCardInHand(d.player1, GeothermalKami.name)
        d.giveMana(d.player1, Color.GREEN, 4)
        d.castSpell(d.player1, kami).outcome shouldBe Outcome.Done
        d.bothPass() // Kami resolves; its enters trigger goes on the stack
        d.bothPass() // the trigger resolves
    }

    test("returning a chosen enchantment you control gains 3 life") {
        val d = driver()
        val keep = d.putPermanentOnBattlefield(d.player1, charm.name)
        val bounce = d.putPermanentOnBattlefield(d.player1, charm.name)
        castKami(d)

        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, listOf(bounce))

        d.getHand(d.player1) shouldContain bounce
        d.getPermanents(d.player1) shouldContain keep
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("declining returns nothing and gains no life") {
        val d = driver()
        val charmId = d.putPermanentOnBattlefield(d.player1, charm.name)
        castKami(d)

        d.submitYesNo(d.player1, false)

        d.getPermanents(d.player1) shouldContain charmId
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("an opponent's enchantment can't be returned, so no life is gained") {
        val d = driver()
        val theirs = d.putPermanentOnBattlefield(d.player2, charm.name)
        castKami(d)

        if (d.pendingDecision != null) d.submitYesNo(d.player1, true)
        // Nothing of the opponent's is offered for the bounce.
        d.pendingDecision shouldBe null

        d.getHand(d.player2) shouldNotContain theirs
        d.getLifeTotal(d.player1) shouldBe 20
    }
})
