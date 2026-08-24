package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.RagMan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rag Man — "{B}{B}{B}, {T}: Target opponent reveals their hand and discards a
 * creature card at random. Activate only during your turn."
 *
 * "At random" is untestable as a distribution here, but *what it draws from* is very testable: with
 * a hand of one creature and several noncreature cards, the discard must be that creature every
 * time. A pipeline that randomised over the whole hand and only then filtered would drop a
 * noncreature — or nothing — most runs, so a handful of creature-free noise cards is the assertion.
 */
class RagManScenarioTest : FunSpec({

    val abilityId = RagMan.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RagMan)
        return driver
    }

    test("the discard comes from the creature cards, not the whole hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragMan = driver.putCreatureOnBattlefield(me, "Rag Man")
        driver.removeSummoningSickness(ragMan)
        driver.giveMana(me, Color.BLACK, 3)

        driver.putCardInHand(opponent, "Grizzly Bears")
        driver.putCardInHand(opponent, "Lightning Bolt")
        driver.putCardInHand(opponent, "Forest")
        driver.putCardInHand(opponent, "Mountain")
        // The opening hand is dealt too, so measure the delta rather than an absolute size.
        val handBefore = driver.getHandSize(opponent)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = ragMan,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the one creature card is the only thing that can be picked") {
            driver.getGraveyardCardNames(opponent) shouldBe listOf("Grizzly Bears")
        }
        withClue("exactly one card left the hand") {
            driver.getHandSize(opponent) shouldBe handBefore - 1
        }
    }

    test("a creature-free hand loses nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragMan = driver.putCreatureOnBattlefield(me, "Rag Man")
        driver.removeSummoningSickness(ragMan)
        driver.giveMana(me, Color.BLACK, 3)

        driver.putCardInHand(opponent, "Lightning Bolt")
        driver.putCardInHand(opponent, "Forest")
        val handBefore = driver.getHandSize(opponent)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = ragMan,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("an empty gather makes the select and move silent no-ops") {
            driver.getHandSize(opponent) shouldBe handBefore
            driver.getGraveyardCardNames(opponent) shouldBe emptyList()
        }
    }

    test("not activatable on the opponent's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val first = driver.activePlayer!!
        val ragManController = driver.getOpponent(first)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragMan = driver.putCreatureOnBattlefield(ragManController, "Rag Man")
        driver.removeSummoningSickness(ragMan)
        driver.giveMana(ragManController, Color.BLACK, 3)
        driver.putCardInHand(first, "Grizzly Bears")

        withClue("it is the other player's turn, and the ability says only during yours") {
            driver.submit(
                ActivateAbility(
                    playerId = ragManController,
                    sourceId = ragMan,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(driver.state, first)),
                )
            ).isSuccess shouldBe false
        }
    }
})
