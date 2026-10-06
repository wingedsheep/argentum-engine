package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.LightningBolt
import com.wingedsheep.mtg.sets.definitions.m10.cards.TomeScour
import com.wingedsheep.mtg.sets.definitions.mid.cards.Dreadhound
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dreadhound: its drain fires once per creature card moved library -> graveyard (its own ETB mill
 * included, and from any player's library) and once per creature death on either side.
 */
class DreadhoundScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Dreadhound, TomeScour, LightningBolt))
        initMirrorMatch(deck = com.wingedsheep.sdk.model.Deck.of("Swamp" to 40), startingLife = 20, skipMulligans = true)
    }

    fun GameTestDriver.drainStack() {
        while (pendingDecision == null && stackSize > 0) bothPass()
    }

    test("ETB mill drains each opponent once per creature card milled") {
        val d = setup()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        d.putCardOnTopOfLibrary(you, "Swamp")
        d.putCardOnTopOfLibrary(you, "Hill Giant")

        d.giveMana(you, Color.BLACK, 6)
        val hound = d.putCardInHand(you, "Dreadhound")
        d.castSpell(you, hound).error shouldBe null
        d.bothPass()
        d.drainStack()

        d.getGraveyardCardNames(you).count { it == "Grizzly Bears" || it == "Hill Giant" } shouldBe 2
        d.getLifeTotal(opp) shouldBe 18
        d.getLifeTotal(you) shouldBe 20
    }

    test("a creature card milled from an opponent's library also drains") {
        val d = setup()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Dreadhound")

        repeat(4) { d.putCardOnTopOfLibrary(opp, "Swamp") }
        d.putCardOnTopOfLibrary(opp, "Grizzly Bears")

        d.giveMana(you, Color.BLUE, 1)
        val scour = d.putCardInHand(you, "Tome Scour")
        d.castSpell(you, scour, listOf(opp)).error shouldBe null
        d.drainStack()

        d.getGraveyardCardNames(opp).count { it == "Grizzly Bears" } shouldBe 1
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(you) shouldBe 20
    }

    test("any creature dying drains each opponent") {
        val d = setup()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Dreadhound")
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.giveMana(you, Color.RED, 1)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.castSpell(you, bolt, listOf(bears)).error shouldBe null
        d.drainStack()

        d.getGraveyardCardNames(opp).count { it == "Grizzly Bears" } shouldBe 1
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(you) shouldBe 20
    }

    test("Dreadhound dying sees itself die") {
        val d = setup()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hound = d.putCreatureOnBattlefield(you, "Dreadhound")

        d.giveMana(you, Color.RED, 2)
        repeat(2) {
            val bolt = d.putCardInHand(you, "Lightning Bolt")
            d.castSpell(you, bolt, listOf(hound)).error shouldBe null
            d.drainStack()
        }

        d.getGraveyardCardNames(you).count { it == "Dreadhound" } shouldBe 1
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(you) shouldBe 20
    }
})
