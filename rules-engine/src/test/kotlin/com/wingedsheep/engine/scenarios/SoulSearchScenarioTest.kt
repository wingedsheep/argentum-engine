package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.SoulSearch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Soul Search (MKM #232) — {W}{B} Sorcery.
 *
 * "Target opponent reveals their hand. You choose a nonland card from it. Exile that card. If the
 *  card's mana value is 1 or less, create a 1/1 white and black Spirit creature token with flying."
 *
 * Three things worth pinning:
 *  - the selection is restricted to **nonland** cards, so lands are never offered;
 *  - the Spirit rider keys off the *exiled* card's mana value, so an expensive pick mints nothing;
 *  - an all-lands hand exiles nothing **and** makes no Spirit — the fail-open shape, where a rider
 *    reading the pre-move selection instead of what actually reached exile would hand out a free body.
 */
class SoulSearchScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SoulSearch)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun spirits(driver: GameTestDriver, player: EntityId): Int =
        driver.getPermanents(player).count { nameOf(driver, it) == "Spirit" }

    /** Cast Soul Search at [opponent] and stop on the "choose a nonland card" decision. */
    fun castAt(driver: GameTestDriver, player: EntityId, opponent: EntityId) {
        val card = driver.putCardInHand(player, "Soul Search")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpellWithTargets(player, card, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        driver.bothPass()
    }

    test("a one-mana pick is exiled and pays off with a Spirit") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // Savannah Lions is {W} (mana value 1); Centaur Courser is {2}{G} (mana value 3).
        driver.putCardInHand(opponent, "Savannah Lions")
        driver.putCardInHand(opponent, "Centaur Courser")
        driver.putCardInHand(opponent, "Swamp")

        castAt(driver, player, opponent)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("only nonland cards are choosable — the Swamp must not be offered") {
            decision.options.mapNotNull { nameOf(driver, it) }.sorted() shouldBe
                listOf("Centaur Courser", "Savannah Lions")
        }

        val lions = decision.options.single { nameOf(driver, it) == "Savannah Lions" }
        driver.submitCardSelection(player, listOf(lions))

        withClue("the chosen card is exiled from the opponent's hand") {
            driver.getExileCardNames(opponent) shouldContain "Savannah Lions"
            driver.findCardInHand(opponent, "Savannah Lions") shouldBe null
        }
        withClue("mana value 1 clears the bar, so the caster gets the Spirit") {
            spirits(driver, player) shouldBe 1
        }
    }

    test("an expensive pick is exiled but mints no Spirit") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCardInHand(opponent, "Savannah Lions")
        driver.putCardInHand(opponent, "Centaur Courser")

        castAt(driver, player, opponent)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        val courser = decision.options.single { nameOf(driver, it) == "Centaur Courser" }
        driver.submitCardSelection(player, listOf(courser))

        withClue("the Courser is gone all the same") {
            driver.getExileCardNames(opponent) shouldContain "Centaur Courser"
        }
        withClue("mana value 3 is above the bar — no consolation body") {
            spirits(driver, player) shouldBe 0
        }
    }

    test("an all-lands hand exiles nothing and makes nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCardInHand(opponent, "Swamp")
        driver.putCardInHand(opponent, "Swamp")

        castAt(driver, player, opponent)
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }

        withClue("there was no nonland card to take") {
            driver.getExileCardNames(opponent).contains("Swamp") shouldBe false
        }
        withClue("and nothing reached exile, so the rider must not fire") {
            spirits(driver, player) shouldBe 0
        }
    }
})
