package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.JeeringInstigator
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Jeering Instigator.
 *
 * Jeering Instigator: {1}{R}
 * Creature — Goblin Rogue
 * 2/1
 * Morph {2}{R}
 * When this creature is turned face up, if it's your turn, gain control of another target creature
 * until end of turn. Untap that creature. It gains haste until end of turn.
 */
class JeeringInstigatorTest : FunSpec({

    val allCards = TestCards.all + listOf(JeeringInstigator)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Morph>()
            .firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(MorphDataComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        return creatureId
    }

    test("turn face up gains control, untaps, and grants haste") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2

        // Put Jeering Instigator face-down on active player's battlefield
        val instigator = driver.putFaceDownCreature(activePlayer, "Jeering Instigator")
        driver.removeSummoningSickness(instigator)

        // Put an opponent creature on battlefield (tapped)
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.replaceState(driver.state.updateEntity(opponentCreature) { it.with(TappedComponent) })

        // Put lands on battlefield to pay morph cost {2}{R}
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")

        // Verify creature is tapped before
        driver.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe true

        // Turn face up
        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = instigator
            )
        )
        (result.error == null) shouldBe true

        // The card should now be face up
        driver.state.getEntity(instigator)?.get<FaceDownComponent>() shouldBe null
        driver.state.getEntity(instigator)?.get<CardComponent>()?.name shouldBe "Jeering Instigator"

        // Triggered ability should have fired - select opponent's creature as target
        driver.state.pendingDecision shouldNotBe null
        driver.submitTargetSelection(activePlayer, listOf(opponentCreature))

        // Resolve the triggered ability
        driver.bothPass()

        // Active player should now control the opponent's creature (projected state)
        val projectedController = driver.state.projectedState.getController(opponentCreature)
        projectedController shouldBe activePlayer

        // Creature should be untapped
        driver.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe false

        // Creature should have haste
        val projected = driver.state.projectedState
        projected.hasKeyword(opponentCreature, Keyword.HASTE) shouldBe true
    }

    test("trigger does not fire on opponent's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2

        // Put Jeering Instigator face-down on OPPONENT's battlefield (not their turn)
        val instigator = driver.putFaceDownCreature(opponent, "Jeering Instigator")
        driver.removeSummoningSickness(instigator)

        // Put a creature on battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        // Put lands on battlefield to pay morph cost {2}{R}
        driver.putLandOnBattlefield(opponent, "Mountain")
        driver.putLandOnBattlefield(opponent, "Mountain")
        driver.putLandOnBattlefield(opponent, "Mountain")

        // Give opponent priority
        driver.replaceState(driver.state.withPriority(opponent))

        // Turn face up
        val result = driver.submit(
            TurnFaceUp(
                playerId = opponent,
                sourceId = instigator
            )
        )
        (result.error == null) shouldBe true

        // The trigger should NOT have fired (not opponent's turn)
        // No pending decision expected
        driver.state.pendingDecision shouldBe null

        // Creature should still be controlled by the active player
        val projectedController = driver.state.projectedState.getController(creature)
        projectedController shouldBe activePlayer
    }
})
