package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.SwordPointDiplomacy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sword-Point Diplomacy — "Reveal the top three cards of your library. For each of those cards, put
 * that card into your hand unless any opponent pays 3 life. Then exile the rest."
 *
 * The caster orders the cards (ruling), the opponent decides card by card, and only the cards an
 * opponent paid for are exiled — after every decision.
 */
class SwordPointDiplomacyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SwordPointDiplomacy)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Stacks the three cards (Centaur Courser on top) and casts the sorcery up to its first decision. */
    fun GameTestDriver.castOverTopThree(): Triple<EntityId, EntityId, EntityId> {
        val guide = putCardOnTopOfLibrary(player1, "Goblin Guide")
        val lions = putCardOnTopOfLibrary(player1, "Savannah Lions")
        val courser = putCardOnTopOfLibrary(player1, "Centaur Courser")
        val spell = putCardInHand(player1, "Sword-Point Diplomacy")
        giveMana(player1, Color.BLACK, 3)
        castSpell(player1, spell)
        bothPass()
        return Triple(courser, lions, guide)
    }

    fun GameTestDriver.casterPicks(card: EntityId) {
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe player1
        submitCardSelection(player1, listOf(card))
    }

    fun GameTestDriver.opponentPays(pay: Boolean) {
        val decision = pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe player2
        submitYesNo(player2, pay)
    }

    test("a card the opponent pays 3 life for is exiled; the others go to hand") {
        val driver = createDriver()
        val (courser, lions, _) = driver.castOverTopThree()

        // The caster puts the Lions to the opponent first; the opponent pays for it.
        driver.casterPicks(lions)
        driver.opponentPays(true)
        // Next the Courser; the opponent declines. The Guide is the last card, offered without a pick.
        driver.casterPicks(courser)
        driver.opponentPays(false)
        driver.opponentPays(false)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(driver.player2) shouldBe 17
        driver.findCardInHand(driver.player1, "Centaur Courser") shouldNotBe null
        driver.findCardInHand(driver.player1, "Goblin Guide") shouldNotBe null
        driver.getHand(driver.player1) shouldNotContain lions
        driver.getExileCardNames(driver.player1) shouldContainExactlyInAnyOrder listOf("Savannah Lions")
        driver.getGraveyardCardNames(driver.player1) shouldContainExactlyInAnyOrder
            listOf("Sword-Point Diplomacy")
    }

    test("when the opponent never pays, all three cards go to hand and nothing is exiled") {
        val driver = createDriver()
        val (courser, lions, _) = driver.castOverTopThree()

        driver.casterPicks(courser)
        driver.opponentPays(false)
        driver.casterPicks(lions)
        driver.opponentPays(false)
        driver.opponentPays(false)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(driver.player2) shouldBe 20
        listOf("Centaur Courser", "Savannah Lions", "Goblin Guide").forEach {
            driver.findCardInHand(driver.player1, it) shouldNotBe null
        }
        driver.getExileCardNames(driver.player1).isEmpty() shouldBe true
    }

    test("paying for every card exiles all three") {
        val driver = createDriver()
        val (courser, lions, _) = driver.castOverTopThree()

        driver.casterPicks(courser)
        driver.opponentPays(true)
        driver.casterPicks(lions)
        driver.opponentPays(true)
        driver.opponentPays(true)

        driver.getLifeTotal(driver.player2) shouldBe 11
        driver.getExileCardNames(driver.player1) shouldContainExactlyInAnyOrder
            listOf("Centaur Courser", "Savannah Lions", "Goblin Guide")
    }
})
