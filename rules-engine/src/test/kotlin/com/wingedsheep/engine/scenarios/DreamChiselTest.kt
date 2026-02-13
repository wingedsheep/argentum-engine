package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.FaceDownSpellCostReduction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Dream Chisel.
 *
 * Dream Chisel: {2}
 * Artifact
 * Face-down creature spells you cast cost {1} less to cast.
 */
class DreamChiselTest : FunSpec({

    val DreamChisel = CardDefinition(
        name = "Dream Chisel",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine.artifact(),
        oracleText = "Face-down creature spells you cast cost {1} less to cast.",
        script = com.wingedsheep.sdk.model.CardScript(
            staticAbilities = listOf(FaceDownSpellCostReduction(CostReductionSource.Fixed(1)))
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DreamChisel))
        return driver
    }

    test("Dream Chisel reduces face-down casting cost from 3 to 2") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dream Chisel on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")

        // Put a morph creature in hand
        val morphCard = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give exactly 2 mana (reduced from 3)
        driver.giveMana(activePlayer, Color.WHITE, 2)

        // Should be able to cast face-down for {2}
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = morphCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        result.isSuccess shouldBe true

        // Spell is on the stack
        driver.stackSize shouldBe 1
    }

    test("face-down creature costs full 3 without Dream Chisel") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No Dream Chisel on battlefield

        // Put a morph creature in hand
        val morphCard = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give exactly 2 mana (not enough without reduction)
        driver.giveMana(activePlayer, Color.WHITE, 2)

        // Should NOT be able to cast face-down for {2} — needs {3}
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = morphCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }

    test("two Dream Chisels reduce face-down cost from 3 to 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two Dream Chisels on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")

        // Put a morph creature in hand
        val morphCard = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give exactly 1 mana (reduced from 3 by 2)
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Should be able to cast face-down for {1}
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = morphCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("three Dream Chisels reduce face-down cost to 0") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put three Dream Chisels on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")

        // Put a morph creature in hand
        val morphCard = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give no mana — cost is fully reduced
        // Should be able to cast face-down for {0}
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = morphCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("Dream Chisel does not affect normal spell costs") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dream Chisel on battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Dream Chisel")

        // Put a morph creature in hand — try to cast it normally (not face-down)
        val morphCard = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give exactly 2 mana — Morph Test Creature costs {2}{W} normally
        driver.giveMana(activePlayer, Color.WHITE, 2)

        // Should NOT be able to cast normally for {2} — Dream Chisel only reduces face-down cost
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = morphCard,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
