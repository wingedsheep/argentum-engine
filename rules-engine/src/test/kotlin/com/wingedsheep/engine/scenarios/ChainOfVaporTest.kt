package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.ChainOfVapor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Chain of Vapor.
 *
 * Chain of Vapor: {U}
 * Instant
 * Return target nonland permanent to its owner's hand. Then that permanent's controller
 * may sacrifice a land of their choice. If the player does, they may copy this spell
 * and may choose a new target for that copy.
 */
class ChainOfVaporTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChainOfVapor))
        return driver
    }

    test("Chain of Vapor bounces target nonland permanent to owner's hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on opponent's battlefield
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        // Cast Chain of Vapor targeting opponent's creature
        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent should be offered to sacrifice a land (but has none)
        // So the creature should just be bounced
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null

        // Creature should be in opponent's hand
        driver.findCardInHand(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("Chain of Vapor - controller declines to sacrifice a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature and a land on opponent's battlefield
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putLandOnBattlefield(opponent, "Forest")

        // Put another nonland permanent so there's a valid target for the copy
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent should be asked if they want to sacrifice a land to copy
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()

        // Decline
        driver.submitDecision(opponent, YesNoResponse(decision.id, false))

        // Creature should be bounced, no further chaining
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findCardInHand(opponent, "Grizzly Bears") shouldNotBe null

        // Opponent still has their land
        driver.findPermanent(opponent, "Forest") shouldNotBe null
    }

    test("Chain of Vapor - controller sacrifices a land and copies to bounce another permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures and a land on opponent's battlefield
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val forest = driver.putLandOnBattlefield(opponent, "Forest")

        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Step 1: Opponent asked if they want to sacrifice a land to copy
        driver.isPaused shouldBe true
        val yesNoDecision = driver.pendingDecision
        yesNoDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(yesNoDecision.id, true))

        // Step 2: Only one land, so auto-sacrificed. Now select target for the copy.
        driver.isPaused shouldBe true
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(opponent, CardsSelectedResponse(targetDecision.id, listOf(courser)))

        // Copy is on the stack - both players need to pass for it to resolve
        driver.bothPass()

        // After the copy resolves, Centaur Courser is bounced.
        // Opponent has no more lands so chain ends automatically.

        // Both creatures should be bounced
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.findCardInHand(opponent, "Grizzly Bears") shouldNotBe null
        driver.findCardInHand(opponent, "Centaur Courser") shouldNotBe null

        // Land should be in graveyard (sacrificed)
        driver.getGraveyardCardNames(opponent).contains("Forest") shouldBe true
    }

    test("Chain of Vapor cannot target a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val land = driver.putLandOnBattlefield(activePlayer, "Forest")
        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(land))
        castResult.isSuccess shouldBe false
    }

    test("Chain of Vapor - no chain when controller has no lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a creature but no lands
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // No lands = no chain option. Should just bounce with no decision.
        driver.isPaused shouldBe false
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findCardInHand(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("Chain of Vapor - can target own permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val chain = driver.putCardInHand(activePlayer, "Chain of Vapor")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bears))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Active player controls the bounced permanent, they get the chain option
        // But since they have no lands on the battlefield, no chain offered
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.findCardInHand(activePlayer, "Grizzly Bears") shouldNotBe null
    }
})
