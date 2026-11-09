package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The madness trigger's cast announces X while it resolves (CR 702.35b / 601.2b), so the AI meets a
 * `ChooseNumberDecision` for an `{X}` madness cost (From Under the Floorboards' {X}{B}{B}). It must
 * spend the mana it has on X rather than take the first legal answer, X = 0.
 */
class MadnessXChoiceAiTest : FunSpec({

    // "Create a 2/2 Zombie; if this spell's madness cost was paid, create X of them instead."
    val madnessHorde = card("Madness Horde") {
        manaCost = "{3}{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(
                Conditions.MadnessCostWasPaid,
                then = Effects.CreateToken(
                    count = DynamicAmounts.xValue(), power = 2, toughness = 2,
                    colors = setOf(Color.BLACK), creatureTypes = setOf("Zombie"),
                ),
                otherwise = Effects.CreateToken(
                    power = 2, toughness = 2, colors = setOf(Color.BLACK), creatureTypes = setOf("Zombie"),
                ),
            )
        }
        madness("{X}{B}")
    }

    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    test("the AI spends its mana on X for an {X} madness cost") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(madnessHorde, selfDiscard))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!

        val horde = d.putCardInHand(p, "Madness Horde")
        val discard = d.putCardInHand(p, "Self Discard")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(CastSpell(p, discard, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitCardSelection(p, listOf(horde))
        var guard = 0
        while (d.state.stack.isNotEmpty() && d.state.pendingDecision == null && guard++ < 20) d.bothPass()

        d.giveMana(p, Color.BLACK, 4) // {X}{B} with X up to 3
        d.submitYesNo(p, true)
        val ask = d.state.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        ask.maxValue shouldBe 3

        val ai = AIPlayer.create(d.cardRegistry, p)
        val answer = ai.respondToDecision(d.state, ask).shouldBeInstanceOf<NumberChosenResponse>()
        answer.number shouldBe 3
    }
})
