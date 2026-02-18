package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GainControlEffect
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.TapUntapEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Insurrection.
 *
 * {5}{R}{R}{R} Sorcery
 * Untap all creatures and gain control of them until end of turn.
 * They gain haste until end of turn.
 */
class InsurrectionTest : FunSpec({

    val projector = StateProjector()

    val Insurrection = CardDefinition.sorcery(
        name = "Insurrection",
        manaCost = ManaCost.parse("{5}{R}{R}{R}"),
        oracleText = "Untap all creatures and gain control of them until end of turn. They gain haste until end of turn.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    ForEachInGroupEffect(GroupFilter.AllCreatures, GainControlEffect(EffectTarget.Self, Duration.EndOfTurn)),
                    ForEachInGroupEffect(GroupFilter.AllCreatures, TapUntapEffect(EffectTarget.Self, tap = false)),
                    ForEachInGroupEffect(GroupFilter.AllCreatures, GrantKeywordUntilEndOfTurnEffect(Keyword.HASTE, EffectTarget.Self, Duration.EndOfTurn))
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Insurrection))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Grizzly Bears" to 10),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.advanceToPlayer1PrecombatMain() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    test("Insurrection gains control of all opponent creatures and grants haste") {
        val driver = createDriver()

        val opponentCreature1 = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val opponentCreature2 = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        repeat(8) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        val insurrectionCard = driver.putCardInHand(driver.player1, "Insurrection")
        val castResult = driver.castSpell(driver.player1, insurrectionCard)
        castResult.isSuccess shouldBe true

        // Resolve the spell (both pass priority)
        driver.bothPass()

        // After resolving, player 1 should control opponent's creatures (via floating effects)
        val projected = projector.project(driver.state)
        projected.getController(opponentCreature1) shouldBe driver.player1
        projected.getController(opponentCreature2) shouldBe driver.player1

        // Creatures should have haste
        projected.hasKeyword(opponentCreature1, Keyword.HASTE) shouldBe true
        projected.hasKeyword(opponentCreature2, Keyword.HASTE) shouldBe true
    }

    test("Insurrection untaps tapped creatures") {
        val driver = createDriver()

        val opponentCreature = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.tapPermanent(opponentCreature)

        val ownCreature = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.tapPermanent(ownCreature)

        repeat(8) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        val insurrectionCard = driver.putCardInHand(driver.player1, "Insurrection")
        val castResult = driver.castSpell(driver.player1, insurrectionCard)
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Both creatures should be untapped
        driver.isTapped(opponentCreature) shouldBe false
        driver.isTapped(ownCreature) shouldBe false
    }

    test("Insurrection control change ends at end of turn") {
        val driver = createDriver()

        val opponentCreature = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        repeat(8) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        val insurrectionCard = driver.putCardInHand(driver.player1, "Insurrection")
        val castResult = driver.castSpell(driver.player1, insurrectionCard)
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // During this turn, player 1 controls it
        val projected = projector.project(driver.state)
        projected.getController(opponentCreature) shouldBe driver.player1

        // Advance to the next turn (end of turn effects are cleaned up during turn transition)
        driver.passPriorityUntil(Step.UPKEEP)

        // After turn ends, control should revert to player 2
        val projectedAfter = projector.project(driver.state)
        projectedAfter.getController(opponentCreature) shouldBe driver.player2
    }

    test("Insurrection does not change control of own creatures") {
        val driver = createDriver()

        val ownCreature = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        repeat(8) { driver.putLandOnBattlefield(driver.player1, "Mountain") }

        driver.advanceToPlayer1PrecombatMain()

        val insurrectionCard = driver.putCardInHand(driver.player1, "Insurrection")
        val castResult = driver.castSpell(driver.player1, insurrectionCard)
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Own creature should still be under player 1's control
        val projected = projector.project(driver.state)
        projected.getController(ownCreature) shouldBe driver.player1
    }
})
