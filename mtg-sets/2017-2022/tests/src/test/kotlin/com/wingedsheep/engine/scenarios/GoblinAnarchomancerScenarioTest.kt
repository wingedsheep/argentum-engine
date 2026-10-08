package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.GoblinAnarchomancer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Goblin Anarchomancer (MH2 #200) — {R}{G} 2/2 Creature — Goblin Shaman
 *
 * "Each spell you cast that's red or green costs {1} less to cast."
 *
 * The first spell-cost reducer over a two-colour OR filter: a red spell and a green spell each get
 * the reduction, a red-and-green spell gets it once (ruling), and a spell of neither colour doesn't.
 */
class GoblinAnarchomancerScenarioTest : FunSpec({

    val redSpell = CardDefinition.creature(
        name = "Test Red Creature", manaCost = ManaCost.parse("{1}{R}"), subtypes = emptySet(), power = 1, toughness = 1
    )
    val greenSpell = CardDefinition.creature(
        name = "Test Green Creature", manaCost = ManaCost.parse("{1}{G}"), subtypes = emptySet(), power = 1, toughness = 1
    )
    val gruulSpell = CardDefinition.creature(
        name = "Test Gruul Creature", manaCost = ManaCost.parse("{2}{R}{G}"), subtypes = emptySet(), power = 1, toughness = 1
    )
    val blueSpell = CardDefinition.creature(
        name = "Test Blue Creature", manaCost = ManaCost.parse("{1}{U}"), subtypes = emptySet(), power = 1, toughness = 1
    )

    fun createDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinAnarchomancer, redSpell, greenSpell, gruulSpell, blueSpell))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Goblin Anarchomancer")
        return driver to you
    }

    fun castFromPool(driver: GameTestDriver, playerId: EntityId, cardId: EntityId) =
        driver.submit(CastSpell(playerId = playerId, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool))

    test("a red spell costs {1} less") {
        val (driver, you) = createDriver()
        val card = driver.putCardInHand(you, "Test Red Creature")
        driver.giveMana(you, Color.RED, 1)
        castFromPool(driver, you, card).error shouldBe null
    }

    test("a green spell costs {1} less") {
        val (driver, you) = createDriver()
        val card = driver.putCardInHand(you, "Test Green Creature")
        driver.giveMana(you, Color.GREEN, 1)
        castFromPool(driver, you, card).error shouldBe null
    }

    test("a spell that's both red and green costs only {1} less") {
        val (driver, you) = createDriver()
        val card = driver.putCardInHand(you, "Test Gruul Creature")
        driver.giveMana(you, Color.RED, 1)
        driver.giveMana(you, Color.GREEN, 1)
        // {2}{R}{G} reduced once is {1}{R}{G}: R + G alone isn't enough.
        (castFromPool(driver, you, card).error != null) shouldBe true

        driver.giveColorlessMana(you, 1)
        castFromPool(driver, you, card).error shouldBe null
    }

    test("a spell that's neither red nor green isn't reduced") {
        val (driver, you) = createDriver()
        val card = driver.putCardInHand(you, "Test Blue Creature")
        driver.giveMana(you, Color.BLUE, 1)
        (castFromPool(driver, you, card).error != null) shouldBe true
    }
})
