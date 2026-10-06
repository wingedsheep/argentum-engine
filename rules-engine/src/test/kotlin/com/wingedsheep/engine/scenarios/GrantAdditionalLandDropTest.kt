package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the GrantAdditionalLandDrop static ability.
 *
 * This models "You may play an additional land on each of your turns" as a
 * continuous effect — active while the permanent is on the battlefield.
 */
class GrantAdditionalLandDropTest : FunSpec({

    val LandDropBear = card("Land Drop Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2

        staticAbility {
            ability = GrantAdditionalLandDrop(count = 1)
        }
    }

    val DoubleLandDropBear = card("Double Land Drop Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3

        staticAbility {
            ability = GrantAdditionalLandDrop(count = 2)
        }
    }

    val AnyNumberOfLandsBear = card("Any Number Of Lands Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 1
        toughness = 1

        staticAbility {
            ability = GrantAdditionalLandDrop(count = null)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LandDropBear)
        driver.registerCard(DoubleLandDropBear)
        driver.registerCard(AnyNumberOfLandsBear)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 30,
                "Grizzly Bears" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    test("grants one additional land drop while on battlefield") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the permanent on the battlefield
        driver.putPermanentOnBattlefield(player, "Land Drop Bear")

        // Base remaining is 1, but with the static bonus we should be able to play 2 lands
        val forest1 = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest1).outcome shouldBe Outcome.Done

        // LandDropsComponent.remaining is now 0, but static bonus allows one more
        val landDrops = driver.state.getEntity(player)?.get<LandDropsComponent>()
        landDrops!!.remaining shouldBe 0

        val forest2 = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest2).outcome shouldBe Outcome.Done

        // remaining is now -1, and with +1 bonus the effective is 0 — no more land plays
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe -1

        val forest3 = driver.putCardInHand(player, "Forest")
        val result = driver.submitExpectFailure(PlayLand(player, forest3))
        result.outcome shouldNotBe Outcome.Done
    }

    test("multiple sources stack additively") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two copies of LandDropBear = +2 bonus
        driver.putPermanentOnBattlefield(player, "Land Drop Bear")
        driver.putPermanentOnBattlefield(player, "Land Drop Bear")

        // Should be able to play 3 total lands (1 base + 2 bonus)
        for (i in 1..3) {
            val forest = driver.putCardInHand(player, "Forest")
            driver.playLand(player, forest).outcome shouldBe Outcome.Done
        }

        // Fourth should fail
        val forest = driver.putCardInHand(player, "Forest")
        val result = driver.submitExpectFailure(PlayLand(player, forest))
        result.outcome shouldNotBe Outcome.Done
    }

    test("count parameter grants multiple drops from a single source") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // DoubleLandDropBear grants +2
        driver.putPermanentOnBattlefield(player, "Double Land Drop Bear")

        // Should be able to play 3 total lands (1 base + 2 bonus)
        for (i in 1..3) {
            val forest = driver.putCardInHand(player, "Forest")
            driver.playLand(player, forest).outcome shouldBe Outcome.Done
        }

        // Fourth should fail
        val forest = driver.putCardInHand(player, "Forest")
        val result = driver.submitExpectFailure(PlayLand(player, forest))
        result.outcome shouldNotBe Outcome.Done
    }

    test("effect does not persist after turn resets") {
        val driver = createDriver()
        val startingPlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(startingPlayer, "Land Drop Bear")

        // Play 2 lands (1 base + 1 bonus)
        val forest1 = driver.putCardInHand(startingPlayer, "Forest")
        driver.playLand(startingPlayer, forest1).outcome shouldBe Outcome.Done
        val forest2 = driver.putCardInHand(startingPlayer, "Forest")
        driver.playLand(startingPlayer, forest2).outcome shouldBe Outcome.Done

        // Advance to next turn for this player
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        var passes = 0
        while ((driver.activePlayer != startingPlayer || driver.currentStep != Step.PRECOMBAT_MAIN) && passes < 50) {
            if (driver.state.pendingDecision != null) {
                driver.autoResolveDecision()
            } else if (driver.state.priorityPlayerId != null) {
                driver.passPriority(driver.state.priorityPlayerId!!)
            }
            passes++
        }

        driver.activePlayer shouldBe startingPlayer
        driver.currentStep shouldBe Step.PRECOMBAT_MAIN

        // LandDropsComponent resets to maxPerTurn=1, plus the static +1 = 2 effective drops
        val landDrops = driver.state.getEntity(startingPlayer)?.get<LandDropsComponent>()
        landDrops!!.remaining shouldBe 1
        landDrops.maxPerTurn shouldBe 1

        // Can still play 2 lands again
        val forest3 = driver.putCardInHand(startingPlayer, "Forest")
        driver.playLand(startingPlayer, forest3).outcome shouldBe Outcome.Done
        val forest4 = driver.putCardInHand(startingPlayer, "Forest")
        driver.playLand(startingPlayer, forest4).outcome shouldBe Outcome.Done

        // Third fails
        val forest5 = driver.putCardInHand(startingPlayer, "Forest")
        val result = driver.submitExpectFailure(PlayLand(startingPlayer, forest5))
        result.outcome shouldNotBe Outcome.Done
    }

    test("stacks with one-shot PlayAdditionalLandsEffect (Summer Bloom)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Static ability grants +1
        driver.putPermanentOnBattlefield(player, "Land Drop Bear")

        // Cast Summer Bloom for +3 to remaining
        driver.giveMana(player, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        val summerBloom = driver.putCardInHand(player, "Summer Bloom")
        driver.castSpell(player, summerBloom)
        driver.bothPass()

        // remaining = 1 + 3 = 4, plus static +1 = 5 effective land plays
        val landDrops = driver.state.getEntity(player)?.get<LandDropsComponent>()
        landDrops!!.remaining shouldBe 4

        for (i in 1..5) {
            val forest = driver.putCardInHand(player, "Forest")
            driver.playLand(player, forest).outcome shouldBe Outcome.Done
        }

        // Sixth fails
        val forest = driver.putCardInHand(player, "Forest")
        val result = driver.submitExpectFailure(PlayLand(player, forest))
        result.outcome shouldNotBe Outcome.Done
    }

    test("count = null grants any number of land plays, offered and accepted alike") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Any Number Of Lands Bear")
        // A finite grant alongside must not turn "unlimited" back into a sum.
        driver.putPermanentOnBattlefield(player, "Land Drop Bear")

        repeat(6) {
            val forest = driver.putCardInHand(player, "Forest")
            driver.legalActions(player).any { (it.action as? PlayLand)?.cardId == forest } shouldBe true
            driver.playLand(player, forest).outcome shouldBe Outcome.Done
        }
        driver.state.getEntity(player)?.get<LandDropsComponent>()!!.remaining shouldBe -5
    }

    test("count = null is lost with its source — the turn's ordinary limit applies again") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bear = driver.putPermanentOnBattlefield(player, "Any Number Of Lands Bear")
        repeat(3) { driver.playLand(player, driver.putCardInHand(player, "Forest")).outcome shouldBe Outcome.Done }

        driver.moveToGraveyard(bear)
        val forest = driver.putCardInHand(player, "Forest")
        driver.legalActions(player).any { (it.action as? PlayLand)?.cardId == forest } shouldBe false
        driver.submitExpectFailure(PlayLand(player, forest)).outcome shouldNotBe Outcome.Done
    }

    test("count = null only reaches the players it names") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(opponent, "Any Number Of Lands Bear")

        driver.playLand(player, driver.putCardInHand(player, "Forest")).outcome shouldBe Outcome.Done
        driver.submitExpectFailure(PlayLand(player, driver.putCardInHand(player, "Forest"))).outcome shouldNotBe
            Outcome.Done
    }
})
