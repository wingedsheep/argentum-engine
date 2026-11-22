package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gpt.cards.Quicken
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quicken (GPT #31) — "The next sorcery spell you cast this turn can be cast as though it had flash.
 * Draw a card." The one-shot grant's rules are pinned in `NextSpellFlashGrantTest`; this pins the
 * card's wiring: it draws, flashes in exactly one sorcery, and leaves the next one at sorcery speed.
 */
class QuickenScenarioTest : FunSpec({

    val sorcery = CardDefinition.sorcery(
        name = "Quicken Test Sorcery",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = Effects.DrawCards(1))
    )

    test("Quicken draws a card and lets one sorcery be cast during the end step") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Quicken, sorcery))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        val p1 = d.player1
        d.passPriorityUntil(Step.END)

        val quicken = d.putCardInHand(p1, "Quicken")
        val first = d.putCardInHand(p1, "Quicken Test Sorcery")
        val second = d.putCardInHand(p1, "Quicken Test Sorcery")
        val handBefore = d.getHand(p1).size
        d.giveMana(p1, Color.BLUE, 1)
        d.submitSuccess(CastSpell(p1, quicken, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.getHand(p1).size shouldBe handBefore // Quicken left, one card drawn

        d.giveMana(p1, Color.RED, 2)
        d.submit(CastSpell(p1, first, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submit(CastSpell(p1, second, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
    }
})
