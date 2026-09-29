package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Convoke and delve paying toward {X}.
 *
 * CR 601.2f: the total cost of a spell includes the announced X as generic mana. CR 702.51a lets
 * each convoked creature pay {1} (or one mana of its colour) of that total cost, and delve likewise
 * pays generic mana of the total cost — so a tap or exile beyond the printed generic pays the X.
 * The effect's X is the announced value regardless of how it was paid.
 *
 * Covers the payment side (`CastCostTotaller.paymentXValue`) and the legal-action X ceiling
 * (`CastSpellEnumerator`'s `maxAffordableX`), which must count convoke creatures without
 * double-counting a creature that is also a mana source.
 */
class ConvokePaysXTest : FunSpec({

    val convokeDrawX = card("Convoke Draw X") {
        manaCost = "{X}{U}{U}"
        typeLine = "Instant"
        oracleText = "Convoke\nDraw X cards."
        keywords(Keyword.CONVOKE)
        spell { effect = Effects.DrawCards(DynamicAmounts.xValue()) }
    }

    val convokeGenericDrawX = card("Convoke Generic Draw X") {
        manaCost = "{X}{1}{U}"
        typeLine = "Instant"
        oracleText = "Convoke\nDraw X cards."
        keywords(Keyword.CONVOKE)
        spell { effect = Effects.DrawCards(DynamicAmounts.xValue()) }
    }

    val delveDrawX = card("Delve Draw X") {
        manaCost = "{X}{B}"
        typeLine = "Instant"
        oracleText = "Delve\nDraw X cards."
        keywords(Keyword.DELVE)
        spell { effect = Effects.DrawCards(DynamicAmounts.xValue()) }
    }

    val blueBird = card("Blue Test Bird") {
        manaCost = "{U}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(convokeDrawX, convokeGenericDrawX, delveDrawX, blueBird))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castX(
        card: EntityId,
        x: Int,
        convoke: Map<EntityId, Color?> = emptyMap(),
        delve: List<EntityId> = emptyList(),
    ) = submit(
        CastSpell(
            playerId = player1,
            cardId = card,
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = AlternativePaymentChoice(
                convokedCreatures = convoke.mapValues { ConvokePayment(color = it.value) },
                delvedCards = delve,
            ),
        )
    )

    fun GameTestDriver.maxX(card: EntityId): Int? =
        legalActions(player1)
            .firstOrNull { (it.action as? CastSpell)?.cardId == card }
            ?.maxAffordableX

    test("generic convoke taps beyond the printed generic pay the X") {
        val driver = newDriver()
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val bears = (1..3).map { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")
        val handBefore = driver.getHand(driver.player1).size

        driver.castX(spell, x = 3, convoke = bears.associateWith { null }).outcome shouldBe Outcome.Done
        driver.bothPass()

        // -1 for the cast spell, +3 drawn.
        driver.getHand(driver.player1).size shouldBe handBefore - 1 + 3
        bears.forEach { driver.isTapped(it) shouldBe true }
        driver.getUntappedLands(driver.player1).size shouldBe 0
    }

    test("convoke taps and lands can split the X") {
        val driver = newDriver()
        repeat(4) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")
        val handBefore = driver.getHand(driver.player1).size

        driver.castX(spell, x = 3, convoke = mapOf(bear to null)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(driver.player1).size shouldBe handBefore + 2
        driver.getUntappedLands(driver.player1).size shouldBe 0
    }

    test("the printed generic is paid before the X") {
        val driver = newDriver()
        driver.putLandOnBattlefield(driver.player1, "Island")
        val bears = (1..2).map { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val spell = driver.putCardInHand(driver.player1, "Convoke Generic Draw X")

        // One bear pays the printed {1}; nothing is left for X=1.
        driver.castX(spell, x = 1, convoke = mapOf(bears[0] to null)).outcome shouldNotBe Outcome.Done

        // Both bears: {1} and X=1.
        val handBefore = driver.getHand(driver.player1).size
        driver.castX(spell, x = 1, convoke = bears.associateWith { null }).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getHand(driver.player1).size shouldBe handBefore
    }

    test("a creature convoked for a coloured pip does not also pay the X") {
        val driver = newDriver()
        driver.putLandOnBattlefield(driver.player1, "Island")
        val bird = driver.putCreatureOnBattlefield(driver.player1, "Blue Test Bird")
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")

        // Island + bird (for {U}) cover {U}{U}; X=1 has nothing left to pay it.
        driver.castX(spell, x = 1, convoke = mapOf(bird to Color.BLUE)).outcome shouldNotBe Outcome.Done

        // The same bird tapped for {1} pays the X, but then {U}{U} is short.
        driver.castX(spell, x = 1, convoke = mapOf(bird to null)).outcome shouldNotBe Outcome.Done
    }

    test("without convoke the whole X is charged as mana") {
        val driver = newDriver()
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Island") }
        repeat(3) { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")

        driver.castX(spell, x = 3).outcome shouldNotBe Outcome.Done
    }

    test("the X ceiling counts each convoke creature as one more source") {
        val driver = newDriver()
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Island") }
        repeat(3) { driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") }
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")

        driver.maxX(spell) shouldBe 3
    }

    test("the X ceiling counts a mana-dork convoke creature once, not twice") {
        val driver = newDriver()
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Island") }
        val elves = driver.putCreatureOnBattlefield(driver.player1, "Llanowar Elves")
        driver.removeSummoningSickness(elves)
        val spell = driver.putCardInHand(driver.player1, "Convoke Draw X")

        // Two Islands pay {U}{U}; the elves pay one more — as mana or as a convoke tap, not both.
        driver.maxX(spell) shouldBe 1
    }

    test("delved cards beyond the printed generic pay the X, and the ceiling offers it") {
        val driver = newDriver()
        driver.putLandOnBattlefield(driver.player1, "Swamp")
        val graveyard = (1..3).map { driver.putCardInGraveyard(driver.player1, "Grizzly Bears") }
        val spell = driver.putCardInHand(driver.player1, "Delve Draw X")

        driver.maxX(spell) shouldBe 3

        val handBefore = driver.getHand(driver.player1).size
        driver.castX(spell, x = 3, delve = graveyard).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(driver.player1).size shouldBe handBefore + 2
        graveyard.forEach { (it in driver.getExile(driver.player1)) shouldBe true }
    }
})
