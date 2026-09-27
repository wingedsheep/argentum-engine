package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.EightAndAHalfTails
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Eight-and-a-Half-Tails (CHK #8)
 *
 * "{1}{W}: Target permanent you control gains protection from white until end of turn.
 *  {1}: Target spell or permanent becomes white until end of turn."
 */
class EightAndAHalfTailsScenarioTest : FunSpec({

    val protectionAbility = EightAndAHalfTails.activatedAbilities[0].id
    val whitenAbility = EightAndAHalfTails.activatedAbilities[1].id
    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EightAndAHalfTails))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("grants protection from white to a permanent you control until end of turn") {
        val (driver, player) = newGame()
        val tails = driver.putCreatureOnBattlefield(player, "Eight-and-a-Half-Tails")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        driver.giveMana(player, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = tails,
                abilityId = protectionAbility,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        projector.project(driver.state).hasKeyword(bears, "PROTECTION_FROM_WHITE") shouldBe true
        projector.project(driver.state).hasKeyword(tails, "PROTECTION_FROM_WHITE") shouldBe false
    }

    test("protection ability can't target a permanent you don't control") {
        val (driver, player) = newGame()
        val opponent = driver.getOpponent(player)
        val tails = driver.putCreatureOnBattlefield(player, "Eight-and-a-Half-Tails")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.giveMana(player, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = tails,
                abilityId = protectionAbility,
                targets = listOf(ChosenTarget.Permanent(theirBears))
            )
        ).outcome shouldNotBe Outcome.Done
    }

    test("a permanent becomes white, replacing its other colors") {
        val (driver, player) = newGame()
        val opponent = driver.getOpponent(player)
        val tails = driver.putCreatureOnBattlefield(player, "Eight-and-a-Half-Tails")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        projector.project(driver.state).getColors(theirBears) shouldBe setOf("GREEN")

        driver.giveMana(player, Color.GREEN, 1) // {1} is generic
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = tails,
                abilityId = whitenAbility,
                targets = listOf(ChosenTarget.Permanent(theirBears))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        projector.project(driver.state).getColors(theirBears) shouldBe setOf("WHITE")
    }

    test("a spell becomes white on the stack and the permanent stays white after it resolves") {
        val (driver, player) = newGame()
        val tails = driver.putCreatureOnBattlefield(player, "Eight-and-a-Half-Tails")

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, bears)
        driver.state.stack.contains(bears) shouldBe true

        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = tails,
                abilityId = whitenAbility,
                targets = listOf(ChosenTarget.Spell(bears))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the ability

        driver.state.stack.contains(bears) shouldBe true
        projector.project(driver.state).getColors(bears) shouldBe setOf("WHITE")

        driver.bothPass() // resolve Grizzly Bears
        driver.state.getBattlefield().contains(bears) shouldBe true
        // Ruling (2016-06-08): a permanent spell made white enters and stays white until end of turn.
        projector.project(driver.state).getColors(bears) shouldBe setOf("WHITE")
    }
})
