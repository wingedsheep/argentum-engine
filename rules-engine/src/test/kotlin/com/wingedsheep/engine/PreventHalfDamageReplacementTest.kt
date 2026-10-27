package com.wingedsheep.engine

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `PreventDamage(half = true)` — the standing "prevent half that damage" prevention effect, with
 * synthetic hosts so the primitive doesn't move with Gisela, Blade of Goldnight (its one printed
 * card, covered in `GiselaBladeOfGoldnightScenarioTest`). Covers both rounding directions, the
 * 1-damage edge, and the combat damage path.
 */
class PreventHalfDamageReplacementTest : FunSpec({

    fun halfWard(name: String, roundUp: Boolean): CardDefinition = card(name) {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to you, prevent half that damage, rounded " +
            (if (roundUp) "up." else "down.")
        replacementEffect(
            PreventDamage(
                half = true,
                roundUp = roundUp,
                appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)
            )
        )
    }

    val wardUp = halfWard("Test Ward Up", roundUp = true)
    val wardDown = halfWard("Test Ward Down", roundUp = false)
    val wardOne: CardDefinition = card("Test Ward One") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to you, prevent 1 of that damage."
        replacementEffect(
            PreventDamage(amount = 1, appliesTo = EventPattern.DamageEvent(recipient = Recipient.You))
        )
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(wardUp, wardDown, wardOne))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        castSpell(player1, putCardInHand(player1, "Lightning Bolt"), listOf(target)).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    test("rounded up: 3 damage loses 2") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("rounded down: 3 damage loses 1") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward Down")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("two hosts each apply once: 3 -> 1 -> 0 rounded up") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("half goes before a fixed amount, whatever the battlefield order: 3 -> 1 -> 0") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward One")
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("the recipient filter scopes it: the host's controller only") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        d.bolt(d.player1)
        d.getLifeTotal(d.player1) shouldBe 17
    }

    test("combat damage is halved too") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Ward Up")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3
        d.removeSummoningSickness(courser)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), defendingPlayer = d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 19
    }
})
