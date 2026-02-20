package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.portal.cards.Gravedigger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GravediggerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Gravedigger test card has correct targetRequirement") {
        // Verify the test card is correctly configured
        val gravedigger = Gravedigger
        gravedigger.triggeredAbilities.size shouldBe 1

        val etbAbility = gravedigger.triggeredAbilities.first()
        etbAbility.optional shouldBe true
        etbAbility.targetRequirement.shouldNotBeNull()
        etbAbility.targetRequirement.shouldBeInstanceOf<TargetObject>()
    }

    test("Gravedigger ETB trigger prompts for creature in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature card in the player's graveyard
        val grizzlyBears = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grizzly Bears"

        // Give active player Gravedigger and mana to cast it
        val gravedigger = driver.putCardInHand(activePlayer, "Gravedigger")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        // Cast Gravedigger
        val castResult = driver.castSpell(activePlayer, gravedigger)
        castResult.isSuccess shouldBe true

        // Let the creature spell resolve (both players pass priority)
        driver.bothPass()

        // Gravedigger should be on the battlefield
        driver.findPermanent(activePlayer, "Gravedigger") shouldNotBe null

        // The ETB trigger should have fired and we should have a target selection decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        targetDecision.playerId shouldBe activePlayer

        // Legal targets should include Grizzly Bears in graveyard
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain grizzlyBears
    }

    test("Gravedigger ETB trigger fizzles with no creatures in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Don't put anything in graveyard - empty graveyard

        // Give active player Gravedigger and mana to cast it
        val gravedigger = driver.putCardInHand(activePlayer, "Gravedigger")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        // Cast Gravedigger
        val castResult = driver.castSpell(activePlayer, gravedigger)
        castResult.isSuccess shouldBe true

        // Let the creature spell resolve
        driver.bothPass()

        // Gravedigger should be on the battlefield
        driver.findPermanent(activePlayer, "Gravedigger") shouldNotBe null

        // The ETB trigger should have fizzled (no legal targets)
        // The game should NOT be paused because the trigger can't go on the stack
        driver.isPaused shouldBe false
    }

    test("Gravedigger ETB trigger only targets YOUR graveyard, not opponent's") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature in OPPONENT's graveyard only
        driver.putCardInGraveyard(opponent, "Grizzly Bears")
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(activePlayer).size shouldBe 0

        // Give active player Gravedigger and mana to cast it
        val gravedigger = driver.putCardInHand(activePlayer, "Gravedigger")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        // Cast Gravedigger
        val castResult = driver.castSpell(activePlayer, gravedigger)
        castResult.isSuccess shouldBe true

        // Let the creature spell resolve
        driver.bothPass()

        // Gravedigger should be on the battlefield
        driver.findPermanent(activePlayer, "Gravedigger") shouldNotBe null

        // The ETB trigger should have fizzled because ONLY opponent has a creature in graveyard
        // Gravedigger targets creature in YOUR graveyard, not any graveyard
        driver.isPaused shouldBe false
    }
})
