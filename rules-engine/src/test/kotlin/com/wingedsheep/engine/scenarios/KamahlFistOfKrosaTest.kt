package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.KamahlFistOfKrosa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Kamahl, Fist of Krosa.
 *
 * Kamahl, Fist of Krosa: {4}{G}{G}
 * Legendary Creature — Human Druid
 * 4/3
 * {G}: Target land becomes a 1/1 creature until end of turn. It's still a land.
 * {2}{G}{G}{G}: Creatures you control get +3/+3 and gain trample until end of turn.
 */
class KamahlFistOfKrosaTest : FunSpec({

    val animateLandAbilityId = KamahlFistOfKrosa.activatedAbilities[0].id
    val overrunAbilityId = KamahlFistOfKrosa.activatedAbilities[1].id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Animate land makes it a 1/1 creature that is still a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kamahl = driver.putCreatureOnBattlefield(activePlayer, "Kamahl, Fist of Krosa")
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.removeSummoningSickness(kamahl)

        driver.giveMana(activePlayer, Color.GREEN, 1)

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = kamahl,
                abilityId = animateLandAbilityId,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        )
        activateResult.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        // Land should now be a creature
        projected.hasType(forest, "CREATURE") shouldBe true
        // Land should still be a land
        projected.hasType(forest, "LAND") shouldBe true
        // Should have 1/1 stats
        projected.getPower(forest) shouldBe 1
        projected.getToughness(forest) shouldBe 1
    }

    test("Animated land reverts at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kamahl = driver.putCreatureOnBattlefield(activePlayer, "Kamahl, Fist of Krosa")
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.removeSummoningSickness(kamahl)

        driver.giveMana(activePlayer, Color.GREEN, 1)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = kamahl,
                abilityId = animateLandAbilityId,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        )
        driver.bothPass()

        // Should be a creature now
        val projected = projector.project(driver.state)
        projected.hasType(forest, "CREATURE") shouldBe true

        // Advance to next turn - effect should wear off
        driver.passPriorityUntil(Step.UPKEEP)

        val projectedNextTurn = projector.project(driver.state)
        projectedNextTurn.hasType(forest, "CREATURE") shouldBe false
        projectedNextTurn.hasType(forest, "LAND") shouldBe true
    }

    test("Overrun ability gives all creatures +3/+3 and trample") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kamahl = driver.putCreatureOnBattlefield(activePlayer, "Kamahl, Fist of Krosa")
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(kamahl)
        driver.removeSummoningSickness(creature)

        driver.giveMana(activePlayer, Color.GREEN, 5)

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = kamahl,
                abilityId = overrunAbilityId,
                targets = emptyList()
            )
        )
        activateResult.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        // Kamahl (4/3) should be 7/6
        projected.getPower(kamahl) shouldBe 7
        projected.getToughness(kamahl) shouldBe 6
        projected.hasKeyword(kamahl, Keyword.TRAMPLE) shouldBe true
        // Grizzly Bears (2/2) should be 5/5
        projected.getPower(creature) shouldBe 5
        projected.getToughness(creature) shouldBe 5
        projected.hasKeyword(creature, Keyword.TRAMPLE) shouldBe true
    }

    test("Overrun does not affect opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kamahl = driver.putCreatureOnBattlefield(activePlayer, "Kamahl, Fist of Krosa")
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(kamahl)

        driver.giveMana(activePlayer, Color.GREEN, 5)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = kamahl,
                abilityId = overrunAbilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        val projected = projector.project(driver.state)
        // Opponent's creature should be unaffected
        projected.getPower(opponentCreature) shouldBe 2
        projected.getToughness(opponentCreature) shouldBe 2
        projected.hasKeyword(opponentCreature, Keyword.TRAMPLE) shouldBe false
    }
})
