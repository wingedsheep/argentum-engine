package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the auto-generated ISD cards that sit in the engine's known-risky space
 * (see mtgish-tooling's creator's note): additional sacrifice costs paid at cast time
 * (Altar's Reap, Infernal Plunge) and a counterspell carrying a second, non-spell target
 * (Lost in the Mist — no other implemented card pairs CounterEffect with a second target).
 */
class IsdAutogenRiskyShapesTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)  // the full catalog — includes the ISD cards under test
        return driver
    }

    test("Altar's Reap — sacrificing a creature at cast time draws two cards") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.giveMana(p1, Color.BLACK, 1)
        d.giveColorlessMana(p1, 1)

        val handSizeBefore = d.state.getHand(p1).size
        val spell = d.putCardInHand(p1, "Altar's Reap")

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // The additional cost is paid on cast (CR 601.2h) — the creature is gone before resolution.
        d.findPermanent(p1, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(p1).contains("Grizzly Bears") shouldBe true

        d.bothPass()

        // Net hand: +1 put, -1 cast, +2 drawn.
        d.state.getHand(p1).size shouldBe (handSizeBefore + 2)
    }

    test("Altar's Reap — cannot be cast without a creature to sacrifice") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(p1, Color.BLACK, 1)
        d.giveColorlessMana(p1, 1)
        val spell = d.putCardInHand(p1, "Altar's Reap")

        val result = d.submit(
            CastSpell(playerId = p1, cardId = spell, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("Infernal Plunge — sacrificing a creature adds {R}{R}{R} usable in the same main phase") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.giveMana(p1, Color.RED, 1)

        val spell = d.putCardInHand(p1, "Infernal Plunge")
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = spell,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        d.findPermanent(p1, "Grizzly Bears") shouldBe null

        d.bothPass()

        // The sorcery resolved into three red mana that survives in the pool for the main phase.
        val pool = d.state.getEntity(p1)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.red shouldBe 3
    }

    test("Lost in the Mist — counters the targeted spell and bounces the second target") {
        val d = createDriver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The permanent the second target will bounce.
        val giant = d.putCreatureOnBattlefield(p1, "Hill Giant")

        // p1 casts a creature spell for Lost in the Mist to counter.
        val bears = d.putCardInHand(p1, "Grizzly Bears")
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        d.submit(
            CastSpell(playerId = p1, cardId = bears, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        d.stackSize shouldBe 1
        d.passPriority(p1)

        // p2 responds: counter the Bears spell (t1) and bounce the Hill Giant (t2).
        val bearsOnStack = d.getTopOfStack()!!
        val mist = d.putCardInHand(p2, "Lost in the Mist")
        d.giveMana(p2, Color.BLUE, 2)
        d.giveColorlessMana(p2, 3)
        d.submit(
            CastSpell(
                playerId = p2,
                cardId = mist,
                targets = listOf(ChosenTarget.Spell(bearsOnStack), ChosenTarget.Permanent(giant)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        d.stackSize shouldBe 2

        d.bothPass()
        d.bothPass()

        // The Bears spell was countered (graveyard, never battlefield); the Giant went to hand.
        d.stackSize shouldBe 0
        d.findPermanent(p1, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(p1).contains("Grizzly Bears") shouldBe true
        d.findPermanent(p1, "Hill Giant") shouldBe null
        val handNames = d.state.getHand(p1).mapNotNull { id ->
            d.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
        }
        handNames.contains("Hill Giant") shouldBe true
    }
})
