package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.triggers.OnUpkeep
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tests for Skittish Valesk's coin flip upkeep trigger.
 *
 * Skittish Valesk: {6}{R}
 * Creature — Beast 5/5
 * At the beginning of your upkeep, flip a coin. If you lose the flip,
 * turn Skittish Valesk face down.
 * Morph {5}{R}
 */
class SkittishValeskTest : FunSpec({

    // Recreate the card for rules-engine tests (no mtg-sets dependency)
    val SkittishValesk = CardDefinition.creature(
        name = "Skittish Valesk",
        manaCost = ManaCost.parse("{6}{R}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5,
        toughness = 5,
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnUpkeep(controllerOnly = true),
                effect = FlipCoinEffect(
                    lostEffect = TurnFaceDownEffect(EffectTarget.Self)
                )
            )
        )
    ).copy(
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{5}{R}")))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SkittishValesk))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        return driver
    }

    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: com.wingedsheep.sdk.model.EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("upkeep trigger fires and produces a coin flip event") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val owner = driver.activePlayer!!
        val valesk = driver.putCreatureOnBattlefield(owner, "Skittish Valesk")
        driver.removeSummoningSickness(valesk)

        advanceToPlayerUpkeep(driver, owner)

        // The trigger should be on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        val result = driver.bothPass()

        // Should have a CoinFlipEvent
        val coinFlipEvents = result.events.filterIsInstance<CoinFlipEvent>()
        coinFlipEvents.size shouldBe 1
        coinFlipEvents[0].playerId shouldBe owner
        coinFlipEvents[0].sourceName shouldBe "Skittish Valesk"
    }

    test("losing the coin flip turns Skittish Valesk face down") {
        // Run multiple times to hit the "lose" case (50% chance each time)
        var foundLostFlip = false
        repeat(50) {
            if (foundLostFlip) return@repeat

            val driver = createDriver()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val owner = driver.activePlayer!!
            val valesk = driver.putCreatureOnBattlefield(owner, "Skittish Valesk")
            driver.removeSummoningSickness(valesk)

            advanceToPlayerUpkeep(driver, owner)
            val result = driver.bothPass()

            val coinEvent = result.events.filterIsInstance<CoinFlipEvent>().first()
            if (!coinEvent.won) {
                // Lost the flip — creature should be face down
                driver.state.getEntity(valesk)!!.has<FaceDownComponent>().shouldBeTrue()
                foundLostFlip = true
            }
        }
        foundLostFlip.shouldBeTrue()
    }

    test("winning the coin flip keeps Skittish Valesk face up") {
        var foundWonFlip = false
        repeat(50) {
            if (foundWonFlip) return@repeat

            val driver = createDriver()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val owner = driver.activePlayer!!
            val valesk = driver.putCreatureOnBattlefield(owner, "Skittish Valesk")
            driver.removeSummoningSickness(valesk)

            advanceToPlayerUpkeep(driver, owner)
            val result = driver.bothPass()

            val coinEvent = result.events.filterIsInstance<CoinFlipEvent>().first()
            if (coinEvent.won) {
                // Won the flip — creature should stay face up
                driver.state.getEntity(valesk)!!.has<FaceDownComponent>().shouldBeFalse()
                foundWonFlip = true
            }
        }
        foundWonFlip.shouldBeTrue()
    }
})
