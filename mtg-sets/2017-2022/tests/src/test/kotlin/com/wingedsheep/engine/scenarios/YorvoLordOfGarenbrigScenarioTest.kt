package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.YorvoLordOfGarenbrig
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Yorvo, Lord of Garenbrig (ELD #185).
 *
 * Pins the 2019-10-04 rulings: the power comparison happens only *after* the first +1/+1 counter
 * (a creature whose power merely ties Yorvo's new power earns no second counter), and an entering
 * creature that dies before the trigger resolves is compared by its last-known power.
 */
class YorvoLordOfGarenbrigScenarioTest : FunSpec({

    val fiveFive = CardDefinition.creature(
        name = "Test Garenbrig Five",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Giant")),
        power = 5,
        toughness = 5,
    )
    val sixThree = CardDefinition.creature(
        name = "Test Garenbrig Six",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Giant")),
        power = 6,
        toughness = 3,
    )
    val redGiant = CardDefinition.creature(
        name = "Test Embereth Giant",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 9,
        toughness = 9,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(YorvoLordOfGarenbrig, fiveFive, sixThree, redGiant))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Yorvo already on the battlefield with its four entry counters (power 4). */
    fun GameTestDriver.yorvoInPlay(you: EntityId): EntityId {
        val yorvo = putCreatureOnBattlefield(you, YorvoLordOfGarenbrig.name)
        addComponent(yorvo, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
        return yorvo
    }

    fun GameTestDriver.castCreature(you: EntityId, name: String, color: Color): EntityId {
        val card = putCardInHand(you, name)
        giveMana(you, color, 1)
        castSpell(you, card).error shouldBe null
        bothPass() // creature resolves; Yorvo's trigger (if any) goes on the stack
        return card
    }

    test("Yorvo enters with four +1/+1 counters") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val yorvo = driver.putCardInHand(you, YorvoLordOfGarenbrig.name)
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, yorvo).error shouldBe null
        driver.bothPass()

        driver.findPermanent(you, YorvoLordOfGarenbrig.name) shouldBe yorvo
        driver.plusOnes(yorvo) shouldBe 4
        driver.state.projectedState.getPower(yorvo) shouldBe 4
    }

    test("a green creature with greater power than Yorvo after the first counter gives a second counter") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val yorvo = driver.yorvoInPlay(you)

        driver.castCreature(you, sixThree.name, Color.GREEN)
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        // 4 -> 5 from the first counter; 6 > 5, so a second counter.
        driver.plusOnes(yorvo) shouldBe 6
    }

    test("the comparison happens after the first counter — a tie gives no second counter") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val yorvo = driver.yorvoInPlay(you)

        driver.castCreature(you, fiveFive.name, Color.GREEN)
        driver.bothPass()

        // 5 > 4 before the counter, but 5 is not greater than Yorvo's new power of 5.
        driver.plusOnes(yorvo) shouldBe 5
    }

    test("a creature that died before the trigger resolved is compared by its last-known power") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val yorvo = driver.yorvoInPlay(you)

        val six = driver.castCreature(you, sixThree.name, Color.GREEN)
        driver.state.stack.size shouldBe 1

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(six)).error shouldBe null
        driver.bothPass() // Bolt resolves; the 6/3 dies
        driver.findPermanent(you, sixThree.name) shouldBe null

        driver.bothPass() // Yorvo's trigger resolves
        driver.plusOnes(yorvo) shouldBe 6
    }

    test("a non-green creature entering does not trigger Yorvo") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val yorvo = driver.yorvoInPlay(you)

        driver.castCreature(you, redGiant.name, Color.RED)
        driver.state.stack.size shouldBe 0
        driver.plusOnes(yorvo) shouldBe 4
    }
})
