package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Chain of Plasma.
 *
 * Chain of Plasma: {1}{R}
 * Instant
 * Chain of Plasma deals 3 damage to any target. Then that player or that permanent's
 * controller may discard a card. If the player does, they may copy this spell and may
 * choose a new target for that copy.
 */
class ChainOfPlasmaTest : FunSpec({

    val ChainOfPlasma = CardDefinition.instant(
        name = "Chain of Plasma",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Chain of Plasma deals 3 damage to any target. Then that player or that permanent's controller may discard a card. If the player does, they may copy this spell and may choose a new target for that copy.",
        script = CardScript.spell(
            effect = Effects.DamageAndChainCopy(
                amount = 3,
                target = EffectTarget.BoundVariable("target"),
                spellName = "Chain of Plasma"
            ),
            AnyTarget(id = "target")
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChainOfPlasma))
        return driver
    }

    test("Chain of Plasma deals 3 damage to target player, opponent declines to copy") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chain = driver.putCardInHand(activePlayer, "Chain of Plasma")
        driver.giveMana(activePlayer, Color.RED, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent is offered to discard a card to copy
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()

        // Decline
        driver.submitDecision(opponent, YesNoResponse(decision.id, false))

        // Opponent should have taken 3 damage
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("Chain of Plasma deals 3 damage to target creature, controller declines to copy") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield for opponent
        val bear = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        val chain = driver.putCardInHand(activePlayer, "Chain of Plasma")
        driver.giveMana(activePlayer, Color.RED, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(bear))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Opponent (creature's controller) is offered to discard to copy
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()

        // Decline
        driver.submitDecision(opponent, YesNoResponse(decision.id, false))

        // Grizzly Bears (2/2) should be dead from 3 damage
        driver.assertInGraveyard(opponent, "Grizzly Bears")
    }

    test("Chain of Plasma - target player copies by discarding, chooses new target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val oppHandBefore = driver.getHandSize(opponent)

        val chain = driver.putCardInHand(activePlayer, "Chain of Plasma")
        driver.giveMana(activePlayer, Color.RED, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Step 1: Opponent is offered to discard to copy — accept
        val copyDecision = driver.pendingDecision
        copyDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(opponent, YesNoResponse(copyDecision.id, true))

        // Step 2: Opponent selects a card to discard
        val discardDecision = driver.pendingDecision
        discardDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discardDecision.minSelections shouldBe 1
        discardDecision.maxSelections shouldBe 1
        val oppHand = driver.getHand(opponent)
        driver.submitDecision(opponent, CardsSelectedResponse(discardDecision.id, listOf(oppHand[0])))

        // Step 3: Opponent selects active player as the target for the copy
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(opponent, CardsSelectedResponse(targetDecision.id, listOf(activePlayer)))

        // Copy is on the stack — both pass to resolve
        driver.bothPass()

        // Active player is offered to discard to copy — decline
        val apDecision = driver.pendingDecision
        apDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(activePlayer, YesNoResponse(apDecision.id, false))

        // Verify: opponent took 3 damage, discarded 1 card; active player took 3 damage
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(activePlayer) shouldBe 17
        driver.getHandSize(opponent) shouldBe (oppHandBefore - 1)
    }

    test("Chain of Plasma - no copy offered when target player has empty hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty opponent's hand by moving all cards to library
        val handZone = ZoneKey(opponent, Zone.HAND)
        val libraryZone = ZoneKey(opponent, Zone.LIBRARY)
        val handCards = driver.state.getZone(handZone)
        var newState = driver.state
        for (cardId in handCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(libraryZone, cardId)
        }
        driver.replaceState(newState)
        driver.getHandSize(opponent) shouldBe 0

        val chain = driver.putCardInHand(activePlayer, "Chain of Plasma")
        driver.giveMana(activePlayer, Color.RED, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(opponent))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // No decision should be offered since opponent has no cards to discard
        driver.isPaused shouldBe false
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("Chain of Plasma - can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chain = driver.putCardInHand(activePlayer, "Chain of Plasma")
        driver.giveMana(activePlayer, Color.RED, 2)

        val castResult = driver.castSpell(activePlayer, chain, listOf(activePlayer))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Active player is offered to discard to copy — decline
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitDecision(activePlayer, YesNoResponse(decision.id, false))

        // Active player should have taken 3 damage
        driver.getLifeTotal(activePlayer) shouldBe 17
    }
})
