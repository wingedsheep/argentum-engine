package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class FestivalOfEmbersTest : FunSpec({

    val bolt = card("Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"

        spell {
            effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
            target = AnyTarget()
        }
    }

    test("instant cast from graveyard via Festival of Embers is exiled on resolve") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(bolt)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30))

        val p1 = driver.player1

        driver.putPermanentOnBattlefield(p1, "Festival of Embers")
        val boltId = driver.putCardInGraveyard(p1, "Test Bolt")
        driver.putLandOnBattlefield(p1, "Mountain")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = boltId,
                targets = listOf(ChosenTarget.Player(driver.player2)),
                graveyardLifeCost = 1,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        driver.getExile(p1).shouldContain(boltId)
        driver.getGraveyard(p1).shouldNotContain(boltId)
    }

    test("modal sorcery (Agate Assault) cast from graveyard via Festival of Embers is exiled on resolve") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30))

        val p1 = driver.player1
        val p2 = driver.player2

        driver.putPermanentOnBattlefield(p1, "Festival of Embers")
        val agateId = driver.putCardInGraveyard(p1, "Agate Assault")
        repeat(3) { driver.putLandOnBattlefield(p1, "Mountain") }
        val bearId = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = agateId,
                targets = listOf(ChosenTarget.Permanent(bearId)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bearId))),
                graveyardLifeCost = 1,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        driver.passPriority(p1)
        driver.passPriority(p2)

        driver.getExile(p1).shouldContain(agateId)
        driver.getGraveyard(p1).shouldNotContain(agateId)
    }

    test("instant cast from hand is exiled while Festival of Embers is out") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(bolt)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30))

        val p1 = driver.player1

        driver.putPermanentOnBattlefield(p1, "Festival of Embers")
        val boltId = driver.putCardInHand(p1, "Test Bolt")
        driver.putLandOnBattlefield(p1, "Mountain")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val result = driver.submit(
            CastSpell(
                playerId = p1,
                cardId = boltId,
                targets = listOf(ChosenTarget.Player(driver.player2)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true

        driver.passPriority(p1)
        driver.passPriority(driver.player2)

        driver.getExile(p1).shouldContain(boltId)
        driver.getGraveyard(p1).shouldNotContain(boltId)
    }
})
