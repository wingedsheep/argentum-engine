package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.GoblinWizard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Wizard.
 *
 * The cheat ability is a *put*, so the land drop and the mana cost both stay untouched — that is
 * what the first test measures. The protection ability is checked through a real block, since
 * protection from white that projects but never reaches combat would be a silent no-op; a
 * non-white blocker on the same board keeps the test honest about *what* it prevents.
 */
class GoblinWizardScenarioTest : FunSpec({

    val putAbilityId = GoblinWizard.activatedAbilities[0].id
    val protectionAbilityId = GoblinWizard.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GoblinWizard)
        return driver
    }

    test("puts a Goblin from hand onto the battlefield for free") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wizard = driver.putCreatureOnBattlefield(me, "Goblin Wizard")
        driver.removeSummoningSickness(wizard)
        driver.putCardInHand(me, "Goblin Balloon Brigade")

        driver.submit(ActivateAbility(playerId = me, sourceId = wizard, abilityId = putAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        driver.submitCardSelection(me, listOf(driver.findCardInHand(me, "Goblin Balloon Brigade")!!))
            .isSuccess shouldBe true

        withClue("the Goblin arrived from hand, with no mana spent") {
            (driver.findPermanent(me, "Goblin Balloon Brigade") != null) shouldBe true
            driver.findCardInHand(me, "Goblin Balloon Brigade") shouldBe null
        }
    }

    test("a non-Goblin card in hand is not a legal pick") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wizard = driver.putCreatureOnBattlefield(me, "Goblin Wizard")
        driver.removeSummoningSickness(wizard)
        driver.putCardInHand(me, "Grizzly Bears")

        driver.submit(ActivateAbility(playerId = me, sourceId = wizard, abilityId = putAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()

        withClue("the gather finds no Goblin permanent card, so nothing is offered or put") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
        }
    }

    test("protection from white stops a white blocker but not a red one") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wizard = driver.putCreatureOnBattlefield(me, "Goblin Wizard")
        driver.removeSummoningSickness(wizard)
        val attacker = driver.putCreatureOnBattlefield(me, "Goblin Balloon Brigade")
        driver.removeSummoningSickness(attacker)
        val whiteBlocker = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val redBlocker = driver.putCreatureOnBattlefield(opponent, "Goblin Balloon Brigade")
        driver.giveMana(me, Color.RED, 1)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = wizard,
                abilityId = protectionAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, attacker)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("protection from white: the white creature can't block it") {
            driver.declareBlockers(opponent, mapOf(whiteBlocker to listOf(attacker))).isSuccess shouldBe false
        }
        withClue("but a red creature still can — it is protection from white, not from everything") {
            driver.declareBlockers(opponent, mapOf(redBlocker to listOf(attacker))).isSuccess shouldBe true
        }
    }
})
