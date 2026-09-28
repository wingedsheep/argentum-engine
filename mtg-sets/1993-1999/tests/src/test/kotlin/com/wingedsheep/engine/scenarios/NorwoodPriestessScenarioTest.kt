package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.p02.cards.NorwoodPriestess
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Norwood Priestess — "{T}: You may put a green creature card from your hand onto the battlefield.
 * Activate only during your turn, before attackers are declared."
 */
class NorwoodPriestessScenarioTest : FunSpec({

    val abilityId = NorwoodPriestess.activatedAbilities[0].id

    val greenBear = card("Test Green Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val redBear = card("Test Red Bear") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NorwoodPriestess)
        driver.registerCard(greenBear)
        driver.registerCard(redBear)
        return driver
    }

    test("puts a green creature from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val priestess = driver.putCreatureOnBattlefield(me, "Norwood Priestess")
        driver.removeSummoningSickness(priestess)
        driver.putCardInHand(me, "Test Green Bear")
        driver.putCardInHand(me, "Test Red Bear")

        driver.submit(ActivateAbility(playerId = me, sourceId = priestess, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
        // Only the green creature is a legal choice.
        driver.submitCardSelection(me, listOf(driver.findCardInHand(me, "Test Green Bear")!!))
            .outcome shouldBe Outcome.Done

        driver.findPermanent(me, "Test Green Bear") shouldNotBe null
        withClue("the red creature stays in hand") {
            driver.findCardInHand(me, "Test Red Bear") shouldNotBe null
            driver.findPermanent(me, "Test Red Bear") shouldBe null
        }
    }

    test("cannot be activated once attackers are declared") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val priestess = driver.putCreatureOnBattlefield(me, "Norwood Priestess")
        driver.removeSummoningSickness(priestess)
        driver.putCardInHand(me, "Test Green Bear")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(ActivateAbility(playerId = me, sourceId = priestess, abilityId = abilityId))
            .outcome shouldNotBe Outcome.Done
    }
})
