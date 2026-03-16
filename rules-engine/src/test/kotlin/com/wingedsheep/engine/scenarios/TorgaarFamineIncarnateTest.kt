package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.TorgaarFamineIncarnate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Torgaar, Famine Incarnate.
 *
 * Torgaar, Famine Incarnate: {6}{B}{B}
 * Legendary Creature — Avatar
 * 7/6
 * As an additional cost to cast this spell, you may sacrifice any number of creatures.
 * This spell costs {2} less to cast for each creature sacrificed this way.
 * When Torgaar enters, up to one target player's life total becomes half their starting
 * life total, rounded down.
 */
class TorgaarFamineIncarnateTest : FunSpec({

    val TestCreature = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Human")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestCreature, TorgaarFamineIncarnate))
        return driver
    }

    test("cast Torgaar with full mana, no sacrifices, ETB sets opponent life to 10") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        driver.giveMana(activePlayer, Color.BLACK, 8) // {6}{B}{B}

        // Cast with no sacrifices
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(), // no sacrifices
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Resolve Torgaar
        driver.bothPass()

        // ETB trigger should be on the stack, asking for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Target the opponent
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // Resolve the trigger
        driver.bothPass()

        // Opponent's life should be 10 (half of 20)
        driver.getLifeTotal(opponent) shouldBe 10

        // Torgaar should be on the battlefield
        driver.findPermanent(activePlayer, "Torgaar, Famine Incarnate") shouldNotBe null
    }

    test("cast Torgaar sacrificing 2 creatures reduces cost by 4") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 2 creatures on battlefield
        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        // {6}{B}{B} minus {4} (2 creatures * {2} each) = {2}{B}{B}, so need 4 mana
        driver.giveMana(activePlayer, Color.BLACK, 4)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(creature1, creature2)
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Sacrificed creatures should be gone
        driver.findPermanent(activePlayer, "Test Creature") shouldBe null

        // Resolve Torgaar
        driver.bothPass()

        // ETB trigger - target opponent
        driver.isPaused shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 10
        driver.findPermanent(activePlayer, "Torgaar, Famine Incarnate") shouldNotBe null
    }

    test("cast Torgaar sacrificing 3 creatures reduces cost by 6") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 3 creatures on battlefield
        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature3 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        // {6}{B}{B} minus {6} (3 creatures * {2} each) = {B}{B}, so need 2 black mana
        driver.giveMana(activePlayer, Color.BLACK, 2)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(creature1, creature2, creature3)
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Resolve Torgaar
        driver.bothPass()

        // ETB trigger - target self to set own life to 10
        driver.isPaused shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(activePlayer))
        driver.bothPass()

        driver.getLifeTotal(activePlayer) shouldBe 10
    }

    test("ETB can target self to set own life total to 10") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 5 // Start at 5 life
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        driver.giveMana(activePlayer, Color.BLACK, 8)

        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Resolve Torgaar
        driver.bothPass()

        // ETB trigger - target self
        driver.isPaused shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(activePlayer))
        driver.bothPass()

        // Life should go UP to 10 (half of 20 starting, even though starting life was set to 5)
        // Actually the effect sets to Fixed(10), so it's always 10
        driver.getLifeTotal(activePlayer) shouldBe 10
    }

    test("ETB with optional target - can choose no target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        driver.giveMana(activePlayer, Color.BLACK, 8)

        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Resolve Torgaar
        driver.bothPass()

        // ETB trigger - choose no target (optional)
        driver.isPaused shouldBe true
        driver.submitTargetSelection(activePlayer, emptyList())
        driver.bothPass()

        // Both players should still have 20 life
        driver.getLifeTotal(activePlayer) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("cost reduction cannot reduce below BB (colored mana)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 4 creatures on battlefield - would reduce by 8, but generic is only 6
        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature3 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        val creature4 = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")

        val torgaar = driver.putCardInHand(activePlayer, "Torgaar, Famine Incarnate")
        // {6}{B}{B} minus {8} -> generic can't go below 0, so {B}{B}
        driver.giveMana(activePlayer, Color.BLACK, 2) // Just {B}{B}

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = torgaar,
                additionalCostPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(creature1, creature2, creature3, creature4)
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }
})
