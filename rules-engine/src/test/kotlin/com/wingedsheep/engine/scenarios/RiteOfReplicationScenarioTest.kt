package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.zen.cards.RiteOfReplication
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for Rite of Replication (ZEN #61; reprinted in Foundations #711).
 *
 * "Kicker {5}. Create a token that's a copy of target creature. If this spell was kicked,
 * create five of those tokens instead."
 *
 * Pins the mutually-exclusive branch on the kicker: an un-kicked cast makes exactly one
 * copy, a kicked cast makes exactly five (not six).
 */
class RiteOfReplicationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RiteOfReplication)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.countCreatures(playerId: EntityId, name: String): Int =
        getCreatures(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("un-kicked, Rite of Replication creates one token copy of the target creature") {
        val driver = createDriver()
        val p1 = driver.player1

        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")
        val rite = driver.putCardInHand(p1, "Rite of Replication")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.giveColorlessMana(p1, 2)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = rite,
                targets = listOf(ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass() // resolve

        // Original + one copy = two.
        driver.countCreatures(p1, "Centaur Courser") shouldBe 2
    }

    test("kicked, Rite of Replication creates five token copies instead of one") {
        val driver = createDriver()
        val p1 = driver.player1

        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")
        val rite = driver.putCardInHand(p1, "Rite of Replication")
        // Kicked total cost is {7}{U}{U}.
        driver.giveMana(p1, Color.BLUE, 2)
        driver.giveColorlessMana(p1, 7)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = rite,
                targets = listOf(ChosenTarget.Permanent(courser)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass() // resolve

        // Original + five copies = six.
        driver.countCreatures(p1, "Centaur Courser") shouldBe 6
    }
})
