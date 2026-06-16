package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThreeStepsAhead
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Three Steps Ahead — {U} Instant, Spree
 *
 * + {1}{U} — Counter target spell.
 * + {3} — Create a token that's a copy of target artifact or creature you control.
 * + {2} — Draw two cards, then discard a card.
 */
class ThreeStepsAheadScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ThreeStepsAhead)
        return driver
    }

    test("counter mode: counter target spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        val spell = driver.putCardInHand(me, "Three Steps Ahead")
        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 1) // {U} base + {1}{U} for counter mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(boltOnStack))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.bothPass()
        driver.isPaused shouldBe false
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
    }

    test("copy mode: create a token copy of a creature you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        val spell = driver.putCardInHand(me, "Three Steps Ahead")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 3) // {U} base + {3} for copy mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.bothPass()
        driver.isPaused shouldBe false

        // Now two Centaur Courser on my battlefield (original + token copy).
        val bears = driver.state.getBattlefield().filter {
            driver.getController(it) == me && driver.getCardName(it) == "Centaur Courser"
        }
        bears.size shouldBe 2
    }

    test("draw mode: draw two cards then discard one") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val handBefore = driver.getHandSize(me)
        val spell = driver.putCardInHand(me, "Three Steps Ahead")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2) // {U} base + {2} for draw mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        driver.bothPass()
        // The discard may pause for a selection if hand has >1 card.
        if (driver.isPaused) {
            val discardable = driver.getHand(me)
            driver.submitCardSelection(me, listOf(discardable.first()))
        }
        driver.isPaused shouldBe false

        // Spell left hand (+1 to gy), drew 2, discarded 1: net hand = before + 2 - 1.
        driver.getHandSize(me) shouldBe handBefore + 1
        driver.getGraveyardCardNames(me).size shouldBeGreaterThanOrEqual 1
    }

    test("Spree requires at least one mode") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Three Steps Ahead")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe false
    }
})
