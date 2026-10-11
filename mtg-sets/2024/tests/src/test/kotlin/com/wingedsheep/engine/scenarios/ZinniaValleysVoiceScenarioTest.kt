package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.ZinniaValleysVoice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Zinnia, Valley's Voice {U}{R}{W} — Legendary Creature — Bird Bard 1/3
 *   Flying
 *   Zinnia gets +X/+0, where X is the number of other creatures you control with base power 1.
 *   Creature spells you cast gain offspring {2} as you cast them.
 *
 * The two halves feed each other: an offspring token is a 1/1 copy — base power 1 — so every
 * offspring paid through Zinnia also grows her.
 */
class ZinniaValleysVoiceScenarioTest : FunSpec({

    test("a creature cast with Zinnia's offspring makes a 1/1 copy, and the copy pumps Zinnia") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ZinniaValleysVoice)
        d.initMirrorMatch(Deck.of("Forest" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = d.activePlayer!!

        val zinnia = d.putCreatureOnBattlefield(p1, "Zinnia, Valley's Voice")
        withClue("no other base-power-1 creatures yet") { d.state.projectedState.getPower(zinnia) shouldBe 1 }

        val bears = d.putCardInHand(p1, "Grizzly Bears")
        d.giveMana(p1, Color.GREEN, 4)
        d.submit(
            CastSpell(p1, bears, paymentStrategy = PaymentStrategy.FromPool, declaredCostSlot = ChoiceSlot.OFFSPRING)
        ).error shouldBe null

        var guard = 0
        while (d.stackSize > 0 && guard++ < 20) d.bothPass()

        val grizzlies = d.getCreatures(p1).filter {
            d.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        grizzlies shouldHaveSize 2
        val token = grizzlies.single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.state.projectedState.getPower(token) shouldBe 1
        d.state.projectedState.getToughness(token) shouldBe 1

        withClue("the 1/1 token copy has base power 1; the 2/2 original doesn't") {
            d.state.projectedState.getPower(zinnia) shouldBe 2
            d.state.projectedState.getToughness(zinnia) shouldBe 3
        }
    }
})
