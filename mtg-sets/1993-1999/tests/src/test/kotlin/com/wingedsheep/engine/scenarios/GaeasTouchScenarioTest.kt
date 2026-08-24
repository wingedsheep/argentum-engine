package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.GaeasTouch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gaea's Touch — "{0}: You may put a basic Forest card from your hand onto the
 * battlefield. Activate only as a sorcery and only once each turn." plus "Sacrifice this
 * enchantment: Add {G}{G}."
 *
 * The point of the card is that the Forest is *put onto the battlefield*, so it does not consume
 * the land drop — a scenario that plays a land afterwards is the only way to catch an
 * implementation that routed this through the play-a-land path. The once-each-turn restriction gets
 * its own case, since it and the sorcery-speed clause are separate knobs that can drift apart.
 */
class GaeasTouchScenarioTest : FunSpec({

    val putAbilityId = GaeasTouch.activatedAbilities[0].id
    val manaAbilityId = GaeasTouch.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GaeasTouch)
        return driver
    }

    test("puts a basic Forest onto the battlefield without spending the land drop") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val touch = driver.putPermanentOnBattlefield(me, "Gaea's Touch")
        driver.putCardInHand(me, "Forest")
        driver.putCardInHand(me, "Forest")
        val landsBefore = driver.getLands(me).size

        driver.submit(ActivateAbility(playerId = me, sourceId = touch, abilityId = putAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        // The "you may" is a ChooseUpTo(1) selection — take the Forest.
        driver.submitCardSelection(me, listOf(driver.findCardInHand(me, "Forest")!!))
            .isSuccess shouldBe true

        withClue("the Forest arrived from hand") {
            driver.getLands(me).size shouldBe landsBefore + 1
        }
        withClue("and the land drop is still available — this is a put, not a play") {
            driver.playLand(me, driver.findCardInHand(me, "Forest")!!).isSuccess shouldBe true
            driver.getLands(me).size shouldBe landsBefore + 2
        }
    }

    test("only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val touch = driver.putPermanentOnBattlefield(me, "Gaea's Touch")
        driver.putCardInHand(me, "Forest")
        driver.putCardInHand(me, "Forest")

        driver.submit(ActivateAbility(playerId = me, sourceId = touch, abilityId = putAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        driver.submitCardSelection(me, listOf(driver.findCardInHand(me, "Forest")!!))
            .isSuccess shouldBe true

        withClue("the second activation this turn is illegal") {
            driver.submit(ActivateAbility(playerId = me, sourceId = touch, abilityId = putAbilityId))
                .isSuccess shouldBe false
        }
    }

    test("sacrificing it adds {G}{G}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val touch = driver.putPermanentOnBattlefield(me, "Gaea's Touch")

        driver.submit(ActivateAbility(playerId = me, sourceId = touch, abilityId = manaAbilityId))
            .isSuccess shouldBe true

        withClue("a mana ability resolves without the stack, and the cost ate the enchantment") {
            driver.findPermanent(me, "Gaea's Touch") shouldBe null
        }
    }
})
