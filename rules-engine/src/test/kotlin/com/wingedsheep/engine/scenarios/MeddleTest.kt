package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ChangeSpellTargetEffect
import com.wingedsheep.sdk.scripting.targets.TargetSpell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Meddle (ONS)
 * {1}{U} Instant
 * "If target spell has only one target and that target is a creature,
 * change that spell's target to another creature."
 */
class MeddleTest : FunSpec({

    val Meddle = CardDefinition(
        name = "Meddle",
        manaCost = ManaCost.parse("{1}{U}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "If target spell has only one target and that target is a creature, change that spell's target to another creature.",
        script = CardScript.spell(
            effect = ChangeSpellTargetEffect(),
            TargetSpell()
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Meddle))
        return driver
    }

    test("Meddle redirects a single-target creature spell to another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Setup: put creatures on battlefield
        val bear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        val lion = driver.putPermanentOnBattlefield(activePlayer, "Savannah Lions")

        // Opponent has Lightning Bolt and active player has Meddle
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val meddle = driver.putCardInHand(activePlayer, "Meddle")
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 2) // {1}{U}

        // Active player passes priority
        driver.passPriority(activePlayer)

        // Opponent casts Lightning Bolt targeting Grizzly Bears
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Opponent passes priority
        driver.passPriority(opponent)

        // Active player casts Meddle targeting the bolt on stack
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = meddle,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — Meddle resolves, presenting a choice of new creature target
        driver.bothPass()

        // Meddle's controller (activePlayer) chooses Savannah Lions as the new target
        driver.submitCardSelection(activePlayer, listOf(lion))

        // Stack should now have just the bolt with changed target
        driver.stackSize shouldBe 1

        // Both pass — bolt resolves, dealing damage to Savannah Lions instead
        driver.bothPass()

        driver.stackSize shouldBe 0

        // Grizzly Bears should still be alive (was original target, but got redirected)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null

        // Savannah Lions (2/1) took 3 damage and should be dead
        driver.findPermanent(activePlayer, "Savannah Lions") shouldBe null
        driver.assertInGraveyard(activePlayer, "Savannah Lions")
    }

    test("Meddle does nothing if spell targets a player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield so there is a legal creature
        driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")

        // Opponent has Lightning Bolt targeting a player, active player has Meddle
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val meddle = driver.putCardInHand(activePlayer, "Meddle")
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Active player passes priority
        driver.passPriority(activePlayer)

        // Opponent casts bolt targeting activePlayer (a player, not a creature)
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                targets = listOf(ChosenTarget.Player(activePlayer)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val boltOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Opponent passes
        driver.passPriority(opponent)

        // Active player casts Meddle targeting the bolt
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = meddle,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — Meddle resolves but does nothing (target is a player, not a creature)
        driver.bothPass()

        // No decision should be presented — Meddle's "if" condition failed
        driver.stackSize shouldBe 1

        // Both pass — bolt resolves normally targeting the player
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.getLifeTotal(activePlayer) shouldBe 17 // Took 3 damage
    }
})
