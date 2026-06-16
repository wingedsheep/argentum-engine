package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.VoidCondition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the Void ability word (Edge of Eternities).
 *
 * Void is satisfied when EITHER:
 *   - a nonland permanent left the battlefield this turn (tokens count, lands do not), OR
 *   - a spell was warped this turn (even if that spell was countered).
 *
 * The flag is global (not per-player) and resets at the start of each turn.
 */
class VoidConditionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Mountain" to 10,
                "Swamp" to 10,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.evalVoid(): Boolean {
        val controller = activePlayer!!
        val context = EffectContext(
            sourceId = null,
            controllerId = controller,
            targets = emptyList(),
            xValue = 0
        )
        return ConditionEvaluator().evaluate(state, VoidCondition, context)
    }

    test("Void is false at start of game") {
        val driver = createDriver()
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false
        driver.state.spellWarpedThisTurn shouldBe false
        driver.evalVoid() shouldBe false
    }

    test("destroying a nonland permanent enables void") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)

        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.castSpellWithTargets(
            player,
            doomBlade,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(victim))
        )
        driver.bothPass() // resolve

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe true
        driver.evalVoid() shouldBe true
    }

    test("a land going to the graveyard does NOT enable void") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Put a Forest on the battlefield, then move it directly to the graveyard
        // via the same zone-transition path destroyed permanents take.
        val land = driver.putLandOnBattlefield(player, "Forest")
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false

        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .moveToZone(
                state = driver.state,
                entityId = land,
                destinationZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD
            )
        driver.replaceState(transitionResult.state)

        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false
        driver.evalVoid() shouldBe false
    }

    test("flag resets at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.castSpellWithTargets(
            player,
            doomBlade,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(victim))
        )
        driver.bothPass()

        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe true

        // Advance to end step, then bothPass through cleanup — the turn
        // auto-advances to the opponent's turn, which is where TurnManager wipes
        // both void flags.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false
        driver.evalVoid() shouldBe false
    }

    test("VoidCondition evaluates true when spellWarpedThisTurn flag is set") {
        val driver = createDriver()
        driver.replaceState(driver.state.copy(spellWarpedThisTurn = true))
        driver.evalVoid() shouldBe true
    }

    test("VoidCondition evaluates true when nonlandPermanentLeftBattlefieldThisTurn flag is set") {
        val driver = createDriver()
        driver.replaceState(
            driver.state.copy(nonlandPermanentLeftBattlefieldThisTurn = true)
        )
        driver.evalVoid() shouldBe true
    }
})
