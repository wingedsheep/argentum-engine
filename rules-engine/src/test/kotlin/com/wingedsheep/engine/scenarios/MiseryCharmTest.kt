package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.MiseryCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Misery Charm (ONS #158).
 *
 * Misery Charm: {B}
 * Instant
 * Choose one —
 * • Destroy target Cleric.
 * • Return target Cleric card from your graveyard to your hand.
 * • Target player loses 2 life.
 */
class MiseryCharmTest : FunSpec({

    val TestCleric = CardDefinition.creature(
        name = "Test Cleric",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestCleric))
        return driver
    }

    test("mode 1 - destroy target Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a Cleric on the battlefield
        driver.putCreatureOnBattlefield(opponent, "Test Cleric")

        // Cast Misery Charm (no targets at cast time for modal spells)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val charm = driver.putCardInHand(activePlayer, "Misery Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → ModalEffectExecutor pauses with mode selection
        driver.bothPass()

        // Choose mode 0 (destroy target Cleric)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 0))

        // Select the single valid Cleric target
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val clericId = targetDecision.legalTargets.values.first().first()
        driver.submitTargetSelection(activePlayer, listOf(clericId))

        // Cleric should be in graveyard
        driver.assertInGraveyard(opponent, "Test Cleric")
    }

    test("mode 1 - fizzles with no valid Cleric target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a non-Cleric creature only
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Misery Charm
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val charm = driver.putCardInHand(activePlayer, "Misery Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → mode selection
        driver.bothPass()

        // Choose mode 0 (destroy target Cleric) - but no Clerics exist
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 0))

        // Spell fizzles (no valid targets for chosen mode)
        // Grizzly Bears should still be on the battlefield
        driver.assertPermanentExists(opponent, "Grizzly Bears")
    }

    test("mode 2 - return target Cleric card from graveyard to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Cleric in our graveyard
        driver.putCardInGraveyard(activePlayer, "Test Cleric")

        // Cast Misery Charm
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val charm = driver.putCardInHand(activePlayer, "Misery Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → mode selection
        driver.bothPass()

        // Choose mode 1 (return Cleric from graveyard)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 1))

        // Select the single valid Cleric target in graveyard
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val clericId = targetDecision.legalTargets.values.first().first()
        driver.submitTargetSelection(activePlayer, listOf(clericId))

        // Cleric should be in hand now
        val hand = driver.state.getHand(activePlayer)
        val clericInHand = hand.any { entityId ->
            driver.state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == "Test Cleric"
        }
        clericInHand shouldBe true
    }

    test("mode 3 - target player loses 2 life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Misery Charm
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val charm = driver.putCardInHand(activePlayer, "Misery Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → mode selection
        driver.bothPass()

        // Choose mode 2 (target player loses 2 life)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 2))

        // Two players are valid targets → target selection decision
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // Opponent should have lost 2 life
        driver.assertLifeTotal(opponent, 18)
    }

    test("mode 3 - can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Misery Charm
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val charm = driver.putCardInHand(activePlayer, "Misery Charm")
        driver.castSpell(activePlayer, charm)

        // Both pass → spell resolves → mode selection
        driver.bothPass()

        // Choose mode 2 (target player loses 2 life)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 2))

        // Two players are valid targets → target selection decision
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(activePlayer, listOf(activePlayer))

        // Active player should have lost 2 life
        driver.assertLifeTotal(activePlayer, 18)
    }
})
