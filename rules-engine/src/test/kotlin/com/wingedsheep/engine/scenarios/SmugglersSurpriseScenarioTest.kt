package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SmugglersSurprise
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Smuggler's Surprise — {G} Instant, Spree
 *
 * + {2} — Mill four cards. You may put up to two creature and/or land cards from among the
 *         milled cards into your hand.
 * + {4}{G} — You may put up to two creature cards from your hand onto the battlefield.
 * + {1} — Creatures you control with power 4 or greater gain hexproof and indestructible.
 */
class SmugglersSurpriseScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SmugglersSurprise)
        return driver
    }

    test("mode + {2}: mill four, put up to two creature/land cards from milled into hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // Top four (top-down): a creature, a land, a noncreature/nonland instant, and a land.
        // Only the creature and the two lands are eligible to go to hand (3 options); the
        // instant is filtered out by the creature/land restriction.
        driver.putCardOnTopOfLibrary(me, "Forest")
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val milledLand = driver.putCardOnTopOfLibrary(me, "Forest")
        val milledCreature = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        val spell = driver.putCardInHand(me, "Smuggler's Surprise")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(0),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve -> mill 4 -> pause for the up-to-two selection

        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()
        // Three creature/land cards among the four milled are eligible (the instant is filtered out).
        (select as SelectCardsDecision).options.size shouldBe 3

        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(milledCreature, milledLand))
        )
        driver.isPaused shouldBe false

        // Both chosen cards moved from graveyard to hand.
        driver.getHand(me).contains(milledCreature) shouldBe true
        driver.getHand(me).contains(milledLand) shouldBe true
        driver.getGraveyard(me).contains(milledCreature) shouldBe false
    }

    test("mode + {1}: power-4+ creatures gain hexproof and indestructible, smaller ones don't") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val big = driver.putCreatureOnBattlefield(me, "Force of Nature") // 5/5
        val small = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(me, "Smuggler's Surprise")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(2),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe false
        val projected = projector.project(driver.state)
        projected.hasKeyword(big, Keyword.HEXPROOF) shouldBe true
        projected.hasKeyword(big, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(small, Keyword.HEXPROOF) shouldBe false
        projected.hasKeyword(small, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("mode + {4}{G}: put a creature card from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val creatureInHand = driver.putCardInHand(me, "Force of Nature") // 5/5 creature
        val spell = driver.putCardInHand(me, "Smuggler's Surprise")

        driver.giveMana(me, Color.GREEN, 2)
        driver.giveColorlessMana(me, 4)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(1),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve -> pause for the up-to-two hand selection

        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()

        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(creatureInHand))
        )
        driver.isPaused shouldBe false

        // The creature is now on the battlefield under my control.
        driver.findPermanent(me, "Force of Nature") shouldBe creatureInHand
        driver.getHand(me).contains(creatureInHand) shouldBe false
    }

    test("Spree requires at least one mode: casting with no modes fails") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Smuggler's Surprise")
        driver.giveMana(me, Color.GREEN, 1)
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
