package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Gigapede:
 * {3}{G}{G}
 * Creature — Insect
 * 6/1
 * Shroud
 * At the beginning of your upkeep, if Gigapede is in your graveyard,
 * you may discard a card. If you do, return Gigapede to its owner's hand.
 */
class GigapedeTest : FunSpec({

    val Gigapede = CardDefinition.creature(
        name = "Gigapede",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype("Insect")),
        power = 6,
        toughness = 1,
        keywords = setOf(Keyword.SHROUD),
        oracleText = "Shroud. At the beginning of your upkeep, if Gigapede is in your graveyard, you may discard a card. If you do, return Gigapede to its owner's hand.",
        script = CardScript.permanent(
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = OnUpkeep(controllerOnly = true),
                    effect = ReflexiveTriggerEffect(
                        action = DiscardCardsEffect(1, EffectTarget.Controller),
                        optional = true,
                        reflexiveEffect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
                    ),
                    activeZone = Zone.GRAVEYARD
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Gigapede))
        return driver
    }

    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("trigger fires from graveyard - discard returns Gigapede to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Gigapede in the graveyard
        val gigapede = driver.putCardInGraveyard(activePlayer, "Gigapede")

        // Advance to the controller's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger goes on the stack - both pass to resolve it
        driver.bothPass()

        // Now the YesNo decision should appear from ReflexiveTriggerEffect
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Accept - yes, discard a card
        driver.submitYesNo(activePlayer, true)

        // Now we need to choose a card to discard
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // Find a card in hand to discard
        val hand = driver.state.getZone(ZoneKey(activePlayer, Zone.HAND))
        driver.submitCardSelection(activePlayer, listOf(hand.first()))

        // Gigapede should be in the player's hand now
        val newHand = driver.state.getZone(ZoneKey(activePlayer, Zone.HAND))
        newHand.contains(gigapede) shouldBe true

        // Gigapede should NOT be in the graveyard
        driver.state.getGraveyard(activePlayer).contains(gigapede) shouldBe false
    }

    test("declining the discard - Gigapede stays in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Gigapede in the graveyard
        val gigapede = driver.putCardInGraveyard(activePlayer, "Gigapede")

        // Advance to the controller's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger goes on the stack - both pass to resolve it
        driver.bothPass()

        // YesNo decision for "you may discard"
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Decline the optional discard
        driver.submitYesNo(activePlayer, false)

        // Gigapede should still be in the graveyard
        driver.state.getGraveyard(activePlayer).contains(gigapede) shouldBe true
        val hand = driver.state.getZone(ZoneKey(activePlayer, Zone.HAND))
        hand.contains(gigapede) shouldBe false
    }

    test("no trigger on battlefield - Gigapede on battlefield does NOT trigger graveyard ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Gigapede on the battlefield (not graveyard)
        driver.putCreatureOnBattlefield(activePlayer, "Gigapede")

        // Advance to upkeep - should NOT trigger the graveyard ability
        advanceToPlayerUpkeep(driver, activePlayer)

        // Should pass through upkeep without any YesNo decision for Gigapede
        driver.currentStep shouldBe Step.UPKEEP
    }

    test("shroud prevents targeting Gigapede on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Gigapede on the battlefield for opponent
        val gigapede = driver.putCreatureOnBattlefield(opponent, "Gigapede")

        // Give active player mana and a Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        val result = driver.castSpell(activePlayer, bolt)
        if (result.isSuccess && driver.pendingDecision is ChooseTargetsDecision) {
            val targetDecision = driver.pendingDecision as ChooseTargetsDecision
            val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
            // Gigapede should NOT be a legal target (it has shroud)
            legalTargets.contains(gigapede) shouldBe false
        }
    }
})
