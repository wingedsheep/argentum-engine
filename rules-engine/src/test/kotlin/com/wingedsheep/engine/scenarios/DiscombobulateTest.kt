package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.CounterSpellEffect
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Discombobulate.
 *
 * Discombobulate: {2}{U}{U}
 * Instant
 * Counter target spell. Look at the top four cards of your library,
 * then put them back in any order.
 */
class DiscombobulateTest : FunSpec({

    val Discombobulate = CardDefinition(
        name = "Discombobulate",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        typeLine = TypeLine.instant(),
        oracleText = "Counter target spell. Look at the top four cards of your library, then put them back in any order.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(CounterSpellEffect, LookAtTopAndReorderEffect(4))
            ),
            Targets.Spell
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Discombobulate)
        return driver
    }

    test("counters target spell and presents reorder decision") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent casts Lightning Bolt targeting active player
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))

        val spellOnStack = driver.getTopOfStack()!!
        driver.stackSize shouldBe 1

        // Opponent passes priority back so active player can respond
        driver.passPriority(opponent)

        // Active player casts Discombobulate targeting the bolt
        val discombobulate = driver.putCardInHand(activePlayer, "Discombobulate")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = discombobulate,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 2

        // Both pass — Discombobulate resolves
        driver.bothPass()

        // Bolt should be countered
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"

        // Should be paused for reorder decision (top 4 cards)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()

        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        reorderDecision.cards.size shouldBe 4

        // Submit the reorder (keep same order)
        driver.submitOrderedResponse(activePlayer, reorderDecision.cards)

        // Everything resolved
        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0

        // Active player took no damage
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("reorders library cards in chosen order") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Get top 4 cards before casting
        val libraryZone = ZoneKey(activePlayer, Zone.LIBRARY)
        val topFourBefore = driver.state.getZone(libraryZone).take(4)

        // Opponent casts a spell
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))

        val spellOnStack = driver.getTopOfStack()!!

        // Opponent passes priority back so active player can respond
        driver.passPriority(opponent)

        // Active player counters with Discombobulate
        val discombobulate = driver.putCardInHand(activePlayer, "Discombobulate")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = discombobulate,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.bothPass()

        // Submit reversed order
        driver.isPaused shouldBe true
        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        val reversedOrder = reorderDecision.cards.reversed()
        driver.submitOrderedResponse(activePlayer, reversedOrder)

        // Verify library is in the new order
        val libraryAfter = driver.state.getZone(libraryZone)
        libraryAfter.take(4) shouldBe reversedOrder
    }

    test("discombobulate goes to graveyard after resolving") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Mountain" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent casts a spell
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(activePlayer))

        val spellOnStack = driver.getTopOfStack()!!

        // Opponent passes priority back so active player can respond
        driver.passPriority(opponent)

        // Active player counters
        val discombobulate = driver.putCardInHand(activePlayer, "Discombobulate")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = discombobulate,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.bothPass()

        // Resolve the reorder decision
        val reorderDecision = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(activePlayer, reorderDecision.cards)

        // Both Discombobulate and the countered spell should be in graveyards
        driver.getGraveyardCardNames(activePlayer) shouldContain "Discombobulate"
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
    }
})
