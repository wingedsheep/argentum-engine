package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Willbender / ChangeTargetEffect
 * "Change the target of target spell or ability with a single target."
 */
class WillbenderTest : FunSpec({

    // Simplified Willbender-like card for testing (instant for easier test setup)
    val Deflect = CardDefinition(
        name = "Deflect",
        manaCost = ManaCost.parse("{1}{U}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "Change the target of target spell or ability with a single target.",
        script = CardScript.spell(
            effect = ChangeTargetEffect,
            TargetObject(filter = TargetFilter.SpellOrAbilityOnStack)
        )
    )

    // A no-targets sorcery for testing
    val SimpleWrath = CardDefinition(
        name = "Simple Wrath",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        typeLine = TypeLine.parse("Sorcery"),
        oracleText = "Destroy all creatures.",
        script = CardScript.spell(
            effect = com.wingedsheep.sdk.scripting.effects.DestroyAllEffect(
                filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Companion.Creature
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Deflect, SimpleWrath))
        return driver
    }

    test("ChangeTarget redirects a single-target creature spell to another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Setup: put creatures on battlefield
        val bear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        val lion = driver.putPermanentOnBattlefield(activePlayer, "Savannah Lions")

        // Opponent has Lightning Bolt, active player has Deflect
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val deflect = driver.putCardInHand(activePlayer, "Deflect")
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 2)

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

        // Active player casts Deflect targeting the bolt on stack
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = deflect,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — Deflect resolves, presenting a choice of new target
        driver.bothPass()

        // Deflect's controller chooses Savannah Lions as the new target
        driver.submitCardSelection(activePlayer, listOf(lion))

        // Stack should now have just the bolt with changed target
        driver.stackSize shouldBe 1

        // Both pass — bolt resolves, dealing damage to Savannah Lions
        driver.bothPass()

        driver.stackSize shouldBe 0

        // Grizzly Bears should still be alive (was original target, but got redirected)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null

        // Savannah Lions (2/1) took 3 damage and should be dead
        driver.findPermanent(activePlayer, "Savannah Lions") shouldBe null
        driver.assertInGraveyard(activePlayer, "Savannah Lions")
    }

    test("ChangeTarget redirects a player-targeting spell to another player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has Lightning Bolt targeting activePlayer
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val deflect = driver.putCardInHand(activePlayer, "Deflect")
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 2)

        driver.passPriority(activePlayer)

        // Opponent casts bolt targeting activePlayer
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = bolt,
                targets = listOf(ChosenTarget.Player(activePlayer)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        // Active player casts Deflect targeting the bolt
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = deflect,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.bothPass()

        // Choose opponent as the new target
        driver.submitCardSelection(activePlayer, listOf(opponent))

        // Bolt resolves targeting opponent instead
        driver.bothPass()

        driver.stackSize shouldBe 0
        driver.getLifeTotal(activePlayer) shouldBe 20 // No damage taken
        driver.getLifeTotal(opponent) shouldBe 17 // Took 3 damage
    }

    test("ChangeTarget does nothing if spell has no targets") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield for Simple Wrath to destroy
        driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        // Active player casts a spell with no targets (Simple Wrath)
        val wrath = driver.putCardInHand(activePlayer, "Simple Wrath")
        val deflect = driver.putCardInHand(opponent, "Deflect")
        driver.giveMana(activePlayer, Color.WHITE, 4)
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = wrath,
                targets = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        val wrathOnStack = driver.getTopOfStack()!!
        driver.passPriority(activePlayer)

        // Opponent casts Deflect targeting Wrath
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = deflect,
                targets = listOf(ChosenTarget.Spell(wrathOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Both pass — Deflect resolves but does nothing (no targets on Wrath)
        driver.bothPass()

        // No decision should be presented — spell has no targets
        driver.stackSize shouldBe 1

        // Wrath resolves normally
        driver.bothPass()

        driver.stackSize shouldBe 0
    }
})
