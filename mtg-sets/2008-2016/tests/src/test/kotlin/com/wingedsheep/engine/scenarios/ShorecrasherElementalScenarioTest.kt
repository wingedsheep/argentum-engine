package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.ShorecrasherElemental
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shorecrasher Elemental (DTK) — "{U}: Exile this creature, then return it to the battlefield face
 * down under its owner's control." / "Megamorph {4}{U}".
 *
 * The blink is the part that depends on engine execution: the exile → face-down return from exile
 * must yield a 2/2 face-down permanent with no abilities, whose turn-up procedure is the card's own
 * megamorph cost — and turning it up that way puts the +1/+1 counter on it.
 */
class ShorecrasherElementalScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + ShorecrasherElemental)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val blinkId = ShorecrasherElemental.script.activatedAbilities.first().id

    test("blink returns it face down as a 2/2 that turns up for its megamorph cost with a +1/+1 counter") {
        val d = driver()
        val elemental = d.putCreatureOnBattlefield(d.player1, "Shorecrasher Elemental")

        d.giveMana(d.player1, Color.BLUE, 1)
        d.submit(
            ActivateAbility(d.player1, elemental, blinkId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (guard++ < 10 && d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.getExileCardNames(d.player1) shouldNotContain "Shorecrasher Elemental"
        val faceDown = d.getPermanents(d.player1).singleOrNull {
            d.state.getEntity(it)!!.has<FaceDownComponent>()
        }
        withClue("it came back face down under its owner's control") { faceDown shouldNotBe null }
        val projected = d.state.projectedState
        withClue("a face-down permanent is a 2/2") {
            projected.getPower(faceDown!!) shouldBe 2
            projected.getToughness(faceDown) shouldBe 2
        }

        withClue("a face-down permanent has none of the card's abilities") {
            d.giveMana(d.player1, Color.BLUE, 1)
            d.submit(
                ActivateAbility(d.player1, faceDown!!, blinkId, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldNotBe null
        }

        d.giveMana(d.player1, Color.BLUE, 1)
        d.giveColorlessMana(d.player1, 4)
        d.submit(TurnFaceUp(d.player1, faceDown!!, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null

        d.state.getEntity(faceDown)!!.has<FaceDownComponent>() shouldBe false
        d.findPermanent(d.player1, "Shorecrasher Elemental") shouldBe faceDown
        withClue("megamorph puts a +1/+1 counter on it") {
            d.state.getEntity(faceDown)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            d.state.projectedState.getPower(faceDown) shouldBe 4
            d.state.projectedState.getToughness(faceDown) shouldBe 4
        }
    }
})
