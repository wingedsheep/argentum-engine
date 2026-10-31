package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.HappilyEverAfter
import com.wingedsheep.mtg.sets.definitions.eld.cards.RealmCloakedGiant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Happily Ever After — "When this enchantment enters, each player gains 5 life and draws a card.
 * At the beginning of your upkeep, if there are five colors among permanents you control, there are
 * six or more card types among permanents you control and/or cards in your graveyard, and your life
 * total is greater than or equal to your starting life total, you win the game."
 *
 * The card-type clause is a union across the two zones: a type present in both counts once, and an
 * adventurer card in the graveyard counts only its main card type (2019-10-04 ruling).
 */
class HappilyEverAfterScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HappilyEverAfter)
        driver.registerCard(RealmCloakedGiant)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    /** Permanents of all five colors, plus Happily Ever After (enchantment) and a Mountain (land). */
    fun GameTestDriver.fiveColorBoard(you: EntityId) {
        putPermanentOnBattlefield(you, "Happily Ever After")
        putPermanentOnBattlefield(you, "Mountain")
        listOf("Savannah Lions", "Phantom Warrior", "Black Creature", "Goblin Guide", "Centaur Courser")
            .forEach { putCreatureOnBattlefield(you, it) }
    }

    /** Advance to your next upkeep and let anything that triggered there resolve. */
    fun GameTestDriver.throughYourNextUpkeep(you: EntityId) {
        passPriorityUntil(Step.UPKEEP, activePlayer = you)
        var guard = 0
        while (!state.gameOver && !isPaused && state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("entering, each player gains 5 life and draws a card") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val handsBefore = driver.getHandSize(you) to driver.getHandSize(opponent)
        val enchantment = driver.putCardInHand(you, "Happily Ever After")
        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, enchantment).outcome shouldBe Outcome.Done
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        withClue("both players gained 5") {
            driver.getLifeTotal(you) shouldBe 25
            driver.getLifeTotal(opponent) shouldBe 25
        }
        withClue("both players drew one") {
            driver.getHandSize(you) shouldBe handsBefore.first + 1
            driver.getHandSize(opponent) shouldBe handsBefore.second + 1
        }
    }

    test("five colors, six card types across battlefield and graveyard, full life: you win at upkeep") {
        val (driver, you) = setup()
        driver.fiveColorBoard(you) // creature, enchantment, land
        listOf("Lightning Bolt", "Doom Blade", "Palladium Myr") // instant, sorcery, artifact
            .forEach { driver.putCardInGraveyard(you, it) }

        driver.throughYourNextUpkeep(you)

        withClue("the game ended with you as the winner") {
            driver.state.gameOver shouldBe true
            driver.state.winnerId shouldBe you
        }
    }

    test("a card type in both zones counts once") {
        val (driver, you) = setup()
        driver.fiveColorBoard(you) // creature, enchantment, land
        // Six graveyard-plus-battlefield type hits, but only five distinct types: creature repeats.
        listOf("Lightning Bolt", "Doom Blade", "Savannah Lions").forEach { driver.putCardInGraveyard(you, it) }

        driver.throughYourNextUpkeep(you)

        withClue("five distinct card types is not enough") { driver.state.gameOver shouldBe false }
    }

    test("an adventurer card in the graveyard counts only its main card type") {
        val (driver, you) = setup()
        driver.fiveColorBoard(you) // creature, enchantment, land
        // Realm-Cloaked Giant's Adventure (Cast Off) is a sorcery; that type must not count.
        listOf("Lightning Bolt", "Palladium Myr", "Realm-Cloaked Giant").forEach { driver.putCardInGraveyard(you, it) }

        driver.throughYourNextUpkeep(you)

        withClue("creature, enchantment, land, instant, artifact — five types") { driver.state.gameOver shouldBe false }
    }

    test("below your starting life total you don't win") {
        val (driver, you) = setup()
        driver.fiveColorBoard(you)
        listOf("Lightning Bolt", "Doom Blade", "Palladium Myr").forEach { driver.putCardInGraveyard(you, it) }
        driver.setLifeTotal(you, 19)

        driver.throughYourNextUpkeep(you)

        withClue("life 19 < starting 20") { driver.state.gameOver shouldBe false }
    }

    test("without five colors among your permanents you don't win") {
        val (driver, you) = setup()
        driver.putPermanentOnBattlefield(you, "Happily Ever After")
        driver.putPermanentOnBattlefield(you, "Mountain")
        listOf("Savannah Lions", "Phantom Warrior", "Black Creature", "Goblin Guide")
            .forEach { driver.putCreatureOnBattlefield(you, it) }
        listOf("Lightning Bolt", "Doom Blade", "Palladium Myr").forEach { driver.putCardInGraveyard(you, it) }

        driver.throughYourNextUpkeep(you)

        withClue("four colors only") { driver.state.gameOver shouldBe false }
    }
})
