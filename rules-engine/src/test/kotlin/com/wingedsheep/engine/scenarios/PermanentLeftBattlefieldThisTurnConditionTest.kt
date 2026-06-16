package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.PermanentLeftBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for `PermanentLeftBattlefieldThisTurn` — the per-player condition behind LTR's
 * Shortcut to Mushrooms ("if a permanent you controlled left the battlefield this turn").
 *
 * Broader than `CreatureDiedThisTurnCondition`: counts every permanent type (including lands
 * and tokens) leaving the battlefield, regardless of destination zone, credited to the
 * *last-known controller* at departure.
 */
class PermanentLeftBattlefieldThisTurnConditionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Swamp" to 10,
                "Mountain" to 10,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.evalFor(player: EntityId): Boolean {
        val context = EffectContext(
            sourceId = null,
            controllerId = player,
            targets = emptyList(),
            xValue = 0
        )
        return ConditionEvaluator().evaluate(state, PermanentLeftBattlefieldThisTurn(Player.You), context)
    }

    test("false at start of game for both players") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.evalFor(p1) shouldBe false
        driver.evalFor(p2) shouldBe false
    }

    test("destroying a creature credits its controller (the victim's controller)") {
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

        // The opponent's bear left their battlefield → opponent's count = 1; player unaffected.
        driver.evalFor(opponent) shouldBe true
        driver.evalFor(player) shouldBe false
    }

    test("a sacrificed land counts (lands are not excluded like Void)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val land = driver.putLandOnBattlefield(player, "Forest")
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false

        val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = land,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(result.state)

        // Void global stays false (land); per-player tracker DOES count it.
        driver.state.nonlandPermanentLeftBattlefieldThisTurn shouldBe false
        driver.evalFor(player) shouldBe true
    }

    test("resets at end of turn") {
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

        driver.evalFor(opponent) shouldBe true

        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent
        driver.evalFor(opponent) shouldBe false
        driver.evalFor(player) shouldBe false
    }
})
