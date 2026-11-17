package com.wingedsheep.engine

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `PreventDamage(amount = <DynamicAmount>)` — the standing "prevent X of that damage, where X is …"
 * prevention effect, with a synthetic host so the primitive doesn't move with Shield of the Avatar
 * (covered in `ShieldOfTheAvatarScenarioTest`). X is read as the damage would be dealt, with the
 * host as the source and its controller as "you"; X = 0 prevents nothing.
 */
class DynamicPreventDamageReplacementTest : FunSpec({

    val ward: CardDefinition = card("Test Crowd Ward") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to you, prevent X of that damage, where X is the number of creatures you control."
        replacementEffect(
            PreventDamage(
                amount = DynamicAmounts.creaturesYouControl(),
                appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)
            )
        )
    }
    val wardOne: CardDefinition = card("Test Ward One") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to you, prevent 1 of that damage."
        replacementEffect(
            PreventDamage(amount = DynamicAmounts.fixed(1), appliesTo = EventPattern.DamageEvent(recipient = Recipient.You))
        )
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ward, wardOne))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        castSpell(player1, putCardInHand(player1, "Lightning Bolt"), listOf(target)).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    test("X = 0: no creatures, nothing is prevented") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("X counts the host controller's creatures: two creatures prevent 2 of 3") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears") // the opponent's don't count
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("X is read when the damage would be dealt, not when the host entered") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 17
        repeat(3) { d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") }
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("X larger than the damage prevents all of it, never more") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        repeat(5) { d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") }
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("stacks with a fixed prevention; the ordering prompt shows X as evaluated: 3 - 2 - 1 = 0") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        d.putPermanentOnBattlefield(d.player2, "Test Ward One")
        repeat(2) { d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") }
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Lightning Bolt"), listOf(d.player2)).error shouldBe null
        var passes = 0
        while (d.stackSize > 0 && d.pendingDecision == null && passes++ < 20) d.bothPass()

        // CR 616.1: the affected player orders the two prevention effects.
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.playerId shouldBe d.player2
        val crowd = choice.options.indexOfFirst { it.contains("Test Crowd Ward") }
        choice.options[crowd] shouldContain "prevent 2 damage"
        d.submitDecision(d.player2, OptionChosenResponse(choice.id, crowd)).error shouldBe null
        passes = 0
        while (d.stackSize > 0 && d.pendingDecision == null && passes++ < 20) d.bothPass()

        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("combat damage is reduced by X too") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Crowd Ward")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3
        d.removeSummoningSickness(courser)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(courser), defendingPlayer = d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(d.player2) shouldBe 18
    }
})
