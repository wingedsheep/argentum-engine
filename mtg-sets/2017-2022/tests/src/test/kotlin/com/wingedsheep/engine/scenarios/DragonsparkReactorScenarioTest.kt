package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.DragonsparkReactor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Dragonspark Reactor (NEO #137) — "{4}, Sacrifice this artifact: It deals damage equal to the
 * number of charge counters on it to target player and that much damage to up to one target
 * creature."
 *
 * Sacrificing is a cost, so the charge counters are gone by resolution; the amount must come from
 * the pre-cost snapshot (last-known information). "Up to one" creature may be left unchosen.
 */
class DragonsparkReactorScenarioTest : FunSpec({

    val sacAbility = DragonsparkReactor.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DragonsparkReactor)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("sacrifice deals the pre-cost charge count to the player and to the creature") {
        val d = driver()
        val reactor = d.putPermanentOnBattlefield(d.player1, "Dragonspark Reactor")
        d.addComponent(reactor, CountersComponent(mapOf(CounterType.CHARGE to 2)))
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 4)

        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = reactor,
                abilityId = sacAbility,
                targets = listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(d.player1, "Dragonspark Reactor") shouldBe null
        d.getLifeTotal(d.player2) shouldBe 18
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(d.player2) shouldContain "Grizzly Bears"
    }

    test("up to one target creature may be left unchosen") {
        val d = driver()
        val reactor = d.putPermanentOnBattlefield(d.player1, "Dragonspark Reactor")
        d.addComponent(reactor, CountersComponent(mapOf(CounterType.CHARGE to 3)))
        d.giveColorlessMana(d.player1, 4)

        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = reactor,
                abilityId = sacAbility,
                targets = listOf(ChosenTarget.Player(d.player2)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getLifeTotal(d.player2) shouldBe 17
        d.getLifeTotal(d.player1) shouldBe 20
    }
})
