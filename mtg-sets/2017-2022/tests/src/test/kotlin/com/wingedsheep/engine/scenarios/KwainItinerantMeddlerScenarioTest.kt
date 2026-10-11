package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmr.cards.KwainItinerantMeddler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kwain, Itinerant Meddler — "{T}: Each player may draw a card, then each player who drew a card
 * this way gains 1 life."
 *
 * Each player answers their own "may"; only a player who actually drew gains the life.
 */
class KwainItinerantMeddlerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(KwainItinerantMeddler)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activateKwainAndAnswer(choices: Map<Any, Boolean>): List<Any> {
        val kwain = putCreatureOnBattlefield(player1, "Kwain, Itinerant Meddler")
        removeSummoningSickness(kwain)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = kwain,
                abilityId = KwainItinerantMeddler.activatedAbilities.first().id,
            )
        )
        bothPass()
        val askedInOrder = mutableListOf<Any>()
        var safety = 0
        while (safety++ < 10) {
            val decision = pendingDecision as? YesNoDecision ?: break
            askedInOrder += decision.playerId
            submitYesNo(decision.playerId, choices[decision.playerId] ?: false)
        }
        return askedInOrder
    }

    test("a player who draws gains 1 life; a player who declines neither draws nor gains") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.player2
        val hand1 = d.getHand(p1).size
        val hand2 = d.getHand(p2).size
        val life1 = d.getLifeTotal(p1)
        val life2 = d.getLifeTotal(p2)

        val asked = d.activateKwainAndAnswer(mapOf(p1 to false, p2 to true))

        asked shouldBe listOf(p1, p2) // APNAP: active player first, each answers their own
        d.isPaused shouldBe false
        d.getHand(p1).size shouldBe hand1
        d.getLifeTotal(p1) shouldBe life1
        d.getHand(p2).size shouldBe hand2 + 1
        d.getLifeTotal(p2) shouldBe life2 + 1
    }

    test("both players drawing both gain 1 life") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.player2
        val life1 = d.getLifeTotal(p1)
        val life2 = d.getLifeTotal(p2)

        d.activateKwainAndAnswer(mapOf(p1 to true, p2 to true))

        d.getLifeTotal(p1) shouldBe life1 + 1
        d.getLifeTotal(p2) shouldBe life2 + 1
    }
})
