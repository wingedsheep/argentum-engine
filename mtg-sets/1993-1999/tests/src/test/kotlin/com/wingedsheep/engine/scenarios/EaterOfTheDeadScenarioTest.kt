package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.EaterOfTheDead
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eater of the Dead — "{0}: If this creature is tapped, exile target creature
 * card from a graveyard and untap this creature."
 *
 * Two things are worth proving. The "if tapped" clause is checked *on resolution*, not as an
 * activation gate, so the ability is legal from an untapped Eater and simply does nothing — the
 * targeted card must survive. And the graveyard is "a graveyard", so an opponent's works too.
 */
class EaterOfTheDeadScenarioTest : FunSpec({

    val abilityId = EaterOfTheDead.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EaterOfTheDead)
        return driver
    }

    test("while tapped it eats a graveyard creature and untaps") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val eater = driver.putCreatureOnBattlefield(me, "Eater of the Dead")
        driver.tapPermanent(eater)
        val corpse = driver.putCardInGraveyard(me, "Centaur Courser")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = eater,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, corpse)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the creature card left the graveyard for exile") {
            driver.getGraveyardCardNames(me) shouldBe emptyList()
            driver.getExileCardNames(me) shouldBe listOf("Centaur Courser")
        }
        withClue("and the Eater untapped") {
            driver.isTapped(eater) shouldBe false
        }
    }

    test("an opponent's graveyard is fair game") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val eater = driver.putCreatureOnBattlefield(me, "Eater of the Dead")
        driver.tapPermanent(eater)
        val corpse = driver.putCardInGraveyard(opponent, "Centaur Courser")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = eater,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, corpse)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.getGraveyardCardNames(opponent) shouldBe emptyList()
        driver.isTapped(eater) shouldBe false
    }

    test("while untapped the ability resolves as a no-op and the card survives") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val eater = driver.putCreatureOnBattlefield(me, "Eater of the Dead")
        val corpse = driver.putCardInGraveyard(me, "Centaur Courser")

        withClue("the ability is legal to activate — the gate is on resolution, not activation") {
            driver.submit(
                ActivateAbility(
                    playerId = me,
                    sourceId = eater,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(driver.state, corpse)),
                )
            ).isSuccess shouldBe true
        }
        driver.bothPass()

        withClue("but an untapped Eater eats nothing") {
            driver.getGraveyardCardNames(me) shouldBe listOf("Centaur Courser")
            driver.getExileCardNames(me) shouldBe emptyList()
        }
    }
})
