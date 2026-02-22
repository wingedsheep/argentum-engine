package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.NosyGoblin
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Nosy Goblin.
 *
 * Nosy Goblin: {2}{R}
 * Creature — Goblin
 * 2/1
 * {T}, Sacrifice Nosy Goblin: Destroy target face-down creature.
 */
class NosyGoblinTest : FunSpec({

    val nosyGoblinAbilityId = NosyGoblin.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        replaceState(state.updateEntity(creatureId) { it.with(FaceDownComponent) })
        return creatureId
    }

    test("destroys target face-down creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Nosy Goblin on the battlefield (no summoning sickness)
        val nosyGoblin = driver.putCreatureOnBattlefield(activePlayer, "Nosy Goblin")
        driver.removeSummoningSickness(nosyGoblin)

        // Put a face-down creature on the opponent's battlefield
        val faceDownCreature = driver.putFaceDownCreature(opponent, "Morph Test Creature")

        // Activate Nosy Goblin targeting the face-down creature
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = nosyGoblin,
                abilityId = nosyGoblinAbilityId,
                targets = listOf(ChosenTarget.Permanent(faceDownCreature))
            )
        )
        result.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Face-down creature should be destroyed
        driver.findPermanent(opponent, "Morph Test Creature") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Morph Test Creature"

        // Nosy Goblin was sacrificed
        driver.findPermanent(activePlayer, "Nosy Goblin") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Nosy Goblin"
    }

    test("cannot target a face-up creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Nosy Goblin on the battlefield
        val nosyGoblin = driver.putCreatureOnBattlefield(activePlayer, "Nosy Goblin")
        driver.removeSummoningSickness(nosyGoblin)

        // Put a normal (face-up) creature on the opponent's battlefield
        val normalCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Attempt to activate Nosy Goblin targeting the face-up creature — should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = nosyGoblin,
                abilityId = nosyGoblinAbilityId,
                targets = listOf(ChosenTarget.Permanent(normalCreature))
            )
        )
        result.isSuccess shouldBe false
    }
})
