package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.RunicArmasaur
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Runic Armasaur — "Whenever an opponent activates an ability of a creature or land that isn't a
 * mana ability, you may draw a card."
 *
 * player2 controls the Armasaur; player1 (the active player, its opponent) activates.
 */
class RunicArmasaurScenarioTest : FunSpec({

    val pingBear = card("Ping Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    val pingRock = card("Ping Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(RunicArmasaur, pingBear, pingRock).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.activateAndAnswer(source: com.wingedsheep.sdk.model.EntityId, name: String, draw: Boolean) {
        giveColorlessMana(player1, 1)
        val abilityId = cardRegistry.requireCard(name).activatedAbilities[0].id
        submitSuccess(ActivateAbility(playerId = player1, sourceId = source, abilityId = abilityId))
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard < 30) {
            if (pendingDecision != null) submitYesNo(player2, draw) else bothPass()
            guard++
        }
    }

    test("an opponent activating a creature's ability lets you draw a card") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Runic Armasaur")
        val bear = driver.putPermanentOnBattlefield(driver.player1, "Ping Bear")
        val handBefore = driver.getHandSize(driver.player2)

        driver.activateAndAnswer(bear, "Ping Bear", draw = true)

        driver.getHandSize(driver.player2) shouldBe handBefore + 1
    }

    test("the draw is optional") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Runic Armasaur")
        val bear = driver.putPermanentOnBattlefield(driver.player1, "Ping Bear")
        val handBefore = driver.getHandSize(driver.player2)

        driver.activateAndAnswer(bear, "Ping Bear", draw = false)

        driver.getHandSize(driver.player2) shouldBe handBefore
    }

    test("an artifact's ability doesn't trigger it") {
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Runic Armasaur")
        val rock = driver.putPermanentOnBattlefield(driver.player1, "Ping Rock")
        val handBefore = driver.getHandSize(driver.player2)

        driver.activateAndAnswer(rock, "Ping Rock", draw = true)

        driver.getHandSize(driver.player2) shouldBe handBefore
    }
})
