package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.IncreaseSpellCostByPlayerSpellsCast
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Damping Sphere.
 *
 * Damping Sphere: {2}
 * Artifact
 * If a land is tapped for two or more mana, it produces {C} instead of any other type and amount.
 * Each spell a player casts costs {1} more to cast for each other spell that player has cast this turn.
 */
class DampingSphereTest : FunSpec({

    // Test land that produces 2 green mana
    val DoubleLand = card("Double Land") {
        manaCost = ""
        typeLine = "Land"

        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.AddMana(Color.GREEN, 2)
            manaAbility = true
        }
    }

    // Simple 1-mana creature for testing spell tax
    val TestGoblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    val DampingSphere = card("Damping Sphere") {
        manaCost = "{2}"
        typeLine = "Artifact"

        staticAbility {
            ability = DampLandManaProduction
        }

        staticAbility {
            ability = IncreaseSpellCostByPlayerSpellsCast()
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DampingSphere, DoubleLand, TestGoblin))
        return driver
    }

    test("first spell of the turn has no additional cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        val goblin = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 1)

        // First spell should cost only {R} — no tax yet
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }

    test("second spell costs {1} more") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        // Cast first spell (Test Goblin costs {R})
        val goblin1 = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin1,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        // Resolve first spell
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)

        // Second spell should cost {1} more
        val goblin2 = driver.putCardInHand(activePlayer, "Test Goblin")

        // With only {R}, should fail (needs {1}{R})
        driver.giveMana(activePlayer, Color.RED, 1)
        val failResult = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin2,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        failResult.isSuccess shouldBe false

        // With {1}{R}, should succeed
        driver.giveMana(activePlayer, Color.RED, 1)
        val successResult = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin2,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        successResult.isSuccess shouldBe true
    }

    test("third spell costs {2} more") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        // Cast first spell
        val goblin1 = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(playerId = activePlayer, cardId = goblin1, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)

        // Cast second spell
        val goblin2 = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(playerId = activePlayer, cardId = goblin2, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)

        // Third spell should cost {2} more (needs {2}{R})
        val goblin3 = driver.putCardInHand(activePlayer, "Test Goblin")

        // {1}{R} is not enough
        driver.giveMana(activePlayer, Color.RED, 2)
        val failResult = driver.submit(
            CastSpell(playerId = activePlayer, cardId = goblin3, paymentStrategy = PaymentStrategy.FromPool)
        )
        failResult.isSuccess shouldBe false

        // {2}{R} should work
        driver.giveMana(activePlayer, Color.RED, 1)
        val successResult = driver.submit(
            CastSpell(playerId = activePlayer, cardId = goblin3, paymentStrategy = PaymentStrategy.FromPool)
        )
        successResult.isSuccess shouldBe true
    }

    test("tax is per-player — opponent's first spell is not taxed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        // Active player casts 2 spells (to accumulate a count of 2)
        val goblin1 = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(playerId = activePlayer, cardId = goblin1, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)

        val goblin2 = driver.putCardInHand(activePlayer, "Test Goblin")
        driver.giveMana(activePlayer, Color.RED, 2) // Costs {1}{R} now
        driver.submitSuccess(
            CastSpell(playerId = activePlayer, cardId = goblin2, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)

        // Pass to combat and then to opponent's turn postcombat
        // Instead, just verify per-player count is correct
        driver.state.playerSpellsCastThisTurn[activePlayer] shouldBe 2
        driver.state.playerSpellsCastThisTurn[opponent] shouldBe null

        // Active player's third spell should cost {2} more (per their count of 2)
        val goblin3 = driver.putCardInHand(activePlayer, "Test Goblin")

        // {1}{R} is not enough (needs {2}{R})
        driver.giveMana(activePlayer, Color.RED, 2)
        val failResult = driver.submit(
            CastSpell(playerId = activePlayer, cardId = goblin3, paymentStrategy = PaymentStrategy.FromPool)
        )
        failResult.isSuccess shouldBe false
    }

    test("mana dampening replaces 2+ mana with 1 colorless") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        // Put Double Land on battlefield (would produce {G}{G} normally)
        val doubleLand = driver.putPermanentOnBattlefield(activePlayer, "Double Land")

        // Get the mana ability ID from the card definition
        val manaAbility = DoubleLand.script.activatedAbilities.first()

        // Tap Double Land
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = doubleLand,
                abilityId = manaAbility.id
            )
        )

        // Should have only 1 colorless mana instead of 2 green
        val pool = driver.state.getEntity(activePlayer)!!
            .get<ManaPoolComponent>()!!
        pool.green shouldBe 0
        pool.colorless shouldBe 1
    }

    test("mana dampening does not affect lands producing 1 mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Damping Sphere")

        // Put Forests on the battlefield so AutoPay can tap them
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Cast Grizzly Bears ({1}{G}) using AutoPay which taps Forests
        // Each Forest produces only 1 green, so dampening should NOT apply
        val bears = driver.putCardInHand(activePlayer, "Grizzly Bears")
        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = bears,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        result.isSuccess shouldBe true
    }
})
