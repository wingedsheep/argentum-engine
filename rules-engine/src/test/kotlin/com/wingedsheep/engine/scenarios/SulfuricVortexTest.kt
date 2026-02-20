package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Sulfuric Vortex:
 * {1}{R}{R} Enchantment
 * At the beginning of each player's upkeep, Sulfuric Vortex deals 2 damage to that player.
 * If a player would gain life, that player gains no life instead.
 */
class SulfuricVortexTest : FunSpec({

    val HealingSalve = CardDefinition.instant(
        name = "Healing Salve",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "Target player gains 3 life.",
        script = CardScript.spell(
            effect = GainLifeEffect(3, EffectTarget.Controller)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HealingSalve))
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

    test("deals 2 damage to active player on their upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Sulfuric Vortex")

        // From PRECOMBAT_MAIN, advance to the next upkeep (opponent's)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe opponent

        // Trigger goes on stack, resolve it
        driver.bothPass()

        // Opponent should have taken 2 damage
        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("deals 2 damage on controller's upkeep too") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Sulfuric Vortex")

        // Advance to controller's own upkeep (skip opponent's turn)
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger goes on stack, resolve it
        driver.bothPass()

        // Controller should have taken 2 damage
        driver.getLifeTotal(activePlayer) shouldBe 18
    }

    test("prevents life gain") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Sulfuric Vortex")

        // Cast Healing Salve to try to gain 3 life
        val salveId = driver.putCardInHand(activePlayer, "Healing Salve")
        driver.giveMana(activePlayer, Color.WHITE)
        driver.castSpell(activePlayer, salveId)
        driver.bothPass()

        // Life should be unchanged - life gain was prevented
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("removing Vortex re-enables life gain") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vortexId = driver.putPermanentOnBattlefield(activePlayer, "Sulfuric Vortex")

        // Remove Vortex from battlefield by moving it to graveyard via state manipulation
        val newState = driver.state.moveToZone(
            vortexId,
            from = ZoneKey(activePlayer, Zone.BATTLEFIELD),
            to = ZoneKey(activePlayer, Zone.GRAVEYARD)
        )
        driver.replaceState(newState)

        // Now cast Healing Salve to gain 3 life
        val salveId = driver.putCardInHand(activePlayer, "Healing Salve")
        driver.giveMana(activePlayer, Color.WHITE)
        driver.castSpell(activePlayer, salveId)
        driver.bothPass()

        // Life gain should work now
        driver.getLifeTotal(activePlayer) shouldBe 23
    }
})
