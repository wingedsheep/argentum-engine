package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.GevScaledScorch
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.PawpatchRecruit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Gev, Scaled Scorch.
 *
 * Gev, Scaled Scorch: {B}{R}
 * Legendary Creature — Lizard Mercenary
 * 3/2
 * Ward—Pay 2 life.
 * Other creatures you control enter with an additional +1/+1 counter on them
 * for each opponent who lost life this turn.
 * Whenever you cast a Lizard spell, Gev deals 1 damage to target opponent.
 */
class GevScaledScorchTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GevScaledScorch, PawpatchRecruit))
        return driver
    }

    test("Lizard spell trigger deals 1 damage to target opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Gev, Scaled Scorch")

        // Cast a Lizard creature — Gev is a Lizard, so casting another Gev (legendary rule aside)
        // would trigger. But we can use Gev itself as our Lizard spell source.
        // Actually, we need another Lizard spell to cast. Let's just use Gev as the trigger test.
        // The trigger fires when we cast a Lizard spell.
        // For simplicity, cast Gev from hand (it will fail to legend rule, but trigger should fire first)
        val gev2 = driver.putCardInHand(player1, "Gev, Scaled Scorch")
        driver.giveMana(player1, Color.BLACK, 1)
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, gev2)

        // Trigger should be on the stack (auto-targets only opponent)
        // Resolve the trigger
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Player2 took 1 damage from trigger
        driver.getLifeTotal(player2) shouldBe 19
    }

    test("creatures enter with counters when opponent lost life this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Gev, Scaled Scorch")

        // Deal damage to opponent so they have LifeLostThisTurnComponent
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.getLifeTotal(player2) shouldBe 17

        // Now cast a creature — it should enter with a +1/+1 counter
        val bear = driver.putCardInHand(player1, "Grizzly Bears")
        driver.giveMana(player1, Color.GREEN, 2)
        driver.castSpell(player1, bear)
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Find the bear on the battlefield and check counters
        val bearOnBf = driver.getCreatures(player1).first { entity ->
            driver.state.getEntity(entity)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == "Grizzly Bears"
        }
        val counters = driver.state.getEntity(bearOnBf)?.get<CountersComponent>()
        val plusOneCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusOneCounters shouldBe 1
    }

    test("creatures do NOT enter with counters when no opponent lost life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Gev, Scaled Scorch")

        // Cast a creature without any opponent losing life
        val bear = driver.putCardInHand(player1, "Grizzly Bears")
        driver.giveMana(player1, Color.GREEN, 2)
        driver.castSpell(player1, bear)
        driver.passPriority(player1)
        driver.passPriority(player2)

        val bearOnBf = driver.getCreatures(player1).first { entity ->
            driver.state.getEntity(entity)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == "Grizzly Bears"
        }
        val counters = driver.state.getEntity(bearOnBf)?.get<CountersComponent>()
        val plusOneCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusOneCounters shouldBe 0
    }

    test("Offspring token copy enters with a +1/+1 counter from Gev") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Gev, Scaled Scorch")

        // Make player2 lose life so Gev's "for each opponent who lost life" trigger tracker fires.
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.getLifeTotal(player2) shouldBe 17

        // Cast Pawpatch Recruit with Offspring (kicker) so its ETB creates a 1/1 token copy.
        val recruit = driver.putCardInHand(player1, "Pawpatch Recruit")
        driver.giveMana(player1, Color.GREEN, 3) // {G} base + {2} offspring
        val castResult = driver.submit(
            CastSpell(
                playerId = player1,
                cardId = recruit,
                wasKicked = true,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        castResult.isSuccess shouldBe true

        // Resolve the Pawpatch Recruit spell so it enters the battlefield.
        driver.passPriority(player1)
        driver.passPriority(player2)
        // Resolve the Offspring ETB trigger (creates the 1/1 token copy).
        if (driver.stackSize > 0) {
            driver.passPriority(player1)
            driver.passPriority(player2)
        }

        // Find the token copy (a creature named "Pawpatch Recruit" that has TokenComponent).
        val token = driver.getCreatures(player1).first { entity ->
            val container = driver.state.getEntity(entity) ?: return@first false
            val card = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            card?.name == "Pawpatch Recruit" && container.get<TokenComponent>() != null
        }

        val counters = driver.state.getEntity(token)?.get<CountersComponent>()
        val plusOneCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusOneCounters shouldBe 1
    }
})
